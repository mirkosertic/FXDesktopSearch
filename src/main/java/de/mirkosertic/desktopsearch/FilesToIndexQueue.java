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

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FilesToIndexQueue {

    private static final int corePoolSize = Runtime.getRuntime().availableProcessors() * 5;
    private static final int maxPoolSize = Runtime.getRuntime().availableProcessors() * 5;
    private static final int keepAliveTime = 1000;
    private static final int workQueueSize = maxPoolSize;

    private ArrayBlockingQueue workQueue1;
    private ThreadPoolExecutor threadPoolExecutor1;

    private ProgressMonitor progressMonitor;
    private ContentExtractor contentExtractor;
    private IndexWriterQueue indexWriterQueue;
    private LuceneIndexHandler luceneIndexHandler;

    public FilesToIndexQueue(ProgressMonitor aProgressMonitor, IndexWriterQueue aWriterQueue, ContentExtractor aContentExtractor, LuceneIndexHandler aLuceneIndexHandler) {

        progressMonitor = aProgressMonitor;
        indexWriterQueue = aWriterQueue;
        contentExtractor = aContentExtractor;
        luceneIndexHandler = aLuceneIndexHandler;

        // Multi Threaded queue
        workQueue1 = new ArrayBlockingQueue(workQueueSize);
        threadPoolExecutor1 = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, workQueue1, new ThreadPoolExecutor.CallerRunsPolicy());

    }

    public void offer(final FilesToIndexQueueEntry aQueueEntry) {
        threadPoolExecutor1.execute(() -> {
            Path thePath = aQueueEntry.getPath();
            BasicFileAttributes theFileAttributes = aQueueEntry.getFileAttributes();

            String theSavedThreadName = Thread.currentThread().getName();
            String theFilename = thePath.toString();

            try {
                Thread.currentThread().setName("Indexing : " + theFilename);

                FileTime theFileTime = theFileAttributes.lastAccessTime();
                UpdateCheckResult theResult = luceneIndexHandler.checkIfModified(theFilename, theFileTime.toMillis());
                if (theResult == UpdateCheckResult.UPDATED) {
                    Content theContent = contentExtractor.extractContentFrom(thePath, theFileAttributes);
                    if (theContent != null) {
                        progressMonitor.addNewFileFound(thePath.toString());
                        indexWriterQueue.offer(new IndexWriterQueueEntry(theContent, aQueueEntry.getLocationId()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Thread.currentThread().setName(theSavedThreadName);
            }
        });
    }

    public void shutdown() {
        threadPoolExecutor1.shutdownNow();
    }
}
