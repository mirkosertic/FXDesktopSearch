/*
 * FXDesktopSearch Copyright 2013 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.desktopsearch;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

@Slf4j
class Backend implements ConfigurationChangeListener {

    public static class FileEvent {
        public enum EventType {
            UPDATED, DELETED
        }
        private final Configuration.CrawlLocation crawlLocation;
        private final Path path;
        private final EventType type;
        private final BasicFileAttributes attributes;

        public FileEvent(final Configuration.CrawlLocation aCrawlLocation, final Path aPath, final BasicFileAttributes aFileAttributes, final EventType aEventType) {
            crawlLocation = aCrawlLocation;
            path = aPath;
            type = aEventType;
            attributes = aFileAttributes;
        }
    }

    public static class LuceneCommand {

        private final FileEvent fileEvent;
        private final Content content;

        public LuceneCommand(final FileEvent aFileEvent, final Content aContent) {
            fileEvent = aFileEvent;
            content = aContent;
        }
    }

    private LuceneIndexHandler luceneIndexHandler;
    private final ContentExtractor contentExtractor;
    private ProgressListener progressListener;
    private final Map<Configuration.CrawlLocation, DirectoryWatcher> locations;
    private final ExecutorPool executorPool;
    private final Notifier notifier;
    private final WatchServiceCache watchServiceCache;
    private final PreviewProcessor previewProcessor;
    private Configuration configuration;
    private DirectoryListener directoryListener;

    public Backend(final Notifier aNotifier, final Configuration aConfiguration, final PreviewProcessor aPreviewProcessor) throws IOException {
        notifier = aNotifier;
        previewProcessor = aPreviewProcessor;
        locations = new HashMap<>();
        executorPool = new ExecutorPool();
        watchServiceCache = new WatchServiceCache();
        contentExtractor = new ContentExtractor(aConfiguration);

        // This is our simple flux
        Flux<FileEvent> theFileEventFlux = Flux.push(sink -> directoryListener = new DirectoryListener() {

            @Override
            public void fileDeleted(final Configuration.CrawlLocation aLocation, final Path aFile) {
                synchronized (this) {
                    try {
                        if (contentExtractor.supportsFile(aFile.toString())) {
                            final var theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);
                            sink.next(new FileEvent(aLocation, aFile, theAttributes, FileEvent.EventType.DELETED));
                        }
                    } catch (final Exception e) {
                        log.error("Error processing file {}", aFile, e);
                    }
                }
            }

            @Override
            public void fileCreatedOrModified(final Configuration.CrawlLocation aLocation, final Path aFile) {
                synchronized (this) {
                    try {
                        if (contentExtractor.supportsFile(aFile.toString())) {
                            final var theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);
                            sink.next(new FileEvent(aLocation, aFile, theAttributes, FileEvent.EventType.UPDATED));
                        }
                    } catch (final Exception e) {
                        log.error("Error processing file {}", aFile, e);
                    }
                }
            }

            @Override
            public void fileFoundByCrawler(final Configuration.CrawlLocation aLocation, final Path aFile) {
                synchronized (this) {
                    try {
                        if (contentExtractor.supportsFile(aFile.toString())) {
                            final var theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);
                            sink.next(new FileEvent(aLocation, aFile, theAttributes, FileEvent.EventType.UPDATED));
                        }
                    } catch (final Exception e) {
                        log.error("Error processing file {}", aFile, e);
                    }
                }
            }
        });

        // Filter update events for Files that were not changed
        theFileEventFlux = theFileEventFlux.filter(aFileEvent -> {
            // Always keep delete file events
            if (aFileEvent.type == FileEvent.EventType.DELETED) {
                return true;
            }
            final var thePath = aFileEvent.path;
            final var theFileName = thePath.toString();

            try {
                final var theUpdateCheckResult = luceneIndexHandler
                        .checkIfModified(theFileName, aFileEvent.attributes.lastModifiedTime().toMillis());
                return theUpdateCheckResult == UpdateCheckResult.UPDATED;
            } catch (final Exception e) {
                throw Exceptions.propagate(e);
            }
        });

        // Ok, we now map the file events to lucene commands
        final var theLuceneFlux = theFileEventFlux.publishOn(Schedulers.newSingle("ContentExtractor")).map(
                aFileEvent -> {
                    if (aFileEvent.type == FileEvent.EventType.DELETED) {
                        return new LuceneCommand(aFileEvent, null);
                    }

                    final var thePath = aFileEvent.path;
                    final var theContent = contentExtractor.extractContentFrom(thePath, aFileEvent.attributes);
                    return new LuceneCommand(aFileEvent, theContent);
                });

        // Ok, finally we add everything to the index
        theLuceneFlux.publishOn(Schedulers.newSingle("LuceneUpdater")).doOnNext(aCommand -> {
            if (aCommand.fileEvent.type == FileEvent.EventType.DELETED) {
                try {
                    luceneIndexHandler.removeFromIndex(aCommand.fileEvent.path.toString());

                    aNotifier.showInformation("Deleted " + aCommand.fileEvent.path.getFileName());

                    progressListener.newFileFound(aCommand.fileEvent.path.toString());
                } catch (Exception e) {
                    aNotifier.showError("Error removing " + aCommand.fileEvent.path.getFileName(), e);
                }
            } else {
                if (aCommand.content != null) {
                    try {
                        luceneIndexHandler.addToIndex(aCommand.fileEvent.crawlLocation.getId(), aCommand.content);

                        notifier.showInformation("Reindexed " + aCommand.fileEvent.path.getFileName());

                        progressListener.newFileFound(aCommand.fileEvent.path.toString());

                    } catch (Exception e) {
                        aNotifier.showError("Error re-inxeding " + aCommand.fileEvent.path.getFileName(), e);
                    }
                }
            }
        }).subscribe(new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(final Subscription aSubscription) {
                request(1);
            }

            @Override
            protected void hookOnNext(final LuceneCommand aCommand) {
                log.info("Processed command for {}", aCommand.fileEvent.path);
                request(1);
            }

            @Override
            protected void hookOnError(final Throwable aThrowable) {
                log.error("Flux went into error state", aThrowable);
            }
        });

        configurationUpdated(aConfiguration);
    }

    @Override
    public void configurationUpdated(final Configuration aConfiguration) throws IOException {

        setIndexLocation(aConfiguration);

        configuration = aConfiguration;
        locations.values().forEach(DirectoryWatcher::stopWatching);
        locations.clear();

        aConfiguration.getCrawlLocations().forEach(e -> {
            final var theDirectory = e.getDirectory();
            if (theDirectory.exists() && theDirectory.isDirectory()) {
                try {
                    add(e);
                } catch (final IOException e1) {
                    log.error("Error setting filesystem location for {}" + theDirectory, e1);
                }
            }
        });
    }

    public void setProgressListener(final ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    private void add(final Configuration.CrawlLocation aLocation) throws IOException {
        locations.put(aLocation, new DirectoryWatcher(watchServiceCache, aLocation, DirectoryWatcher.DEFAULT_WAIT_FOR_ACTION, directoryListener, executorPool).startWatching());
    }

    private void setIndexLocation(final Configuration aConfiguration) throws IOException {
        if (luceneIndexHandler != null) {
            shutdown();
        }
        luceneIndexHandler = new LuceneIndexHandler(aConfiguration, previewProcessor);
    }

    public void crawlLocations() {

        luceneIndexHandler.crawlingStarts();

        final var theRunner = new Thread(() -> {

            log.info("Starting to crawl");
            locations.values().forEach(theWatcher -> {
                try {
                    theWatcher.crawl();
                } catch (final Exception e) {
                    log.error("Error while crawling", e);
                }
            });

            progressListener.crawlingFinished();
        });
        theRunner.start();
    }

    public void shutdown() {
        luceneIndexHandler.shutdown();
    }

    public QueryResult performQuery(final String aQueryString, final String aBasePath, final Map<String, String> aDrilldownDimensions) {
        return luceneIndexHandler.performQuery(aQueryString, aBasePath, configuration, aDrilldownDimensions);
    }

    public Suggestion[] findSuggestionTermsFor(final String aTerm) {
        return luceneIndexHandler.findSuggestionTermsFor(aTerm);
    }

    public File getFileOnDiskForDocument(final String aDocumentID) {
        return luceneIndexHandler.getFileOnDiskForDocument(aDocumentID);
    }
}