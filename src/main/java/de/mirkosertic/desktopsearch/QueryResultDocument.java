/*
 * FXDesktopSearch Copyright 2013 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    public QueryResultDocument(final int aDocumentID, final String aFileName, final String aHighlighterResult, final long aLastModified, final int aNormalizedScore, final String aUniqueID, final boolean aPreviewAvailable) {
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

    public void addFileName(final String aFileName) {
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

    public void addSimilarFile(final String aFileName) {
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
