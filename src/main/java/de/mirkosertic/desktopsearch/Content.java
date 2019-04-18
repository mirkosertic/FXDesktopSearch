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
