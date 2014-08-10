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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileCrawler {

    private final ContentExtractor contentExtractor;
    private final FilesToIndexQueue queue;

    public FileCrawler(FilesToIndexQueue aQueue, ContentExtractor aContentExtractor) {
        queue = aQueue;
        contentExtractor = aContentExtractor;
    }

    public void crawl(String aLocationId, File aFile) {
        try {
            Files.walkFileTree(aFile.toPath(), new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path aPath, BasicFileAttributes aFileAttributes)
                        throws IOException {
                    if (Files.isReadable(aPath)) {
                        return FileVisitResult.CONTINUE;
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult visitFile(Path aPath, BasicFileAttributes aFileAttributes) throws IOException {
                    if (aFileAttributes.size() <= 10 * 1024 * 1024 && contentExtractor.supportsFile(aPath.toString())) {
                        queue.offer(new FilesToIndexQueueEntry(aPath, aFileAttributes, aLocationId));
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
