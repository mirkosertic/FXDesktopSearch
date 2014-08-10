/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2013 Mirko Sertic
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses/>.
 */
package de.mirkosertic.desktopsearch;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class IndexWriterQueue {

    private static final int corePoolSize = 1;
    private static final int maxPoolSize = 2;
    private static final int keepAliveTime = 1000;
    private static final int workQueueSize = maxPoolSize;

    private ArrayBlockingQueue workQueue;
    private ThreadPoolExecutor threadPoolExecutor;

    private LuceneIndexHandler luceneIndexHandler;
    private ProgressMonitor progressMonitor;

    public IndexWriterQueue(ProgressMonitor aHandler, LuceneIndexHandler aLuceneIndexHandler) {
        luceneIndexHandler = aLuceneIndexHandler;
        progressMonitor = aHandler;
        workQueue = new ArrayBlockingQueue(workQueueSize);
        threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS,
                workQueue, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void offer(final IndexWriterQueueEntry aQueueEntry) {
        threadPoolExecutor.execute(() -> {

            String theOldThreadName = Thread.currentThread().getName();

            try {
                Thread.currentThread().setName(
                        "Storing " + aQueueEntry.getLocationId() + " : " + aQueueEntry.getContent().getFileName());

                progressMonitor.addFilesIndexed();
                luceneIndexHandler.addToIndex(aQueueEntry.getLocationId(), aQueueEntry.getContent());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Thread.currentThread().setName(theOldThreadName);
            }
        });
    }

    public void shutdown() {
        threadPoolExecutor.shutdown();
    }
}