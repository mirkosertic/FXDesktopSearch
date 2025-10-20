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

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@Component
public class Backend implements ConfigurationChangeListener {

    public static class FileEvent {
        public enum EventType {
            UPDATED, DELETED
        }
        private final Configuration.CrawlLocation crawlLocation;
        private final Path path;
        private final EventType type;
        private final BasicFileAttributes attributes;

        public FileEvent(final Configuration.CrawlLocation crawlLocation, final Path crawlPath, final BasicFileAttributes fileAttributes, final EventType eventType) {
            this.crawlLocation = crawlLocation;
            path = crawlPath;
            type = eventType;
            attributes = fileAttributes;
        }
    }

    public static class LuceneCommand {

        private final FileEvent fileEvent;
        private final Content content;

        public LuceneCommand(final FileEvent fileEvent, final Content fileContent) {
            this.fileEvent = fileEvent;
            content = fileContent;
        }
    }

    private LuceneIndexHandler luceneIndexHandler;
    private final ContentExtractor contentExtractor;
    private ProgressListener progressListener;
    private final Map<Configuration.CrawlLocation, LocalDirectoryWatcher> locations;
    private final PreviewProcessor previewProcessor;
    private Configuration configuration;
    private final DirectoryListener directoryListener;
    private final Statistics statistics;
    private Thread progressInfo;
    private final Consumer<FileEvent> processingPipeline;

    class SimpleDirectoryListener implements DirectoryListener {

        public SimpleDirectoryListener() {
        }

        @Override
        public void fileDeleted(final Configuration.CrawlLocation crawlLocation, final Path deletedFile) {
            synchronized (this) {
                try {
                    log.info("File deleted {}", deletedFile);
                    if (contentExtractor.supportsFile(deletedFile.toString())) {
                        log.info("Deleting file from index");
                        statistics.newDeletedFileJob();

                        processingPipeline.accept(new FileEvent(crawlLocation, deletedFile, null, FileEvent.EventType.DELETED));
                        log.info("Deleting file done");
                    } else {
                        log.info("File {} has no supported file type", deletedFile);
                    }
                } catch (final Exception e) {
                    log.error("Error processing file {}", deletedFile, e);
                }
            }
        }

        @Override
        public void fileCreatedOrModified(final Configuration.CrawlLocation crawlLocation, final Path createdOrModifiedFile) {
            synchronized (this) {
                try {
                    log.info("File created or modified {}", createdOrModifiedFile);
                    if (contentExtractor.supportsFile(createdOrModifiedFile.toString())) {
                        log.info("Reindexing file");
                        final var theAttributes = Files.readAttributes(createdOrModifiedFile, BasicFileAttributes.class);

                        statistics.newModifiedFileJob();

                        processingPipeline.accept(new FileEvent(crawlLocation, createdOrModifiedFile, theAttributes, FileEvent.EventType.UPDATED));
                        log.info("Reindexing file done");
                    } else {
                        log.info("File {} has no supported file type", createdOrModifiedFile);
                    }
                } catch (final Exception e) {
                    log.error("Error processing file {}", createdOrModifiedFile, e);
                }
            }
        }
    }

    class UpdatedFilter implements Predicate<Backend.FileEvent> {
        @Override
        public boolean test(final FileEvent fileEvent) {
            // Always keep delete file events
            if (fileEvent.type == FileEvent.EventType.DELETED) {
                return true;
            }
            final var thePath = fileEvent.path;
            final var theFileName = thePath.toString();

            try {
                final var theUpdateCheckResult = luceneIndexHandler
                        .checkIfModified(theFileName, fileEvent.attributes.lastModifiedTime().toMillis());

                final var result = theUpdateCheckResult == UpdateCheckResult.UPDATED;
                if (!result) {
                    log.info("File seems not to have changed, skipping it ({}).", fileEvent.path);
                    statistics.jobSkipped();
                }
                return result;
            } catch (final Exception e) {
                throw new RuntimeException();
            }
        }
    }

    class EventContentExtractor implements Function<FileEvent, LuceneCommand> {
        @Override
        public LuceneCommand apply(final FileEvent fileEvent) {
            if (fileEvent.type == FileEvent.EventType.DELETED) {
                return new LuceneCommand(fileEvent, null);
            }

            final var thePath = fileEvent.path;
            log.info("Extracting content from {}", thePath);
            final var theContent = contentExtractor.extractContentFrom(thePath, fileEvent.attributes);
            log.info("Extracting content done");
            return new LuceneCommand(fileEvent, theContent);
        }
    }

    class IndexUpdater implements Consumer<LuceneCommand> {
        @Override
        public void accept(final LuceneCommand command) {
            if (command.fileEvent.type == FileEvent.EventType.DELETED) {
                try {
                    luceneIndexHandler.removeFromIndex(command.fileEvent.path.toString());

                    final String message = "Deleted " + command.fileEvent.path.getFileName();
                    progressListener.infotext(message);

                    log.info(message);

                } catch (final Exception e) {

                    final String message = "Error deleting " + command.fileEvent.path.getFileName();
                    progressListener.infotext(message);

                    log.error(message, e);
                }
            } else {
                if (command.content != null) {
                    try {
                        luceneIndexHandler.addToIndex(command.fileEvent.crawlLocation.getId(), command.content);

                        final String message = "Reindexed " + command.fileEvent.path.getFileName();

                        progressListener.infotext(message);

                        log.info(message);

                    } catch (final Exception e) {

                        final String message = "Error re-inxeding " + command.fileEvent.path.getFileName();

                        progressListener.infotext(message);

                        log.error(message, e);
                    }
                }
            }
        }
    }

