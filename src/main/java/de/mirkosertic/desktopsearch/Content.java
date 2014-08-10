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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

public class Content {

    private String fileName;
    private long fileSize;
    private long lastModified;
    private Map<String, String> metadata;
    private String fileContent;

    public Content(String fileName, String aFileContent, long fileSize, long lastModified) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        metadata = new HashMap<>();
        fileContent = aFileContent;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileContent() {
        return fileContent;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getLastModified() {
        return lastModified;
    }

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public void addMetaData(String aKey, String aValue) {
        metadata.put(aKey, aValue);
    }
}
