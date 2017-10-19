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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryResultDocument {

    private final List<String> fileNames;

    private final String highlightedSearchResult;

    private final long lastModified;

    private final List<String> similarFiles;

    private final int documentID;

    private final int normalizedScore;

    private final String uniqueID;

    private final boolean previewAvailable;

    public QueryResultDocument(int aDocumentID, String aFileName, String aHighlighterResult, long aLastModified, int aNormalizedScore, String aUniqueID, boolean aPreviewAvailable) {
        previewAvailable = aPreviewAvailable;
        fileNames = new ArrayList<>();
        fileNames.add(aFileName);
        highlightedSearchResult = aHighlighterResult;
        lastModified = aLastModified;
        documentID = aDocumentID;
        similarFiles = new ArrayList<>();
        normalizedScore = aNormalizedScore;
        uniqueID = aUniqueID;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void addFileName(String aFileName) {
        if (!fileNames.contains(aFileName)) {
            fileNames.add(aFileName);
        }
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

    public int getDocumentID() {
        return documentID;
    }

    public void addSimilarFile(String aFileName) {
        if (!similarFiles.contains(aFileName)) {
            similarFiles.add(aFileName);
        }
    }

    public int getNormalizedScore() {
        return normalizedScore;
    }

    public String getUniqueID() {
        return uniqueID;
    }

    public boolean isPreviewAvailable() {
        return previewAvailable;
    }
}
