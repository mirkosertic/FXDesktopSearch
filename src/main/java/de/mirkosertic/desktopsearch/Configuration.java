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

        public CrawlLocation(final String aID, final File aDirectory) {
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

    private Configuration(final Configuration aConfiguration) {
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
        crawlOnStartup = aConfiguration.crawlOnStartup;
    }

    public Configuration(final File aConfigDirectory) {
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

    public Configuration addLocation(final CrawlLocation aCrawlLocation) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.crawlLocations.add(aCrawlLocation);
        return theConfiguration;
    }

    public Configuration removeLocation(final CrawlLocation aCrawlLocation) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.crawlLocations.remove(aCrawlLocation);
        return theConfiguration;
    }

    public Configuration updateIncludeSimilarDocuments(final boolean aValue) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.showSimilarDocuments = aValue;
        return theConfiguration;
    }

    public Configuration updateNumberOfSearchResults(final int aValue) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.numberOfSearchResults = aValue;
        return theConfiguration;
    }

    public Configuration updateNumberOfSuggestions(final int aValue) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.numberOfSuggestions = aValue;
        return theConfiguration;
    }

    public Configuration enableDocumentType(final SupportedDocumentType aType) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.enabledDocumentTypes.add(aType);
        return theConfiguration;
    }

    public Configuration disableDocumentType(final SupportedDocumentType aType) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.enabledDocumentTypes.remove(aType);
        return theConfiguration;
    }

    public Configuration enableLanguage(final SupportedLanguage aLanguage) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.enabledLanguages.add(aLanguage);
        return theConfiguration;
    }

    public Configuration disableLanguage(final SupportedLanguage aLanguage) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.enabledLanguages.remove(aLanguage);
        return theConfiguration;
    }

    public Configuration updateSuggestionWindowBefore(final int aValue) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.suggestionWindowBefore = aValue;
        return theConfiguration;
    }

    public Configuration updateSuggestionWindowAfter(final int aValue) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.suggestionWindowAfter = aValue;
        return theConfiguration;
    }

    public Configuration updateSuggestionSlop(final int aValue) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.suggestionSlop = aValue;
        return theConfiguration;
    }

    public Configuration updateSuggestionsInOrder(final boolean aValue) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.suggestionInOrder = aValue;
        return theConfiguration;
    }

    public Configuration updateCrawlOnStartup(final boolean aValue) {
        final var theConfiguration = new Configuration(this);
        theConfiguration.crawlOnStartup = aValue;
        return theConfiguration;
    }
}