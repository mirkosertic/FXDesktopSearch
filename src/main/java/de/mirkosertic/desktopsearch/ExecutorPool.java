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

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

class ExecutorPool {

    private final ForkJoinPool forkJoinPool;

    public ExecutorPool() {
        forkJoinPool = new ForkJoinPool();
    }

    public void execute(final Runnable aRunnable) {
        forkJoinPool.execute(aRunnable);
    }

    public <T> ForkJoinTask<T> submit(final Callable<T> aCallable) {
        return forkJoinPool.submit(aCallable);
    }
}
