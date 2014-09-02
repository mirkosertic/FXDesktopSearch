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
import java.util.*;

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
    private Set<SupportedLanguage> enabledLanguages;
    private Set<SupportedDocumentType> enabledDocumentTypes;
    private Map<String, String> metaDataNameReplacement;

    private Configuration() {
        // Needed by Jackson
        numberOfSearchResults = 50;
        showSimilarDocuments = false;
        crawlLocations = new ArrayList<>();
        enabledLanguages = new HashSet<>(Arrays.asList(SupportedLanguage.values()));
        enabledDocumentTypes = new HashSet<>(Arrays.asList(SupportedDocumentType.values()));
        metaDataNameReplacement = new HashMap<>();
        metaDataNameReplacement.put("created", "creation-date");
        metaDataNameReplacement.put("date", "creation-date");
        metaDataNameReplacement.put("modified", "last-modified");
        metaDataNameReplacement.put("last-save-date", "last-modified");
        metaDataNameReplacement.put("sourcemodified", "last-modified");
        metaDataNameReplacement.put("save-date", "last-modified");
        metaDataNameReplacement.put("creatortool", "application-name");
        metaDataNameReplacement.put("producer", "application-name");
        metaDataNameReplacement.put("creator", "author");
        metaDataNameReplacement.put("last-author", "author");
        metaDataNameReplacement.put("contentstatus", "content-status");
        metaDataNameReplacement.put("presentationformat", "presentation-format");
        metaDataNameReplacement.put("print-date", "last-printed");
        metaDataNameReplacement.put("keyword", "keywords");
        metaDataNameReplacement.put("revision", "revision-number");
        metaDataNameReplacement.put("appversion", "application-version");
        metaDataNameReplacement.put("character count", "character-count");
        metaDataNameReplacement.put("npages", "page-count");
        metaDataNameReplacement.put("slide-count", "page-count");
    }

    private Configuration(Configuration aConfiguration) {
        this();
        numberOfSearchResults = aConfiguration.numberOfSearchResults;
        showSimilarDocuments = aConfiguration.showSimilarDocuments;
        crawlLocations = new ArrayList<>(aConfiguration.crawlLocations);
        enabledLanguages = new HashSet<>(aConfiguration.enabledLanguages);
        enabledDocumentTypes = new HashSet<>(aConfiguration.enabledDocumentTypes);
        metaDataNameReplacement = new HashMap<>(aConfiguration.metaDataNameReplacement);
        indexDirectory = aConfiguration.indexDirectory;
    }

    public Configuration(File aConfigDirectory) {
        this();
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

    public Set<SupportedLanguage> getEnabledLanguages() {
        return Collections.unmodifiableSet(enabledLanguages);
    }

    public Set<SupportedDocumentType> getEnabledDocumentTypes() {
        return Collections.unmodifiableSet(enabledDocumentTypes);
    }

    public Map<String, String> getMetaDataNameReplacement() {
        return Collections.unmodifiableMap(metaDataNameReplacement);
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