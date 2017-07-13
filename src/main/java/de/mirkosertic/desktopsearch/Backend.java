/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2013 Mirko Sertic
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.mirkosertic.desktopsearch;

import org.apache.log4j.Logger;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class Backend implements ConfigurationChangeListener {

    public static class FileEvent {
        public enum EventType {
            UPDATED, DELETED;
        }
        private final Configuration.CrawlLocation crawlLocation;
        private final Path path;
        private final EventType type;
        private final BasicFileAttributes attributes;

        public FileEvent(Configuration.CrawlLocation aCrawlLocation, Path aPath, BasicFileAttributes aFileAttributes, EventType aEventType) {
            crawlLocation = aCrawlLocation;
            path = aPath;
            type = aEventType;
            attributes = aFileAttributes;
        }
    }

    public static class LuceneCommand {

        private final FileEvent fileEvent;
        private final Content content;

        public LuceneCommand(FileEvent aFileEvent, Content aContent) {
            fileEvent = aFileEvent;
            content = aContent;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Backend.class);

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

    public Backend(Notifier aNotifier, Configuration aConfiguration, PreviewProcessor aPreviewProcessor) throws IOException {
        notifier = aNotifier;
        previewProcessor = aPreviewProcessor;
        locations = new HashMap<>();
        executorPool = new ExecutorPool();
        watchServiceCache = new WatchServiceCache();
        contentExtractor = new ContentExtractor(aConfiguration);

        // This is our simple flux
        Flux<FileEvent> theFileEventFlux = Flux.push(sink -> {
            directoryListener = new DirectoryListener() {

                @Override
                public void fileDeleted(Configuration.CrawlLocation aLocation, Path aFile) {
                    synchronized (this) {
                        try {
                            BasicFileAttributes theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);
                            sink.next(new FileEvent(aLocation, aFile, theAttributes, FileEvent.EventType.DELETED));
                        } catch (Exception e) {
                            LOGGER.error("Error processing file " + aFile, e);
                        }
                    }
                }

                @Override
                public void fileCreatedOrModified(Configuration.CrawlLocation aLocation, Path aFile) {
                    synchronized (this) {
                        try {
                            BasicFileAttributes theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);
                            sink.next(new FileEvent(aLocation, aFile, theAttributes, FileEvent.EventType.UPDATED));
                        } catch (Exception e) {
                            LOGGER.error("Error processing file " + aFile, e);
                        }
                    }
                }

                @Override
                public void fileFoundByCrawler(Configuration.CrawlLocation aLocation, Path aFile) {
                    synchronized (this) {
                        try {
                            BasicFileAttributes theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);
                            sink.next(new FileEvent(aLocation, aFile, theAttributes, FileEvent.EventType.UPDATED));
                        } catch (Exception e) {
                            LOGGER.error("Error processing file " + aFile, e);
                        }
                    }
                }
            };
        });

        // Filter update events for Files that were not changed
        theFileEventFlux = theFileEventFlux.filter(new Predicate<FileEvent>() {
            @Override
            public boolean test(FileEvent aFileEvent) {
                // Always keep delete file events
                if (aFileEvent.type == FileEvent.EventType.DELETED) {
                    return true;
                }
                Path thePath = aFileEvent.path;
                String theFileName = thePath.toString();

                try {
                    UpdateCheckResult theUpdateCheckResult = luceneIndexHandler
                            .checkIfModified(theFileName, aFileEvent.attributes.lastModifiedTime().toMillis());
                    return theUpdateCheckResult == UpdateCheckResult.UPDATED;
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }
        });

        // Ok, we now map the file events to lucene commands
        Flux<LuceneCommand> theLuceneFlux = theFileEventFlux.publishOn(Schedulers.newSingle("ContentExtractor")).map(new Function<FileEvent, LuceneCommand>() {
            @Override
            public LuceneCommand apply(FileEvent aFileEvent) {
                if (aFileEvent.type == FileEvent.EventType.DELETED) {
                    return new LuceneCommand(aFileEvent, null);
                }

                Path thePath = aFileEvent.path;
                Content theContent = contentExtractor.extractContentFrom(thePath, aFileEvent.attributes);
                return new LuceneCommand(aFileEvent, theContent);
            }
        });

        // Ok, finally we add everything to the index
        theLuceneFlux.publishOn(Schedulers.newSingle("LuceneUpdater")).doOnNext(new Consumer<LuceneCommand>() {
            @Override
            public void accept(LuceneCommand aCommand) {
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
            }
        }).subscribe(new BaseSubscriber<LuceneCommand>() {
            @Override
            protected void hookOnSubscribe(Subscription aSubscription) {
                request(1);
            }

            @Override
            protected void hookOnNext(LuceneCommand aCommand) {
                LOGGER.info("Processed command for "  + aCommand.fileEvent.path);
                request(1);
            }

            @Override
            protected void hookOnError(Throwable aThrowable) {
                LOGGER.error("Flux went into error state", aThrowable);
            }
        });

        configurationUpdated(aConfiguration);
    }

    @Override
    public void configurationUpdated(Configuration aConfiguration) throws IOException {

        setIndexLocation(aConfiguration);

        configuration = aConfiguration;
        locations.values().stream().forEach(DirectoryWatcher::stopWatching);
        locations.clear();

        aConfiguration.getCrawlLocations().stream().forEach(e -> {
            File theDirectory = e.getDirectory();
            if (theDirectory.exists() && theDirectory.isDirectory()) {
                try {
                    add(e);
                } catch (IOException e1) {
                    LOGGER.error("Error setting filesystem location for " + theDirectory, e1);
                }
            }
        });
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    private void add(Configuration.CrawlLocation aLocation) throws IOException {
        locations.put(aLocation, new DirectoryWatcher(watchServiceCache, aLocation, DirectoryWatcher.DEFAULT_WAIT_FOR_ACTION, directoryListener, executorPool).startWatching());
    }

    private void setIndexLocation(Configuration aConfiguration) throws IOException {
        if (luceneIndexHandler != null) {
            shutdown();
        }
        AnalyzerCache theCache = new AnalyzerCache(aConfiguration);
        luceneIndexHandler = new LuceneIndexHandler(aConfiguration, theCache, executorPool, previewProcessor);
    }

    public void crawlLocations() throws IOException {

        luceneIndexHandler.crawlingStarts();

        Thread theRunner = new Thread() {
            @Override
            public void run() {

                try {
                    luceneIndexHandler.cleanupDeadContent();
                } catch (IOException e) {
                    LOGGER.error("Error removing dead content", e);
                }

                locations.values().stream().forEach(theWatcher -> {
                    try {
                        theWatcher.crawl();
                    } catch (Exception e) {
                        LOGGER.error("Error while crawling", e);
                    }
                });

                progressListener.crawlingFinished();
            }
        };
        theRunner.start();
    }

    public void shutdown() {
        luceneIndexHandler.shutdown();
    }

    public QueryResult performQuery(String aQueryString, String aBacklink, String aBasePath, Map<String, String> aDrilldownDimensions) throws IOException {
        return luceneIndexHandler.performQuery(aQueryString, aBacklink, aBasePath, configuration, aDrilldownDimensions);
    }

    public Suggestion[] findSuggestionTermsFor(String aTerm) throws IOException {
        return luceneIndexHandler.findSuggestionTermsFor(aTerm);
    }

    public File getFileOnDiskForDocument(String aDocumentID) throws IOException {
        return luceneIndexHandler.getFileOnDiskForDocument(aDocumentID);
    }
}