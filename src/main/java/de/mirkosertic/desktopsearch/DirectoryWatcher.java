package de.mirkosertic.desktopsearch;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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

    public DirectoryWatcher(Path aPath, int aWaitForAction) throws IOException {
        fileTimers = new HashMap<>();
        waitForAction = aWaitForAction;
        watchService = aPath.getFileSystem().newWatchService();
        Files.walkFileTree(aPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path aDirectory, BasicFileAttributes aAttributes) throws IOException {
                System.out.println("Registering watches for " + aDirectory);
                registerWatcher(aDirectory);
                return FileVisitResult.CONTINUE;
            }
        });
        watcherThread = new Thread("WatcherThread-"+aPath) {
            @Override
            public void run() {
                while(!isInterrupted()) {

                    try {
                        WatchKey theKey = watchService.take();
                        for (WatchEvent theEvent : theKey.pollEvents()) {
                            if (theEvent.kind() == StandardWatchEventKinds.OVERFLOW) {
                                System.out.println("Overflow for "+theEvent.context()+" count = "+theEvent.count());
                                // Overflow events are not handled
                                continue;
                            }

                            Path thePath = (Path) theEvent.context();
                            System.out.println(theEvent.kind()+" for "+theEvent.context()+" count = "+theEvent.count());

                            publishActionFor(thePath, theEvent.kind());
                        }
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
            for (Map.Entry<Path, ActionTimer> theEntry : fileTimers.entrySet()) {
                if (theEntry.getValue().runOneCycle()) {
                    theKeysToRemove.add(theEntry.getKey());
                    // Trigger the action
                    System.out.println("Triggering action "+theEntry.getValue().kind+" for "+theEntry.getKey());
                }
            }
            for (Path thePath : theKeysToRemove) {
                fileTimers.remove(thePath);
            }
        }
    }

    private void registerWatcher(Path aDirectory) throws IOException {
        aDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    public void startWatching() {
        watcherThread.start();
        actionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                actionCountDown();
            }
        }, 1000, 1000);
    }

    public void stopWatching() {
        actionTimer.cancel();
        watcherThread.interrupt();
    }
}
