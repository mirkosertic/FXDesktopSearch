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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.KeywordField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollectorManager;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.PDF;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.utils.DateUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class LuceneIndexHandler {

    private static final int NUMBER_OF_HIGHLIGHT_PASSAGES = 5;

    private final Map<String, String> facetFieldToTitle;
    private final Configuration configuration;
    private final PreviewProcessor previewProcessor;
    private final IndexWriter indexWriter;
    private DirectoryReader indexReader;
    private IndexSearcher indexSearcher;
    private final QueryParser queryParser;
    private final Analyzer analyzer;
    private final FacetsConfig facetsConfig;
    private final Map<String, SortedSetDocValuesReaderState> facetStatesCache;
    private Directory suggestDirectory;
    private final AtomicReference<AnalyzingInfixSuggester> suggester;

    private final FieldType contentFieldType;

    public LuceneIndexHandler(final Configuration configuration, final PreviewProcessor previewProcessor) throws IOException {
        this.suggester = new AtomicReference<>();
        this.previewProcessor = previewProcessor;
        this.configuration = configuration;
        this.facetFieldToTitle = new HashMap<>();
        this.facetFieldToTitle.put(IndexFields.LANGUAGE, "Language");
        this.facetFieldToTitle.put("attr_author", "Author");
        this.facetFieldToTitle.put("attr_last-modified-year", "Last modified");
        this.facetFieldToTitle.put("attr_" + IndexFields.EXTENSION, "File type");

        this.contentFieldType = new FieldType(TextField.TYPE_STORED);
        this.contentFieldType.setStoreTermVectors(true);
        this.contentFieldType.setStoreTermVectorPositions(true);
        this.contentFieldType.setStoreTermVectorOffsets(true);

        this.facetsConfig = new FacetsConfig();
        this.facetStatesCache = new HashMap<>();

        final var theIndexDirectory = new File(configuration.getConfigDirectory(), "index");
        if (!theIndexDirectory.mkdirs()) {
            log.warn("Could not create index directory {}", theIndexDirectory.getAbsolutePath());
        }

        // Try to open or create a new lucene index
        final Directory directory = FSDirectory.open(theIndexDirectory.toPath());
        this.analyzer = new StandardAnalyzer();
        final IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        this.indexWriter = new IndexWriter(directory, config);
        this.indexReader = DirectoryReader.open(indexWriter);
        IndexSearcher.setMaxClauseCount(8192);
        this.indexSearcher = new IndexSearcher(indexReader);

        this.queryParser = new QueryParser(analyzer);

        rebuildSuggester();
    }

    private void rebuildSuggester() {
        try {
            final var suggestDirectoryFile = new File(configuration.getConfigDirectory(), "suggester");
            if (!suggestDirectoryFile.mkdirs()) {
                log.warn("Could not suggest index directory {}", suggestDirectoryFile.getAbsolutePath());
            }

            new Thread(() -> {
                try {
                    if (suggestDirectory != null) {
                        suggestDirectory.close();
                    }
                    suggestDirectory = FSDirectory.open(suggestDirectoryFile.toPath());
                    final AnalyzingInfixSuggester sugg = new AnalyzingInfixSuggester(suggestDirectory, analyzer);
                    final LuceneDictionary dictionary = new LuceneDictionary(indexSearcher.getIndexReader(), IndexFields.CONTENT);
                    final long suggestStart = System.currentTimeMillis();
                    log.info("Loading suggester...");
                    sugg.build(dictionary);
                    final long suggestDuration = System.currentTimeMillis() - suggestStart;
                    log.info("Loading suggester took {} ms", suggestDuration);

                    suggester.set(sugg);
                } catch (final Exception e) {
                    log.error("Error while rebuilding suggester", e);
                }
            }).start();

        } catch (final Exception e) {
            log.error("Error while rebuilding suggester", e);
        }
    }

    public synchronized void commitDataJob() {
        log.info("Committing data job");

        try {
            indexWriter.flush();
            indexWriter.commit();

            final DirectoryReader newReader = DirectoryReader.openIfChanged(indexReader);
            if (newReader != null) {
                indexReader.close();
                indexReader = newReader;
                indexSearcher = new IndexSearcher(indexReader);
                facetStatesCache.clear();

                suggester.set(null);
                rebuildSuggester();
            }

            if (facetStatesCache.isEmpty()) {
                for (final String facetField : facetFields()) {
                    try {
                        final SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(indexSearcher.getIndexReader(), facetField, facetsConfig);
                        facetStatesCache.put(facetField, state);
                    } catch (final IllegalArgumentException e) {
                        log.debug("Could not get facets for field {}. Maybe field not used by documents?", facetField, e);
                    }
                }
            }
        } catch (final Exception e) {
            log.error("Error while committing data job", e);
        }

        log.info("Done");
    }

    private String[] facetFields() {
        final var result = new String[facetFieldToTitle.size()];
        var i=0;
        for (final var field : facetFieldToTitle.keySet()) {
            result[i++] = field;
        }
        return result;
    }

    public void crawlingStarts() {
    }

    public void addToIndex(final String locationId, final Content fileContent) throws IOException {

        final var theLanguage = fileContent.getLanguage();

        final var theDocument = new Document();
        theDocument.add(new StringField(IndexFields.UNIQUEID, fileContent.getFileName(), Field.Store.YES));
        theDocument.add(new StringField(IndexFields.LOCATIONID, locationId, Field.Store.YES));
        theDocument.add(new StringField(IndexFields.CONTENTMD5, DigestUtils.md5Hex(fileContent.getFileContent()), Field.Store.YES));
        theDocument.add(new StringField(IndexFields.FILESIZE, Long.toString(fileContent.getFileSize()), Field.Store.YES));
        theDocument.add(new StringField(IndexFields.LASTMODIFIED, Long.toString(fileContent.getLastModified()), Field.Store.YES));
        theDocument.add(new KeywordField(IndexFields.LANGUAGE, new BytesRef(theLanguage.name()), Field.Store.YES));

        fileContent.getMetadata().forEach(theEntry -> {
            if (!StringUtils.isEmpty(theEntry.key)) {
                final var theValue = theEntry.value;
                if (theValue instanceof String) {
                    final var theStringValue = ((String) theValue).trim();
                    if (!StringUtils.isEmpty(theStringValue)) {
                        theDocument.add(new KeywordField("attr_" + theEntry.key, new BytesRef(theStringValue), Field.Store.YES));
                    }
                }
                if (theValue instanceof List) {
                    final var theList = (List) theValue;
                    // TODO: How to handle this list?
                    //theDocument.setField("attr_" + theEntry.key, theList);
                }
                if (theValue instanceof Date) {
                    final var theDateValue = (Date) theValue;
                    final var theCalendar = GregorianCalendar.getInstance(DateUtils.UTC, Locale.US);
                    theCalendar.setTime(theDateValue);

                    // Full-Path
                    {
                        final var thePathInfo = String.format(
                                "%04d/%02d/%02d",
                                theCalendar.get(Calendar.YEAR),
                                theCalendar.get(Calendar.MONTH) + 1,
                                theCalendar.get(Calendar.DAY_OF_MONTH));

                        theDocument.add(new KeywordField("attr_" + theEntry.key+"-year-month-day", new BytesRef(thePathInfo), Field.Store.YES));
                    }
                    // Year
                    {
                        final var thePathInfo = String.format(
                                "%04d",
                                theCalendar.get(Calendar.YEAR));

                        theDocument.add(new KeywordField("attr_" + theEntry.key+"-year", new BytesRef(thePathInfo), Field.Store.YES));
                    }
                    // Year-month
                    {
                        final var thePathInfo = String.format(
                                "%04d/%02d",
                                theCalendar.get(Calendar.YEAR),
                                theCalendar.get(Calendar.MONTH) + 1);

                        theDocument.add(new KeywordField("attr_" + theEntry.key+"-year-month", new BytesRef(thePathInfo), Field.Store.YES));
                    }
                }
            }
        });

        theDocument.add(new Field(IndexFields.CONTENT, fileContent.getFileContent(), contentFieldType));

        try {
            final long start = System.currentTimeMillis();
            indexWriter.addDocument(theDocument);
            final long duration = System.currentTimeMillis() - start;
            log.debug("Added document {} to index in {} ms", fileContent.getFileName(), duration);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void removeFromIndex(final String fileName) throws IOException {
        try {
            // Create a Term for the field and value
            final Term term = new Term(IndexFields.UNIQUEID, fileName);
            // Delete all documents matching this term
            final long deletedCount = indexWriter.deleteDocuments(term);
            log.debug("Deleted {} documents for file {}", deletedCount, fileName);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void shutdown() {
        try {
            indexWriter.flush();
            indexWriter.commit();
            indexReader.close();
            indexWriter.close();

            final AnalyzingInfixSuggester sugg = suggester.get();
            if (sugg != null) {
                sugg.close();
            }
        } catch (final Exception e) {
            log.error("Error while closing IndexWriter", e);
        }
    }

    public UpdateCheckResult checkIfModified(final String fileName, final long lastModifiedTimestamp) throws IOException {

        final Term term = new Term(IndexFields.UNIQUEID, fileName);

        try {
            final TopDocs topDocs = indexSearcher.search(new TermQuery(term), 1000);
            for (final var scoreDoc : topDocs.scoreDocs) {
                final Document doc = indexSearcher.storedFields().document(scoreDoc.doc);

                final var theStoredLastModified = Long.parseLong(doc.get(IndexFields.LASTMODIFIED));
                if (theStoredLastModified != lastModifiedTimestamp) {
                    return UpdateCheckResult.UPDATED;
                }
                return UpdateCheckResult.UNMODIFIED;
            }
            return UpdateCheckResult.UPDATED;
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    private long indexSize() {
        return indexReader.numDocs();
    }

    private String getOrNull(final Document document, final String fieldName) {
        final String value = document.get(fieldName);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    public synchronized QueryResult performQuery(final String queryString, final Configuration configuration, final MultiValueMap<String, String> drilldownFields) {

        try {
            final long startTime = System.currentTimeMillis();
            final Query query = queryParser.parse(queryString, IndexFields.CONTENT, configuration.isDefaultFuzzySearch(), configuration.getFuzzySearchEditDistance());

            final List<QueryResultDocument> documents = new ArrayList<>();

            final FacetsCollectorManager facetsCollectorManager = new FacetsCollectorManager();
            final BooleanQuery.Builder drillDownQueryBuilder = new BooleanQuery.Builder();
            drillDownQueryBuilder.add(query, BooleanClause.Occur.MUST);

            final List<QueryFilter> activeFilters = new ArrayList<>();
            for (final Map.Entry<String, List<String>> entry : drilldownFields.entrySet()) {
                final String key = entry.getKey();
                if (key.startsWith("filter")) {
                    final String dim = key.substring("filter".length());
                    for (final String value : entry.getValue()) {
                        drillDownQueryBuilder.add(KeywordField.newExactQuery(dim, value), BooleanClause.Occur.MUST);

                        final UriComponentsBuilder linkBuilder = UriComponentsBuilder.fromPath("/search");
                        for (final Map.Entry<String, List<String>> e : drilldownFields.entrySet()) {
                            if (!e.getKey().equals(key)) {
                                linkBuilder.queryParam(e.getKey(), e.getValue());
                            }
                        }

                        activeFilters.add(new QueryFilter(facetFieldToTitle.get(dim), linkBuilder.encode().toUriString()));
                    }
                }
            }

            final long searchStart = System.currentTimeMillis();
            final Query drilldownQuery = drillDownQueryBuilder.build();
            final FacetsCollectorManager.FacetsResult facetResult = FacetsCollectorManager.search(indexSearcher, drilldownQuery, this.configuration.getNumberOfSearchResults(), facetsCollectorManager);
            final long searchDuration = System.currentTimeMillis() - searchStart;
            log.info("Search for '{}' took {} ms", drilldownQuery, searchDuration);
            final StoredFields storedFields = indexSearcher.storedFields();
            final TopDocs topDocs = facetResult.topDocs();

            final long docFetchStart = System.currentTimeMillis();
            for (final var scoreDoc : topDocs.scoreDocs) {
                storedFields.prefetch(scoreDoc.doc);
            }

            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                final var scoreDoc = topDocs.scoreDocs[i];

                final Document doc = storedFields.document(scoreDoc.doc,
                        Set.of(IndexFields.UNIQUEID,
                               IndexFields.LASTMODIFIED,
                               "attr_" + DublinCore.TITLE,
                               "attr_title",
                               "attr_" + PDF.DOC_INFO_TITLE.getName(),
                               "attr_" + TikaCoreProperties.TITLE.getName(),
                               "attr_" + DublinCore.SUBJECT.getName()));

                final var theFileName = doc.get(IndexFields.UNIQUEID);
                final var theStoredLastModified = Long.parseLong(doc.get(IndexFields.LASTMODIFIED));

                final var theNormalizedScore = (int) (
                        scoreDoc.score / topDocs.scoreDocs[0].score * 5);

                final var theFileOnDisk = new File(theFileName);
                if (theFileOnDisk.exists()) {

                    final var thePreviewAvailable = previewProcessor.previewAvailableFor(theFileOnDisk);

                    // Try to extract the title from the metadata
                    var theTitle = theFileName;
                    if (configuration.isUseTitleAsFilename()) {
                        theTitle = getOrNull(doc, "attr_" + DublinCore.TITLE);
                        if (theTitle == null) {
                            theTitle = getOrNull(doc, "attr_" + PDF.DOC_INFO_TITLE.getName());
                        }
                        if (theTitle == null) {
                            theTitle = getOrNull(doc, "attr_title");
                        }
                        if (theTitle == null) {
                            theTitle = getOrNull(doc,"attr_" + TikaCoreProperties.TITLE.getName());
                        }
                        if (theTitle == null) {
                            theTitle = getOrNull(doc, "attr_" + DublinCore.SUBJECT.getName());
                        }
                        if (theTitle == null) {
                            theTitle = theFileName;
                        }
                    }

                    final var theDocument = new QueryResultDocument(scoreDoc.doc, theTitle, theFileName, theStoredLastModified, theNormalizedScore, theFileName, thePreviewAvailable);

                    documents.add(theDocument);

                } else {
                    // Document can be deleted, as it is no longer on the hard drive
                    removeFromIndex(theFileName);
                }
            }
            final long docFetchDuration = System.currentTimeMillis() - docFetchStart;
            log.info("Fetching {} documents took {} ms", topDocs.scoreDocs.length, docFetchDuration);

            final long computeFacetsStart = System.currentTimeMillis();
            final List<FacetDimension> facetDimensions = new ArrayList<>();
            for (final Map.Entry<String, SortedSetDocValuesReaderState> entry : facetStatesCache.entrySet()) {
                final String filterParam = "filter" + entry.getKey();
                if (!drilldownFields.containsKey(filterParam)) {
                    final String facetField = entry.getKey();
                    final SortedSetDocValuesReaderState state = entry.getValue();
                    final Facets facets = new SortedSetDocValuesFacetCounts(state, facetResult.facetsCollector());
                    final List<Facet> facetValues = new ArrayList<>();
                    for (final FacetResult facet : facets.getAllDims(this.configuration.getFacetCount())) {

                        // Querystring is already part of the map
                        final MultiValueMap<String, String> linkParams = new LinkedMultiValueMap<>(drilldownFields);
                        linkParams.add(filterParam, facet.dim);

                        final UriComponentsBuilder linkBuilder = UriComponentsBuilder.fromPath("/search");
                        for (final Map.Entry<String, List<String>> e : linkParams.entrySet()) {
                            linkBuilder.queryParam(e.getKey(), e.getValue());
                        }
                        facetValues.add(new Facet(facet.dim, facet.value.longValue(), linkBuilder.encode().toUriString()));
                    }
                    if (!facetValues.isEmpty()) {
                        facetDimensions.add(new FacetDimension(facetField, facetFieldToTitle.get(facetField), facetValues));
                    }
                }
            }
            final long computeFacetsDuration = System.currentTimeMillis() - computeFacetsStart;
            log.info("Computing facets took {} ms", computeFacetsDuration);

            final long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("Complete query took {} ms", elapsedTime);

            return new QueryResult(queryString, elapsedTime, documents, facetDimensions, indexSize(), activeFilters);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Suggestion[] findSuggestionTermsFor(final String term) {
        try {
            final List<Suggestion> result = new ArrayList<>();

            // Check if suggest is already built
            final AnalyzingInfixSuggester sug = suggester.get();
            if (sug != null) {
                final List<Lookup.LookupResult> lookupResults = sug.lookup(term, configuration.getNumberOfSuggestions(), true, true);
                for (final Lookup.LookupResult lookupResult : lookupResults) {
                    final Suggestion suggestion = new Suggestion(lookupResult.highlightKey.toString(), lookupResult.key.toString());
                    result.add(suggestion);
                }
            }
            return result.toArray(new Suggestion[0]);
        } catch (final Exception e) {
            log.error("Error while looking up suggestions for term {}", term, e);
            return new Suggestion[0];
        }
    }

    public File getFileOnDiskForDocument(final String aUniqueID) {
        return new File(aUniqueID);
    }

    public String highlight(final String queryString, final int luceneDocumentId) {

        final UnifiedHighlighter highlighter = new UnifiedHighlighter.Builder(indexSearcher, analyzer)
                .withMaxLength(1_000_000)
                .withFormatter(new DefaultPassageFormatter())
                .withBreakIterator(() -> BreakIterator.getSentenceInstance(Locale.getDefault()))
                .build();

        final TotalHits totalHits = new TotalHits(1, TotalHits.Relation.EQUAL_TO);
        final ScoreDoc scoreDoc = new ScoreDoc(luceneDocumentId, 1.0f);
        final TopDocs topDocs = new TopDocs(totalHits, new ScoreDoc[] {scoreDoc});

        try {
            final Query query = queryParser.parse(queryString, IndexFields.CONTENT, configuration.isDefaultFuzzySearch(), configuration.getFuzzySearchEditDistance());

            // Perform highlighting
            final long highlightStart = System.currentTimeMillis();
            final String[] highlights = highlighter.highlight(IndexFields.CONTENT, query, topDocs, NUMBER_OF_HIGHLIGHT_PASSAGES);
            final long highlightDuration = System.currentTimeMillis() - highlightStart;
            log.info("Highlighting took {} ms", highlightDuration);

            return highlights[0];
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
