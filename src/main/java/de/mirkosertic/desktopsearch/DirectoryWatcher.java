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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class DirectoryWatcher {

    private static final Logger LOGGER = Logger.getLogger(DirectoryWatcher.class);

    public static final int DEFAULT_WAIT_FOR_ACTION = 5;

    private static class ActionTimer {

        private WatchEvent.Kind kind;
        private int waitForAction;

        public ActionTimer(WatchEvent.Kind aKind, int aWaitForAction) {
            kind = aKind;
            waitForAction = aWaitForAction;
        }

        public void reset(WatchEvent.Kind aKind, int aWaitForAction) {
            kind = aKind;
            waitForAction = aWaitForAction;
        }

        public boolean runOneCycle() {
            return (--waitForAction <= 0);
        }
    }

    private final WatchService watchService;
    private final Thread watcherThread;
    private final Map<Path, ActionTimer> fileTimers;
    private final int waitForAction;
    private final Timer actionTimer;
    private final DirectoryListener directoryListener;
    private final Configuration.CrawlLocation filesystemLocation;
    private final ExecutorPool executorPool;

    public DirectoryWatcher(WatchServiceCache aWatchServiceCache, Configuration.CrawlLocation aFileSystemLocation, int aWaitForAction, DirectoryListener aDirectoryListener, ExecutorPool aExecutorPool) throws IOException {
        executorPool = aExecutorPool;
        fileTimers = new HashMap<>();
        waitForAction = aWaitForAction;
        directoryListener = aDirectoryListener;
        filesystemLocation = aFileSystemLocation;

        Path thePath = aFileSystemLocation.getDirectory().toPath();

        watchService = aWatchServiceCache.getWatchServiceFor(thePath);
        watcherThread = new Thread("WatcherThread-"+thePath) {
            @Override
            public void run() {
                while(!isInterrupted()) {

                    try {
                        WatchKey theKey = watchService.take();
                        Path theParent = (Path) theKey.watchable();
                        theKey.pollEvents().stream().forEach(theEvent -> {
                            if (theEvent.kind() == StandardWatchEventKinds.OVERFLOW) {
                                LOGGER.warn("Overflow for " + theEvent.context() + " count = " + theEvent.count());
                                // Overflow events are not handled
                            } else {
                                Path thePath = theParent.resolve((Path) theEvent.context());
                                LOGGER.debug(theEvent.kind() + " for " + theEvent.context() + " count = " + theEvent.count());

                                publishActionFor(thePath, theEvent.kind());
                            }
                        });
                        theKey.reset();

                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        LOGGER.debug("Has been interrupted");
                    }
                }
            }
        };
        actionTimer = new Timer();
    }

    private void publishActionFor(Path aPath, WatchEvent.Kind aKind ) {
        synchronized (fileTimers) {
            ActionTimer theTimer = fileTimers.get(aPath);
            if (theTimer == null) {
                fileTimers.put(aPath, new ActionTimer(aKind, waitForAction));
            } else {
                theTimer.reset(aKind, waitForAction);
            }
        }
    }

    private void actionCountDown() {
        synchronized (fileTimers) {
            Set<Path> theKeysToRemove = new HashSet<>();
            fileTimers.entrySet().stream().forEach(theEntry -> {
                if (theEntry.getValue().runOneCycle()) {
                    theKeysToRemove.add(theEntry.getKey());
                    if (!Files.isDirectory(theEntry.getKey())) {
                        if (theEntry.getValue().kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            directoryListener.fileCreatedOrModified(filesystemLocation, theEntry.getKey());
                        }
                        if (theEntry.getValue().kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            directoryListener.fileDeleted(filesystemLocation, theEntry.getKey());
                        }
                        if (theEntry.getValue().kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            directoryListener.fileCreatedOrModified(filesystemLocation, theEntry.getKey());
                        }
                    } else {
                        try {
                            if (theEntry.getValue().kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                registerWatcher(theEntry.getKey());
                            }
                            if (theEntry.getValue().kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                registerWatcher(theEntry.getKey());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            theKeysToRemove.forEach(fileTimers::remove);
        }
    }

    private void registerWatcher(Path aDirectory) throws IOException {
        LOGGER.info("New watchable directory detected : " + aDirectory);
        aDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    public DirectoryWatcher startWatching() {

        Thread theRegisterWatchers = new Thread("Registering Watchers") {
            @Override
            public void run() {
                try {
                    Files.walk(filesystemLocation.getDirectory().toPath()).forEach(path -> {
                        if (Files.isDirectory(path)) {
                            LOGGER.info("Registering watches for " + path);
                            try {
                                registerWatcher(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                } catch (IOException e) {
                    LOGGER.error("Error registering file watcher", e);
                }
            }
        };
        theRegisterWatchers.start();

        watcherThread.start();
        actionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                actionCountDown();
            }
        }, 1000, 1000);
        return this;
    }

    public void stopWatching() {
        actionTimer.cancel();
        watcherThread.interrupt();
    }

    public void crawl() throws IOException {

        Path thePath = filesystemLocation.getDirectory().toPath();

        Files.walk(thePath).forEach(aPath -> {
            if (!Files.isDirectory(aPath)) {
                executorPool.execute(() -> directoryListener.fileFoundByCrawler(filesystemLocation, aPath));
            }
        });
    }
}
