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

public class QueryResultDocument {

    private final String title;

    private final String fileName;

    private final long lastModified;

    private final int documentID;

    private final int normalizedScore;

    private final String uniqueID;

    private final boolean previewAvailable;

    public QueryResultDocument(final int aDocumentID, final String aTitle, final String aFileName, final long aLastModified, final int aNormalizedScore, final String aUniqueID, final boolean aPreviewAvailable) {
        previewAvailable = aPreviewAvailable;
        title = aTitle;
        fileName = aFileName;
        lastModified = aLastModified;
        documentID = aDocumentID;
        normalizedScore = aNormalizedScore;
        uniqueID = aUniqueID;
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

    public long getLastModified() {
        return lastModified;
    }

    public int getDocumentID() {
        return documentID;
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
