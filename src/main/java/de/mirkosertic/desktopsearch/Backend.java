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
import java.util.*;

class Backend {

    private static final Logger LOGGER = Logger.getLogger(Backend.class);

    private LuceneIndexHandler luceneIndexHandler;
    private final ContentExtractor contentExtractor;
    private ProgressListener progressListener;
    private final Map<FilesystemLocation, DirectoryWatcher> locations;
    private final DirectoryListener directoryListener;
    private final ExecutorPool executorPool;
    private final Notifier notifier;

    private boolean includeSimilarDocuments;
    private int numberOfSearchResults;

    public Backend(Notifier aNotifier) {
        notifier = aNotifier;
        locations = new HashMap<>();
        executorPool = new ExecutorPool();
        contentExtractor = new ContentExtractor();
        directoryListener = new DirectoryListener() {
            @Override
            public void fileDeleted(FilesystemLocation aFileSystemLocation, Path aFile) {
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
            public void fileFoundByCrawler(FilesystemLocation aFileSystemLocation, Path aFile) {
                fileCreatedOrModified(aFileSystemLocation, aFile, false);
            }

            @Override
            public void fileCreatedOrModified(FilesystemLocation aFileSystemLocation, Path aFile) {
                fileCreatedOrModified(aFileSystemLocation, aFile, true);
            }

            private void fileCreatedOrModified(FilesystemLocation aFileSystemLocation, Path aFile, boolean aShowInformation) {
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
                                luceneIndexHandler.addToIndex(aFileSystemLocation.getId(), theContent);
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
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void add(FilesystemLocation aLocation) throws IOException {
        locations.put(aLocation, new DirectoryWatcher(aLocation, DirectoryWatcher.DEFAULT_WAIT_FOR_ACTION, directoryListener, executorPool).startWatching());
    }

    public void setIndexLocation(File aFile) throws IOException {
        if (luceneIndexHandler != null) {
            shutdown();
        }
        luceneIndexHandler = new LuceneIndexHandler(aFile);
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
                        e.printStackTrace();
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
        return luceneIndexHandler.performQuery(aQueryString, aBacklink, aBasePath, includeSimilarDocuments, numberOfSearchResults, aDrilldownDimensions);
    }

    public boolean isIncludeSimilarDocuments() {
        return includeSimilarDocuments;
    }

    public int getNumberOfSearchResults() {
        return numberOfSearchResults;
    }

    public void setNumberOfSearchResults(int numberOfSearchResults) {
        this.numberOfSearchResults = numberOfSearchResults;
    }

    public String getIndexLocation() {
        return luceneIndexHandler.getIndexLocation().toString();
    }

    public List<FilesystemLocation> getFileSystemLocations() {
        return Collections.unmodifiableList(new ArrayList<>(locations.keySet()));
    }

    public void setIncludeSimilarDocuments(boolean includeSimilarDocuments) {
        this.includeSimilarDocuments = includeSimilarDocuments;
    }

    public void remove(FilesystemLocation aLocation) {
        DirectoryWatcher theWatcher = locations.get(aLocation);
        if (theWatcher != null) {
            theWatcher.stopWatching();
            locations.remove(aLocation);
        }
    }
}