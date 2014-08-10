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

public class FilesToIndexQueueEntry {

    private final Path path;
    private final String locationId;
    private final BasicFileAttributes fileAttributes;

    public FilesToIndexQueueEntry(Path aPath, BasicFileAttributes aAttributes, String aLocationID) {
        path = aPath;
        locationId = aLocationID;
        fileAttributes = aAttributes;
    }

    public BasicFileAttributes getFileAttributes() {
        return fileAttributes;
    }

    public String getLocationId() {
        return locationId;
    }

    public Path getPath() {
        return path;
    }
}
