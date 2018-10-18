/*
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

class Backend implements ConfigurationChangeListener {

    public static class FileEvent {
        public enum EventType {
            UPDATED, DELETED;
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

    public Backend(final Notifier aNotifier, final Configuration aConfiguration, final PreviewProcessor aPreviewProcessor) throws IOException {
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
                public void fileDeleted(final Configuration.CrawlLocation aLocation, final Path aFile) {
                    synchronized (this) {
                        try {
                            if (contentExtractor.supportsFile(aFile.toString())) {
                                final BasicFileAttributes theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);
                                sink.next(new FileEvent(aLocation, aFile, theAttributes, FileEvent.EventType.DELETED));
                            }
                        } catch (final Exception e) {
                            LOGGER.error("Error processing file " + aFile, e);
                        }
                    }
                }

                @Override
                public void fileCreatedOrModified(final Configuration.CrawlLocation aLocation, final Path aFile) {
                    synchronized (this) {
                        try {
                            if (contentExtractor.supportsFile(aFile.toString())) {
                                final BasicFileAttributes theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);
                                sink.next(new FileEvent(aLocation, aFile, theAttributes, FileEvent.EventType.UPDATED));
                            }
                        } catch (final Exception e) {
                            LOGGER.error("Error processing file " + aFile, e);
                        }
                    }
                }

                @Override
                public void fileFoundByCrawler(final Configuration.CrawlLocation aLocation, final Path aFile) {
                    synchronized (this) {
                        try {
                            if (contentExtractor.supportsFile(aFile.toString())) {
                                final BasicFileAttributes theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);
                                sink.next(new FileEvent(aLocation, aFile, theAttributes, FileEvent.EventType.UPDATED));
                            }
                        } catch (final Exception e) {
                            LOGGER.error("Error processing file " + aFile, e);
                        }
                    }
                }
            };
        });

        // Filter update events for Files that were not changed
        theFileEventFlux = theFileEventFlux.filter(aFileEvent -> {
            // Always keep delete file events
            if (aFileEvent.type == FileEvent.EventType.DELETED) {
                return true;
            }
            final Path thePath = aFileEvent.path;
            final String theFileName = thePath.toString();

            try {
                final UpdateCheckResult theUpdateCheckResult = luceneIndexHandler
                        .checkIfModified(theFileName, aFileEvent.attributes.lastModifiedTime().toMillis());
                return theUpdateCheckResult == UpdateCheckResult.UPDATED;
            } catch (final Exception e) {
                throw Exceptions.propagate(e);
            }
        });

        // Ok, we now map the file events to lucene commands
        final Flux<LuceneCommand> theLuceneFlux = theFileEventFlux.publishOn(Schedulers.newSingle("ContentExtractor")).map(
                aFileEvent -> {
                    if (aFileEvent.type == FileEvent.EventType.DELETED) {
                        return new LuceneCommand(aFileEvent, null);
                    }

                    final Path thePath = aFileEvent.path;
                    final Content theContent = contentExtractor.extractContentFrom(thePath, aFileEvent.attributes);
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
                LOGGER.info("Processed command for " + aCommand.fileEvent.path);
                request(1);
            }

            @Override
            protected void hookOnError(final Throwable aThrowable) {
                LOGGER.error("Flux went into error state", aThrowable);
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
            final File theDirectory = e.getDirectory();
            if (theDirectory.exists() && theDirectory.isDirectory()) {
                try {
                    add(e);
                } catch (final IOException e1) {
                    LOGGER.error("Error setting filesystem location for " + theDirectory, e1);
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

    public void crawlLocations() throws IOException {

        luceneIndexHandler.crawlingStarts();

        final Thread theRunner = new Thread(() -> {

            LOGGER.info("Startint to crawl");;
            locations.values().forEach(theWatcher -> {
                try {
                    theWatcher.crawl();
                } catch (final Exception e) {
                    LOGGER.error("Error while crawling", e);
                }
            });

            progressListener.crawlingFinished();
        });
        theRunner.start();
    }

    public void shutdown() {
        luceneIndexHandler.shutdown();
    }

    public QueryResult performQuery(final String aQueryString, final String aBacklink, final String aBasePath, final Map<String, String> aDrilldownDimensions) throws IOException {
        return luceneIndexHandler.performQuery(aQueryString, aBacklink, aBasePath, configuration, aDrilldownDimensions);
    }

    public Suggestion[] findSuggestionTermsFor(final String aTerm) throws IOException {
        return luceneIndexHandler.findSuggestionTermsFor(aTerm);
    }

    public File getFileOnDiskForDocument(final String aDocumentID) throws IOException {
        return luceneIndexHandler.getFileOnDiskForDocument(aDocumentID);
    }
}