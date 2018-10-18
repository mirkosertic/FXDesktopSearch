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

import java.io.File;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

class PreviewProcessor {

    private final Set<PreviewGenerator> generators;

    public PreviewProcessor() {

        generators = new HashSet<>();

        final ServiceLoader<PreviewGenerator> theGeneratorLoader = ServiceLoader.load(PreviewGenerator.class, getClass().getClassLoader());
        for (final PreviewGenerator aTheGeneratorLoader : theGeneratorLoader) {
            generators.add(aTheGeneratorLoader);
        }
    }

    public Preview computePreviewFor(final File aFile) {
        for (final PreviewGenerator theGenerator : generators) {
            if (theGenerator.supportsFile(aFile)) {
                return theGenerator.createPreviewFor(aFile);
            }
        }
        return null;
    }

    public boolean previewAvailableFor(final File aFile) {
        for (final PreviewGenerator theGenerator : generators) {
            if (theGenerator.supportsFile(aFile)) {
                return true;
            }
        }
        return false;
    }
}