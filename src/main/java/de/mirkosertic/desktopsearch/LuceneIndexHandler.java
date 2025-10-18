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
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
public class LuceneIndexHandler {

    private static final int NUMBER_OF_FRAGMENTS = 5;

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

    public LuceneIndexHandler(final Configuration aConfiguration, final PreviewProcessor aPreviewProcessor) throws IOException {
        previewProcessor = aPreviewProcessor;
        configuration = aConfiguration;
        facetFieldToTitle = new HashMap<>();
        facetFieldToTitle.put(IndexFields.LANGUAGE, "Language");
        facetFieldToTitle.put("attr_author", "Author");
        facetFieldToTitle.put("attr_last-modified-year", "Last modified");
        facetFieldToTitle.put("attr_" + IndexFields.EXTENSION, "File type");

        facetsConfig = new FacetsConfig();
        facetStatesCache = new HashMap<>();

        final var theIndexDirectory = new File(aConfiguration.getConfigDirectory(), "index");
        theIndexDirectory.mkdirs();

        // Try to open or create a new lucene index
        final Directory directory = FSDirectory.open(theIndexDirectory.toPath());
        analyzer = new StandardAnalyzer();
        final IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        indexWriter = new IndexWriter(directory, config);
        indexReader = DirectoryReader.open(indexWriter);
        indexSearcher = new IndexSearcher(indexReader);

        queryParser = new QueryParser(analyzer);
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

    public void addToIndex(final String aLocationId, final Content aContent) throws IOException {

        final var theLanguage = aContent.getLanguage();

        final var theDocument = new Document();
        theDocument.add(new StringField(IndexFields.UNIQUEID, aContent.getFileName(), Field.Store.YES));
        theDocument.add(new StringField(IndexFields.LOCATIONID, aLocationId, Field.Store.YES));
        theDocument.add(new StringField(IndexFields.CONTENTMD5, DigestUtils.md5Hex(aContent.getFileContent()), Field.Store.YES));
        theDocument.add(new StringField(IndexFields.FILESIZE, Long.toString(aContent.getFileSize()), Field.Store.YES));
        theDocument.add(new StringField(IndexFields.LASTMODIFIED, Long.toString(aContent.getLastModified()), Field.Store.YES));
        theDocument.add(new KeywordField(IndexFields.LANGUAGE, new BytesRef(theLanguage.name()), Field.Store.YES));

        aContent.getMetadata().forEach(theEntry -> {
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

        theDocument.add(new TextField(IndexFields.CONTENT, aContent.getFileContent(), Field.Store.YES));

        try {
            final long start = System.currentTimeMillis();
            indexWriter.addDocument(facetsConfig.build(theDocument));
            final long duration = System.currentTimeMillis() - start;
            log.debug("Added document {} to index in {} ms", aContent.getFileName(), duration);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void removeFromIndex(final String aFileName) throws IOException {
        try {
            // Create a Term for the field and value
            final Term term = new Term(IndexFields.UNIQUEID, aFileName);
            // Delete all documents matching this term
            final long deletedCount = indexWriter.deleteDocuments(term);
            log.debug("Deleted {} documents for file {}", deletedCount, aFileName);
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
        } catch (final Exception e) {
            log.error("Error while closing IndexWriter", e);
        }
    }

    public UpdateCheckResult checkIfModified(final String aFilename, final long aLastModified) throws IOException {

        final Term term = new Term(IndexFields.UNIQUEID, aFilename);

        try {
            final TopDocs topDocs = indexSearcher.search(new TermQuery(term), 1000);
            for (final var scoreDoc : topDocs.scoreDocs) {
                final Document doc = indexSearcher.storedFields().document(scoreDoc.doc);

                final var theStoredLastModified = Long.parseLong(doc.get(IndexFields.LASTMODIFIED));
                if (theStoredLastModified != aLastModified) {
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

    private String getOrDefault(final Document aDocument, final String aFieldName, final String aDefault) {
        final String value = aDocument.get(aFieldName);
        if (value == null || value.trim().isEmpty()) {
            return aDefault;
        }
        return value;
    }

    public QueryResult performQuery(final String aQueryString, final Configuration aConfiguration, final MultiValueMap<String, String> aDrilldownFields) {

        try {
            indexWriter.flush();
            indexWriter.commit();

            final DirectoryReader newReader = DirectoryReader.openIfChanged(indexReader);
            if (newReader != null) {
                indexReader.close();
                indexReader = newReader;
                indexSearcher = new IndexSearcher(indexReader);
                facetStatesCache.clear();
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

            final long startTime = System.currentTimeMillis();
            final Query query = queryParser.parse(aQueryString, IndexFields.CONTENT);

            final List<QueryResultDocument> documents = new ArrayList<>();

            final Formatter formatter = new SimpleHTMLFormatter("", "");
            final QueryScorer scorer = new QueryScorer(query);
            final Highlighter highlighter = new Highlighter(formatter, scorer);
            final Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 300);
            highlighter.setTextFragmenter(fragmenter);
            highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

            final FacetsCollectorManager facetsCollectorManager = new FacetsCollectorManager();
            final BooleanQuery.Builder drillDownQueryBuilder = new BooleanQuery.Builder();
            drillDownQueryBuilder.add(query, BooleanClause.Occur.MUST);

            final List<QueryFilter> activeFilters = new ArrayList<>();
            for (final Map.Entry<String, List<String>> entry : aDrilldownFields.entrySet()) {
                final String key = entry.getKey();
                if (key.startsWith("filter")) {
                    final String dim = key.substring("filter".length());
                    for (final String value : entry.getValue()) {
                        drillDownQueryBuilder.add(KeywordField.newExactQuery(dim, value), BooleanClause.Occur.MUST);

                        final UriComponentsBuilder linkBuilder = UriComponentsBuilder.fromPath("/search");
                        for (final Map.Entry<String, List<String>> e : aDrilldownFields.entrySet()) {
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
            final FacetsCollectorManager.FacetsResult facetResult = FacetsCollectorManager.search(indexSearcher, drilldownQuery, configuration.getNumberOfSearchResults(), facetsCollectorManager);
            final long searchDuration = System.currentTimeMillis() - searchStart;
            log.info("Search for '{}' took {} ms", drilldownQuery, searchDuration);
            final StoredFields storedFields = indexSearcher.storedFields();
            final TopDocs topDocs = facetResult.topDocs();
            final long docFetchStart = System.currentTimeMillis();
            for (final var scoreDoc : topDocs.scoreDocs) {
                final long getStart = System.currentTimeMillis();
                final Document doc = storedFields.document(scoreDoc.doc);
                final long getDuration = System.currentTimeMillis() - getStart;
                log.info("Fetching single document took {} ms", getDuration);

                final var theFileName = doc.get(IndexFields.UNIQUEID);
                final var theStoredLastModified = Long.parseLong(doc.get(IndexFields.LASTMODIFIED));

                final var theNormalizedScore = (int) (
                        scoreDoc.score / topDocs.scoreDocs[0].score * 5);

                final var theHighlight = new StringBuilder();

                final String[] frags = highlighter.getBestFragments(analyzer, IndexFields.CONTENT, doc.get(IndexFields.CONTENT), 10);
                for (final String frag : frags) {
                    if (!theHighlight.isEmpty()) {
                        theHighlight.append(" ... ");
                    }
                    theHighlight.append(frag.trim());
                }

                final var theFileOnDisk = new File(theFileName);
                if (theFileOnDisk.exists()) {

                    final var thePreviewAvailable = previewProcessor.previewAvailableFor(theFileOnDisk);

                    // Try to extract the title from the metadata
                    var theTitle = theFileName;
                    if (aConfiguration.isUseTitleAsFilename()) {
                        theTitle = getOrDefault(doc, "attr_" + DublinCore.TITLE, "");
                        if (theTitle == null || theTitle.trim().isEmpty()) {
                            theTitle = getOrDefault(doc, "attr_" + PDF.DOC_INFO_TITLE.getName(), "");
                        }
                        if (theTitle == null || theTitle.trim().isEmpty()) {
                            theTitle = getOrDefault(doc, "attr_title", "");
                        }
                        if (theTitle == null || theTitle.trim().isEmpty()) {
                            theTitle = getOrDefault(doc,"attr_" + TikaCoreProperties.TITLE.getName(), "");
                        }
                        if (theTitle == null || theTitle.trim().isEmpty()) {
                            theTitle = getOrDefault(doc,"attr_" + DublinCore.TITLE.getName(), "");
                        }
                        if (theTitle == null || theTitle.trim().isEmpty()) {
                            theTitle = getOrDefault(doc, "attr_" + DublinCore.SUBJECT.getName(), "");
                        }
                        if (theTitle == null || theTitle.trim().isEmpty()) {
                            theTitle = theFileName;
                        }
                    }

                    final var theDocument = new QueryResultDocument(scoreDoc.doc, theTitle, theFileName, theHighlight.toString().trim(),
                            theStoredLastModified, theNormalizedScore, theFileName, thePreviewAvailable);

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
                if (!aDrilldownFields.containsKey(filterParam)) {
                    final String facetField = entry.getKey();
                    final SortedSetDocValuesReaderState state = entry.getValue();
                    final Facets facets = new SortedSetDocValuesFacetCounts(state, facetResult.facetsCollector());
                    final List<Facet> facetValues = new ArrayList<>();
                    for (final FacetResult facet : facets.getAllDims(configuration.getFacetCount())) {

                        // Querystring is already part of the map
                        final MultiValueMap<String, String> linkParams = new LinkedMultiValueMap<>(aDrilldownFields);
                        linkParams.add(filterParam, facet.dim);

                        final UriComponentsBuilder linkBuilder = UriComponentsBuilder.fromPath("/search");
                        for (final Map.Entry<String, List<String>> e : linkParams.entrySet()) {
                            linkBuilder.queryParam(e.getKey(), e.getValue());
                        }
                        // TODO: Compute selection link
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

            return new QueryResult(aQueryString, elapsedTime, documents, facetDimensions, indexSize(), activeFilters);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Suggestion[] findSuggestionTermsFor(final String aTerm) {
        // TODO: Implement this...
        return new Suggestion[0];
    }

    public File getFileOnDiskForDocument(final String aUniqueID) {
        return new File(aUniqueID);
    }
}
