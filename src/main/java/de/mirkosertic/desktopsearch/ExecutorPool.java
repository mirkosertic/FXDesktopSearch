package de.mirkosertic.desktopsearch;

import java.util.concurrent.ForkJoinPool;

public class ExecutorPool {

    private final ForkJoinPool forkJoinPool;

    public ExecutorPool() {
        forkJoinPool = new ForkJoinPool();
    }

    public void execute(Runnable aRunnable) {
        forkJoinPool.execute(aRunnable);
    }
}
