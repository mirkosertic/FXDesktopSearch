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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Configuration {

    public static class CrawlLocation {
        private String id;
        private File directory;

        private CrawlLocation() {
            // Needed by Jackson
        }

        public CrawlLocation(String aID, File aDirectory) {
            directory = aDirectory;
            id = aID;
        }

        public String getId() {
            return id;
        }

        public File getDirectory() {
            return directory;
        }

        @Override
        public String toString() {
            return directory.toString();
        }
    }

    private int numberOfSearchResults;
    private boolean showSimilarDocuments;
    private List<CrawlLocation> crawlLocations;
    private File indexDirectory;

    private Configuration() {
        // Needed by Jackson
    }

    private Configuration(Configuration aConfiguration) {
        numberOfSearchResults = aConfiguration.numberOfSearchResults;
        showSimilarDocuments = aConfiguration.showSimilarDocuments;
        crawlLocations = new ArrayList<>(aConfiguration.crawlLocations);
        indexDirectory = aConfiguration.indexDirectory;
    }

    public Configuration(File aConfigDirectory) {
        // Our defaults
        numberOfSearchResults = 50;
        showSimilarDocuments = false;
        crawlLocations = new ArrayList<>();
        indexDirectory = new File(aConfigDirectory, "index");
    }

    public int getNumberOfSearchResults() {
        return numberOfSearchResults;
    }

    public boolean isShowSimilarDocuments() {
        return showSimilarDocuments;
    }

    public List<CrawlLocation> getCrawlLocations() {
        return Collections.unmodifiableList(crawlLocations);
    }

    public File getIndexDirectory() {
        return indexDirectory;
    }

    public Configuration addLocation(CrawlLocation aCrawlLocation) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.crawlLocations.add(aCrawlLocation);
        return theConfiguration;
    }

    public Configuration removeLocation(CrawlLocation aCrawlLocation) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.crawlLocations.remove(aCrawlLocation);
        return theConfiguration;
    }

    public Configuration updateIncludeSimilarDocuments(boolean aValue) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.showSimilarDocuments = aValue;
        return theConfiguration;
    }

    public Configuration updateNumberOfSearchResults(int aValue) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.numberOfSearchResults = aValue;
        return theConfiguration;
    }
}