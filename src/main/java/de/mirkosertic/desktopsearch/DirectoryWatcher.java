package de.mirkosertic.desktopsearch;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class DirectoryWatcher {

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
    private final FilesystemLocation filesystemLocation;
    private final ExecutorPool executorPool;

    public DirectoryWatcher(FilesystemLocation aFileSystemLocation, int aWaitForAction, DirectoryListener aDirectoryListener, ExecutorPool aExecutorPool) throws IOException {
        executorPool = aExecutorPool;
        fileTimers = new HashMap<>();
        waitForAction = aWaitForAction;
        directoryListener = aDirectoryListener;
        filesystemLocation = aFileSystemLocation;

        Path thePath = aFileSystemLocation.getDirectory().toPath();

        watchService = thePath.getFileSystem().newWatchService();
        Files.walk(thePath).forEach(path -> {
            if (Files.isDirectory(path)) {
                System.out.println("Registering watches for " + path);
                try {
                    registerWatcher(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        watcherThread = new Thread("WatcherThread-"+thePath) {
            @Override
            public void run() {
                while(!isInterrupted()) {

                    try {
                        WatchKey theKey = watchService.take();
                        Path theParent = (Path) theKey.watchable();
                        theKey.pollEvents().stream().forEach(theEvent -> {
                            if (theEvent.kind() == StandardWatchEventKinds.OVERFLOW) {
                                System.out.println("Overflow for "+theEvent.context()+" count = "+theEvent.count());
                                // Overflow events are not handled
                            } else {
                                Path thePath = theParent.resolve((Path) theEvent.context());
                                System.out.println(theEvent.kind() + " for " + theEvent.context() + " count = " + theEvent.count());

                                publishActionFor(thePath, theEvent.kind());
                            }
                        });
                        theKey.reset();

                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Has been interrupted");
                        // This will stop the thread
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
            for (Path thePath : theKeysToRemove) {
                fileTimers.remove(thePath);
            }
        }
    }

    private void registerWatcher(Path aDirectory) throws IOException {
        aDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    public DirectoryWatcher startWatching() {
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
                executorPool.execute(() -> {
                    directoryListener.fileCreatedOrModified(filesystemLocation, aPath);
                });
            }
        });
    }
}
