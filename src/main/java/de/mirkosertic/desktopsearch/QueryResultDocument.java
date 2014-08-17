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
import java.util.concurrent.Future;

public class QueryResultDocument {

    private final String fileName;

    private final Future<String> highlightedSearchResult;

    private final long lastModified;

    private final List<String> similarFiles;

    public QueryResultDocument(String fileName, Future<String> aHighlighterResult, long lastModified, List<String> similarFiles) {
        this.fileName = fileName;
        highlightedSearchResult = aHighlighterResult;
        this.lastModified = lastModified;
        this.similarFiles = similarFiles;
    }

    public String getFileName() {
        return fileName;
    }

    public String getHighlightedSearchResult() {
        try {
            return highlightedSearchResult.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long getLastModified() {
        return lastModified;
    }

    public List<String> getSimilarFiles() {
        return Collections.unmodifiableList(similarFiles);
    }
}
