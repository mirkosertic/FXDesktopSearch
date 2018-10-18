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
import java.util.List;
import java.util.stream.Stream;

public class Content {

    public final class KeyValuePair {
        public final String key;
        public final Object value;

        private KeyValuePair(final String key, final Object value) {
            this.key = key;
            this.value = value;
        }
    }

    private final String fileName;
    private final long fileSize;
    private final long lastModified;
    private final List<KeyValuePair> metadata;
    private final String fileContent;
    private final SupportedLanguage language;

    public Content(
            final String aFileName, final String aFileContent, final long aFileSize, final long aLastModified, final SupportedLanguage aLanguage) {
        fileName = aFileName;
        fileSize = aFileSize;
        lastModified = aLastModified;
        metadata = new ArrayList<>();
        fileContent = aFileContent;
        language = aLanguage;
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

    public SupportedLanguage getLanguage() {
        return language;
    }

    public Stream<KeyValuePair> getMetadata() {
        return metadata.stream();
    }

    public void addMetaData(final String aKey, final Object aValue) {
        metadata.add(new KeyValuePair(aKey, aValue));
    }
}
