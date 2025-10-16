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

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

@Component
public class PreviewProcessor {

    private final Set<PreviewGenerator> generators;

    public PreviewProcessor() {

        generators = new HashSet<>();

        final var theGeneratorLoader = ServiceLoader.load(PreviewGenerator.class, getClass().getClassLoader());
        for (final var aTheGeneratorLoader : theGeneratorLoader) {
            generators.add(aTheGeneratorLoader);
        }
    }

    public Preview computePreviewFor(final File aFile) {
        for (final var theGenerator : generators) {
            if (theGenerator.supportsFile(aFile)) {
                return theGenerator.createPreviewFor(aFile);
            }
        }
        return null;
    }

    public boolean previewAvailableFor(final File aFile) {
        for (final var theGenerator : generators) {
            if (theGenerator.supportsFile(aFile)) {
                return true;
            }
        }
        return false;
    }
}