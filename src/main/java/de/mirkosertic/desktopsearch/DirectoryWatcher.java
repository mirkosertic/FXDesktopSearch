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

@Slf4j
public class DirectoryWatcher {

    public static final int DEFAULT_WAIT_FOR_ACTION = 5;

    private static class ActionTimer {

        private WatchEvent.Kind kind;
        private int waitForAction;

        public ActionTimer(final WatchEvent.Kind aKind, final int aWaitForAction) {
            kind = aKind;
            waitForAction = aWaitForAction;
        }

        public void reset(final WatchEvent.Kind aKind, final int aWaitForAction) {
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

    public DirectoryWatcher(final WatchServiceCache aWatchServiceCache, final Configuration.CrawlLocation aFileSystemLocation, final int aWaitForAction, final DirectoryListener aDirectoryListener, final ExecutorPool aExecutorPool) throws IOException {
        executorPool = aExecutorPool;
        fileTimers = new HashMap<>();
        waitForAction = aWaitForAction;
        directoryListener = aDirectoryListener;
        filesystemLocation = aFileSystemLocation;

        final var thePath = aFileSystemLocation.getDirectory().toPath();

        watchService = aWatchServiceCache.getWatchServiceFor(thePath);
        watcherThread = new Thread("WatcherThread-"+thePath) {
            @Override
            public void run() {
                while(!isInterrupted()) {

                    try {
                        final var theKey = watchService.take();
                        final var theParent = (Path) theKey.watchable();
                        theKey.pollEvents().stream().forEach(theEvent -> {
                            if (theEvent.kind() == StandardWatchEventKinds.OVERFLOW) {
                                log.warn("Overflow for {} count = {}", theEvent.context(), theEvent.count());
                                // Overflow events are not handled
                            } else {
                                final var thePath = theParent.resolve((Path) theEvent.context());
                                log.debug("{} for {} count = {}", theEvent.kind(), theEvent.context(), theEvent.count());

                                publishActionFor(thePath, theEvent.kind());
                            }
                        });
                        theKey.reset();

                        Thread.sleep(10000);
                    } catch (final InterruptedException e) {
                        log.debug("Has been interrupted");
                    }
                }
            }
        };
        actionTimer = new Timer();
    }

    private void publishActionFor(final Path aPath, final WatchEvent.Kind aKind ) {
        synchronized (fileTimers) {
            final var theTimer = fileTimers.get(aPath);
            if (theTimer == null) {
                fileTimers.put(aPath, new ActionTimer(aKind, waitForAction));
            } else {
                theTimer.reset(aKind, waitForAction);
            }
        }
    }

    private void actionCountDown() {
        synchronized (fileTimers) {
            final Set<Path> theKeysToRemove = new HashSet<>();
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
                        } catch (final IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            theKeysToRemove.forEach(fileTimers::remove);
        }
    }

    private void registerWatcher(final Path aDirectory) throws IOException {
        log.info("New watchable directory detected : {}", aDirectory);
        aDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    public DirectoryWatcher startWatching() {

        final Thread theRegisterWatchers = new Thread("Registering Watchers") {
            @Override
            public void run() {
                try {
                    Files.walk(filesystemLocation.getDirectory().toPath()).forEach(path -> {
                        if (Files.isDirectory(path)) {
                            log.info("Registering watches for {}", path);
                            try {
                                registerWatcher(path);
                            } catch (final IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                } catch (final IOException e) {
                    log.error("Error registering file watcher", e);
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

        final var thePath = filesystemLocation.getDirectory().toPath();

        Files.walk(thePath).forEach(aPath -> {
            if (!Files.isDirectory(aPath)) {
                executorPool.execute(() -> directoryListener.fileFoundByCrawler(filesystemLocation, aPath));
            }
        });
    }
}
