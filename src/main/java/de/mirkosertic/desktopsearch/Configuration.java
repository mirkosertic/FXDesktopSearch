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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private int numberOfSuggestions;
    private int suggestionSlop;
    private int suggestionWindowBefore;
    private int suggestionWindowAfter;
    private boolean suggestionInOrder;
    private boolean showSimilarDocuments;
    private boolean crawlOnStartup;
    private List<CrawlLocation> crawlLocations;
    private Set<SupportedLanguage> enabledLanguages;
    private Set<SupportedDocumentType> enabledDocumentTypes;
    private Map<String, String> metaDataNameReplacement;
    private File configDirectory;

    private Configuration() {
        // Needed by Jackson
        numberOfSearchResults = 50;
        numberOfSuggestions = 10;
        suggestionSlop = 6;
        suggestionWindowBefore = 0;
        suggestionWindowAfter = 3;
        suggestionInOrder = true;
        showSimilarDocuments = false;
        crawlOnStartup = true;
        crawlLocations = new ArrayList<>();
        enabledLanguages = new HashSet<>();
        enabledDocumentTypes = new HashSet<>();
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
        numberOfSuggestions = aConfiguration.numberOfSuggestions;
        numberOfSearchResults = aConfiguration.numberOfSearchResults;
        suggestionSlop = aConfiguration.suggestionSlop;
        suggestionWindowBefore = aConfiguration.suggestionWindowBefore;
        suggestionWindowAfter = aConfiguration.suggestionWindowAfter;
        suggestionInOrder = aConfiguration.suggestionInOrder;
        showSimilarDocuments = aConfiguration.showSimilarDocuments;
        crawlLocations = new ArrayList<>(aConfiguration.crawlLocations);
        enabledLanguages = new HashSet<>(aConfiguration.enabledLanguages);
        enabledDocumentTypes = new HashSet<>(aConfiguration.enabledDocumentTypes);
        metaDataNameReplacement = new HashMap<>(aConfiguration.metaDataNameReplacement);
        configDirectory = aConfiguration.configDirectory;
    }

    public Configuration(File aConfigDirectory) {
        this();
        configDirectory = aConfigDirectory;
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

    public File getConfigDirectory() {
        return configDirectory;
    }

    public int getNumberOfSuggestions() {
        return numberOfSuggestions;
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

    public int getSuggestionSlop() {
        return suggestionSlop;
    }

    public int getSuggestionWindowBefore() {
        return suggestionWindowBefore;
    }

    public int getSuggestionWindowAfter() {
        return suggestionWindowAfter;
    }

    public boolean isSuggestionInOrder() {
        return suggestionInOrder;
    }

    public boolean isCrawlOnStartup() {
        return crawlOnStartup;
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

    public Configuration updateNumberOfSuggestions(int aValue) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.numberOfSuggestions = aValue;
        return theConfiguration;
    }

    public Configuration enableDocumentType(SupportedDocumentType aType) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.enabledDocumentTypes.add(aType);
        return theConfiguration;
    }

    public Configuration disableDocumentType(SupportedDocumentType aType) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.enabledDocumentTypes.remove(aType);
        return theConfiguration;
    }

    public Configuration enableLanguage(SupportedLanguage aLanguage) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.enabledLanguages.add(aLanguage);
        return theConfiguration;
    }

    public Configuration disableLanguage(SupportedLanguage aLanguage) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.enabledLanguages.remove(aLanguage);
        return theConfiguration;
    }

    public Configuration updateSuggestionWindowBefore(int aValue) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.suggestionWindowBefore = aValue;
        return theConfiguration;
    }

    public Configuration updateSuggestionWindowAfter(int aValue) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.suggestionWindowAfter = aValue;
        return theConfiguration;
    }

    public Configuration updateSuggestionSlop(int aValue) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.suggestionSlop = aValue;
        return theConfiguration;
    }

    public Configuration updateSuggestionsInOrder(boolean aValue) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.suggestionInOrder = aValue;
        return theConfiguration;
    }

    public Configuration updateCrawlOnStartup(boolean aValue) {
        Configuration theConfiguration = new Configuration(this);
        theConfiguration.crawlOnStartup = aValue;
        return theConfiguration;
    }
}