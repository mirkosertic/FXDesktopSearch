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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Backend {

    private FilesToIndexQueue filesToIndexQueue;
    private IndexWriterQueue indexWriterQueue;
    private LuceneIndexHandler luceneIndexHandler;
    private ContentExtractor contentExtractor;
    private List<FilesystemLocation> locations;
    private ProgressMonitor progressMonitor;
    private ProgressListener progressListener;

    private boolean includeSimilarDocuments;
    private int numberOfSearchResults;

    public Backend() {
        locations = new ArrayList<>();
        contentExtractor = new ContentExtractor();
        progressMonitor = new ProgressMonitor(new ProgressListener() {

            public void newFileFound(String aFilename, long aNumNewFiles, long aNumIndexedFiles) {
                if (progressListener != null) {
                    progressListener.newFileFound(aFilename, aNumNewFiles, aNumIndexedFiles);
                }
            }

            public void indexingProgress(long aNumNewFiles, long aNumIndexedFiles) {
                if (progressListener != null) {
                    progressListener.indexingProgress(aNumNewFiles, aNumIndexedFiles);
                }
            }

            public void crawlingFinished() {
                if (progressListener != null) {
                    progressListener.crawlingFinished();
                }
            }
        });
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void add(FilesystemLocation aLocation) {
        locations.add(aLocation);
    }

    public void setIndexLocation(File aFile) throws IOException {
        if (luceneIndexHandler != null) {
            shutdown();
        }
        luceneIndexHandler = new LuceneIndexHandler(aFile);
        indexWriterQueue = new IndexWriterQueue(progressMonitor, luceneIndexHandler);
        filesToIndexQueue = new FilesToIndexQueue(progressMonitor, indexWriterQueue, contentExtractor, luceneIndexHandler);
    }

    public void crawlLocations() throws IOException {

        luceneIndexHandler.crawlingStarts();

        progressMonitor.resetStats();

        final FileCrawler theCrawler = new FileCrawler(filesToIndexQueue, contentExtractor);

        Thread theRunner = new Thread() {
            @Override
            public void run() {
                for (FilesystemLocation theLocation : locations) {
                    theLocation.crawl(theCrawler);
                }
                progressMonitor.crawlingFinished();
            }
        };
        theRunner.start();
    }

    public void shutdown() {
        filesToIndexQueue.shutdown();
        indexWriterQueue.shutdown();
        luceneIndexHandler.shutdown();
    }

    public QueryResult performQuery(String aQueryString) throws IOException {
        return luceneIndexHandler.performQuery(aQueryString, includeSimilarDocuments, numberOfSearchResults);
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
        return Collections.unmodifiableList(locations);
    }

    public void setIncludeSimilarDocuments(boolean includeSimilarDocuments) {
        this.includeSimilarDocuments = includeSimilarDocuments;
    }

    public void remove(FilesystemLocation aLocation) {
        locations.remove(aLocation);
    }

    public DocFlareElement getDocFlare() throws IOException {
        return luceneIndexHandler.getDocFlare();
    }
}