    public Backend(final ConfigurationManager configurationManager, final PreviewProcessor aPreviewProcessor) throws IOException {
        this.previewProcessor = aPreviewProcessor;
        this.locations = new HashMap<>();

        final var configuration = configurationManager.getConfiguration();
        configurationManager.addChangeListener(this);

        this.contentExtractor = new ContentExtractor(configuration);
        this.statistics = new Statistics();

        this.directoryListener = new SimpleDirectoryListener();

        final UpdatedFilter updatedFilter = new UpdatedFilter();
        final EventContentExtractor eventContentExtractor = new EventContentExtractor();
        final IndexUpdater indexUpdater = new IndexUpdater();

        this.processingPipeline = fileEvent -> {
            try {
                if (updatedFilter.test(fileEvent)) {
                    final LuceneCommand theCommand = eventContentExtractor.apply(fileEvent);
                    indexUpdater.accept(theCommand);
                }
            } catch (final Exception e) {
                log.error("Error processing file {}", fileEvent.path, e);
            }
        };

        configurationUpdated(configuration);
    }

    @Override
    public void configurationUpdated(final Configuration changedConfiguration) throws IOException {

        setIndexLocation(changedConfiguration);

        configuration = changedConfiguration;
        locations.values().forEach(LocalDirectoryWatcher::stopWatching);
        locations.clear();

        changedConfiguration.getCrawlLocations().forEach(e -> {
            final var theDirectory = e.getDirectory();
            if (theDirectory.exists() && theDirectory.isDirectory()) {
                try {
                    add(e);
                } catch (final IOException e1) {
                    log.error("Error setting filesystem location for {}", theDirectory, e1);
                }
            }
        });
    }

    public void setProgressListener(final ProgressListener progressListener) {
        this.progressListener = progressListener;

        progressInfo = new Thread("ProgressInfo") {
            @Override
            public void run() {

                long lastRemaining = -1;
                final var format = NumberFormat.getIntegerInstance();
                var lastMessage = "";

                while (!isInterrupted()) {

                    final var totalJobs = statistics.totalJobs();
                    final var completedJobs = statistics.completedJobs();

                    final var remaining = Math.max(totalJobs - completedJobs, 0);

                    if (remaining > 0) {
                        if (lastRemaining == -1) {
                            lastMessage = remaining + " Files are still in the indexing queue.";
                            Backend.this.progressListener.infotext(lastMessage);
                        } else {
                            final var thruput = lastRemaining - remaining;
                            if (thruput > 0) {
                                final var eta = ((double) remaining) / thruput;
                                lastMessage = remaining + " Files are still in the indexing queue, " + format.format(eta) + " seconds remaining (ETA).";
                                Backend.this.progressListener.infotext(lastMessage);
                            } else {
                                if (!lastMessage.isEmpty()) {
                                    Backend.this.progressListener.infotext(lastMessage);
                                }
                            }
                        }
                    } else {
                        lastMessage = "";
                    }

                    lastRemaining = remaining;

                    try {
                        sleep(1000);
                    } catch (final InterruptedException e) {
                        // Do nothing here...
                    }
                }
            }
        };
        progressInfo.start();
    }

    private void add(final Configuration.CrawlLocation crawlLocation) throws IOException {
        locations.put(crawlLocation, new LocalDirectoryWatcher(crawlLocation, LocalDirectoryWatcher.DEFAULT_WAIT_FOR_ACTION, directoryListener).startWatching());
    }

    private void setIndexLocation(final Configuration configuration) throws IOException {
        if (luceneIndexHandler != null) {
            shutdown();
        }
        luceneIndexHandler = new LuceneIndexHandler(configuration, previewProcessor);
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


    @PreDestroy
    public void shutdown() {
        log.info("Shutting down backend");
        if (progressInfo != null) {
            progressInfo.interrupt();
        }
        luceneIndexHandler.shutdown();
    }

    public QueryResult performQuery(final String queryString, final MultiValueMap<String, String> drilldownDimensions) {
        return luceneIndexHandler.performQuery(queryString, configuration, drilldownDimensions);
    }

    public Suggestion[] findSuggestionTermsFor(final String term) {
        return luceneIndexHandler.findSuggestionTermsFor(term);
    }

    public File getFileOnDiskForDocument(final String luceneDocumentId) {
        return luceneIndexHandler.getFileOnDiskForDocument(luceneDocumentId);
    }

    public String highlightMatch(final String queryString, final int luceneDocumentId) {
        return luceneIndexHandler.highlight(queryString, luceneDocumentId);
    }
}
