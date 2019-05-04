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
import java.text.NumberFormat;
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
    private final Notifier notifier;
    private final WatchServiceCache watchServiceCache;
    private final PreviewProcessor previewProcessor;
    private Configuration configuration;
    private DirectoryListener directoryListener;
    private final Statistics statistics;
    private Thread progressInfo;

    public Backend(final Notifier aNotifier, final Configuration aConfiguration, final PreviewProcessor aPreviewProcessor) throws IOException {
        notifier = aNotifier;
        previewProcessor = aPreviewProcessor;
        locations = new HashMap<>();
        watchServiceCache = new WatchServiceCache();
        contentExtractor = new ContentExtractor(aConfiguration);
        statistics = new Statistics();
        // This is our simple flux
        final Flux<FileEvent> theFileEventFlux = Flux.push(sink -> directoryListener = new DirectoryListener() {

            @Override
            public void fileDeleted(final Configuration.CrawlLocation aLocation, final Path aFile) {
                synchronized (this) {
                    try {
                        if (contentExtractor.supportsFile(aFile.toString())) {
                            final var theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);

                            statistics.newDeletedFileJob();

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

                            statistics.newModifiedFileJob();

                            sink.next(new FileEvent(aLocation, aFile, theAttributes, FileEvent.EventType.UPDATED));
                        }
                    } catch (final Exception e) {
                        log.error("Error processing file {}", aFile, e);
                    }
                }
            }
        });

        // Filter update events for Files that were not changed
        final var theCheckFlux = theFileEventFlux.parallel(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)).runOn(Schedulers.parallel()).filter(aFileEvent -> {
            // Always keep delete file events
            if (aFileEvent.type == FileEvent.EventType.DELETED) {
                return true;
            }
            final var thePath = aFileEvent.path;
            final var theFileName = thePath.toString();

            try {
                final var theUpdateCheckResult = luceneIndexHandler
                        .checkIfModified(theFileName, aFileEvent.attributes.lastModifiedTime().toMillis());

                final boolean result = theUpdateCheckResult == UpdateCheckResult.UPDATED;
                if (!result) {
                    statistics.jobSkipped();
                }
                return result;
            } catch (final Exception e) {
                throw Exceptions.propagate(e);
            }
        });

        // Ok, we now map the file events to lucene commands
        final var theLuceneFlux = theCheckFlux.map(
                aFileEvent -> {
                    if (aFileEvent.type == FileEvent.EventType.DELETED) {
                        return new LuceneCommand(aFileEvent, null);
                    }

                    final var thePath = aFileEvent.path;
                    final var theContent = contentExtractor.extractContentFrom(thePath, aFileEvent.attributes);
                    return new LuceneCommand(aFileEvent, theContent);
                });

        // Ok, finally we add everything to the index
        theLuceneFlux.doOnNext(aCommand -> {
            if (aCommand.fileEvent.type == FileEvent.EventType.DELETED) {
                try {
                    luceneIndexHandler.removeFromIndex(aCommand.fileEvent.path.toString());

                    aNotifier.showInformation("Deleted " + aCommand.fileEvent.path.getFileName());

                } catch (Exception e) {
                    aNotifier.showError("Error removing " + aCommand.fileEvent.path.getFileName(), e);
                }
            } else {
                if (aCommand.content != null) {
                    try {
                        luceneIndexHandler.addToIndex(aCommand.fileEvent.crawlLocation.getId(), aCommand.content);

                        notifier.showInformation("Reindexed " + aCommand.fileEvent.path.getFileName());

                    } catch (Exception e) {
                        aNotifier.showError("Error re-inxeding " + aCommand.fileEvent.path.getFileName(), e);
                    }
                }
            }
        }).sequential().subscribe(new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(final Subscription aSubscription) {
                request(Runtime.getRuntime().availableProcessors());
            }

            @Override
            protected void hookOnNext(final LuceneCommand aCommand) {
                log.info("Processed command for {}", aCommand.fileEvent.path);
                statistics.jobFinished();
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

    public void setProgressListener(final ProgressListener aProgressListener) {
        progressListener = aProgressListener;

        progressInfo = new Thread("ProgressInfo") {
            @Override
            public void run() {

                long lastRemaining = -1;
                final var format = NumberFormat.getIntegerInstance();
                var lastMessage = "";

                while (!isInterrupted()) {

                    final long totalJobs = statistics.totalJobs();
                    final long completedJobs = statistics.completedJobs();

                    final long remaining = Math.max(totalJobs - completedJobs, 0);

                    if (remaining > 0) {
                        if (lastRemaining == -1) {
                            lastMessage = remaining + " Files are still in the indexing queue.";
                            progressListener.infotext(lastMessage);
                        } else {
                            final var thruput = lastRemaining - remaining;
                            if (thruput > 0) {
                                final double eta = ((double) remaining) / thruput;
                                lastMessage = remaining + " Files are still in the indexing queue, " + format.format(eta) + " seconds remaining (ETA).";
                                progressListener.infotext(lastMessage);
                            } else {
                                if (lastMessage.length() > 0) {
                                    progressListener.infotext(lastMessage);
                                }
                            }
                        }
                    } else {
                        lastMessage = "";
                    }

                    lastRemaining = remaining;

                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {

                    }
                }
            }
        };
        progressInfo.start();
    }

    private void add(final Configuration.CrawlLocation aLocation) throws IOException {
        locations.put(aLocation, new DirectoryWatcher(watchServiceCache, aLocation, DirectoryWatcher.DEFAULT_WAIT_FOR_ACTION, directoryListener).startWatching());
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
        if (progressInfo != null) {
            progressInfo.interrupt();
        }
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