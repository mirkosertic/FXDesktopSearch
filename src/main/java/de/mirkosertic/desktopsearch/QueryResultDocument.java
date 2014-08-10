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
import java.util.List;

public class QueryResultDocument {

    private String fileName;

    private String highlightedSearchResult;

    private long lastModified;

    private List<String> similarFiles;

    public QueryResultDocument(String fileName, String highlightedSearchResult, long lastModified, List<String> similarFiles) {
        this.fileName = fileName;
        this.highlightedSearchResult = highlightedSearchResult;
        this.lastModified = lastModified;
        this.similarFiles = similarFiles;
    }

    public String getFileName() {
        return fileName;
    }

    public String getHighlightedSearchResult() {
        return highlightedSearchResult;
    }

    public long getLastModified() {
        return lastModified;
    }

    public List<String> getSimilarFiles() {
        return Collections.unmodifiableList(similarFiles);
    }
}
