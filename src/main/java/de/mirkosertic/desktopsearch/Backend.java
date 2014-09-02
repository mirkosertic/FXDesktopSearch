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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

class Backend implements ConfigurationChangeListener {

    private static final Logger LOGGER = Logger.getLogger(Backend.class);

    private LuceneIndexHandler luceneIndexHandler;
    private final ContentExtractor contentExtractor;
    private ProgressListener progressListener;
    private final Map<Configuration.CrawlLocation, DirectoryWatcher> locations;
    private final DirectoryListener directoryListener;
    private final ExecutorPool executorPool;
    private final Notifier notifier;
    private Configuration configuration;

    public Backend(Notifier aNotifier, Configuration aConfiguration) throws IOException {
        notifier = aNotifier;
        locations = new HashMap<>();
        executorPool = new ExecutorPool();
        contentExtractor = new ContentExtractor(aConfiguration);
        directoryListener = new DirectoryListener() {
            @Override
            public void fileDeleted(Configuration.CrawlLocation aFileSystemLocation, Path aFile) {
                try {
                    String theFilename = aFile.toString();
                    if (luceneIndexHandler.checkIfExists(theFilename)) {
                        luceneIndexHandler.removeFromIndex(theFilename);
                        aNotifier.showInformation("Deleted " + aFile.getFileName());
                    }
                } catch (Exception e) {
                    aNotifier.showError("Error removing " + aFile.getFileName(), e);
                }
            }

            @Override
            public void fileFoundByCrawler(Configuration.CrawlLocation aLocation, Path aFile) {
                fileCreatedOrModified(aLocation, aFile, false);
            }

            @Override
            public void fileCreatedOrModified(Configuration.CrawlLocation aLocation, Path aFile) {
                fileCreatedOrModified(aLocation, aFile, true);
            }

            private void fileCreatedOrModified(Configuration.CrawlLocation aLocation, Path aFile, boolean aShowInformation) {
                String theFileName = aFile.toString();
                if (contentExtractor.supportsFile(theFileName)) {
                    try {
                        progressListener.newFileFound(theFileName);

                        BasicFileAttributes theAttributes = Files.readAttributes(aFile, BasicFileAttributes.class);
                        UpdateCheckResult theUpdateCheckResult = luceneIndexHandler.checkIfModified(theFileName, theAttributes.size());
                        if (theUpdateCheckResult == UpdateCheckResult.UPDATED) {

                            if (aShowInformation) {
                                notifier.showInformation("Reindexed " + aFile.getFileName());
                            }

                            Content theContent = contentExtractor.extractContentFrom(aFile, theAttributes);
                            if (theContent != null) {
                                luceneIndexHandler.addToIndex(aLocation.getId(), theContent);
                            }
                        } else {
                            LOGGER.info("File " + aFile+" was modified, but Index Status is " + theUpdateCheckResult);
                        }
                    } catch (Exception e) {
                        aNotifier.showError("Error re-inxeding " + aFile.getFileName(), e);
                    }
                }
            }
        };
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
        locations.put(aLocation, new DirectoryWatcher(aLocation, DirectoryWatcher.DEFAULT_WAIT_FOR_ACTION, directoryListener, executorPool).startWatching());
    }

    private void setIndexLocation(Configuration aConfiguration) throws IOException {
        if (luceneIndexHandler != null) {
            shutdown();
        }
        AnalyzerCache theCache = new AnalyzerCache(aConfiguration);
        luceneIndexHandler = new LuceneIndexHandler(aConfiguration.getIndexDirectory(), theCache);
    }

    public void crawlLocations() throws IOException {

        luceneIndexHandler.crawlingStarts();

        Thread theRunner = new Thread() {
            @Override
            public void run() {

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
}