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

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHasher;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class LocalDirectoryWatcher {

    public static final int DEFAULT_WAIT_FOR_ACTION = 5;

    private static class ActionTimer {

        private DirectoryChangeEvent.EventType eventType;
        private int waitForAction;

        public ActionTimer(final DirectoryChangeEvent.EventType aKind, final int aWaitForAction) {
            eventType = aKind;
            waitForAction = aWaitForAction;
        }

        public void reset(final DirectoryChangeEvent.EventType aKind, final int aWaitForAction) {
            eventType = aKind;
            waitForAction = aWaitForAction;
        }

        public boolean runOneCycle() {
            return (--waitForAction <= 0);
        }
    }

    private final Thread monitorThread;
    private final Map<Path, ActionTimer> fileTimers;
    private final int waitForAction;
    private final Timer actionTimer;
    private final DirectoryListener directoryListener;
    private final Configuration.CrawlLocation filesystemLocation;
    private final DirectoryWatcher directoryWatcher;
    private CompletableFuture<Void> watcherFuture;

    public LocalDirectoryWatcher(final Configuration.CrawlLocation aFileSystemLocation, final int aWaitForAction, final DirectoryListener aDirectoryListener) throws IOException {
        fileTimers = new HashMap<>();
        waitForAction = aWaitForAction;
        directoryListener = aDirectoryListener;
        filesystemLocation = aFileSystemLocation;

        directoryWatcher = DirectoryWatcher
                .builder()
                .path(filesystemLocation.getDirectory().toPath())
                .logger(log)
                .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                .listener(event -> publishActionFor(event.path(), event.eventType()))
                .build();

        monitorThread = new Thread("Index-Monitor") {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        synchronized (fileTimers) {
                            final var size = fileTimers.size();
                            if (size > 0) {
                                log.info("Currently {} files in index queue...", size);
                            }
                        }
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        log.debug("Waiting interrupted", e);
                    }
                }
            }
        };

        actionTimer = new Timer();
    }

    private void publishActionFor(final Path aPath, final DirectoryChangeEvent.EventType aEventType ) {
        log.debug("Got event {} for path {}", aEventType, aPath);
        synchronized (fileTimers) {
            final var theTimer = fileTimers.get(aPath);
            if (theTimer == null) {
                fileTimers.put(aPath, new ActionTimer(aEventType, waitForAction));
            } else {
                theTimer.reset(aEventType, waitForAction);
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
                        if (theEntry.getValue().eventType == DirectoryChangeEvent.EventType.CREATE) {
                            directoryListener.fileCreatedOrModified(filesystemLocation, theEntry.getKey());
                        }
                        if (theEntry.getValue().eventType == DirectoryChangeEvent.EventType.DELETE) {
                            directoryListener.fileDeleted(filesystemLocation, theEntry.getKey());
                        }
                        if (theEntry.getValue().eventType == DirectoryChangeEvent.EventType.MODIFY) {
                            directoryListener.fileCreatedOrModified(filesystemLocation, theEntry.getKey());
                        }
                    }
                }
            });
            theKeysToRemove.forEach(fileTimers::remove);
        }
    }

    public LocalDirectoryWatcher startWatching() {

        watcherFuture = directoryWatcher.watchAsync();

        monitorThread.start();
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
        monitorThread.interrupt();
        if (directoryWatcher != null) {
            try {
                watcherFuture.cancel(true);
            } catch (final Exception e) {
            }
            try {
                directoryWatcher.close();
            } catch (final Exception e) {
                // Do nothing here
            }
        }
    }

    public void crawl() throws IOException {

        final var thePath = filesystemLocation.getDirectory().toPath();
        log.info("Crawling {}", thePath);
        Files.walk(thePath).forEach(aPath -> {
            if (!Files.isDirectory(aPath)) {
                publishActionFor(aPath, DirectoryChangeEvent.EventType.MODIFY);
            }
        });
    }
}
