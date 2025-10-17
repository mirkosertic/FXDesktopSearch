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
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.DrillDownQuery;
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
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.PDF;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.utils.DateUtils;

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

    public LuceneIndexHandler(final Configuration aConfiguration, final PreviewProcessor aPreviewProcessor) throws IOException {
        previewProcessor = aPreviewProcessor;
        configuration = aConfiguration;
        facetFieldToTitle = new HashMap<>();
        facetFieldToTitle.put(IndexFields.LANGUAGE, "Language");
        facetFieldToTitle.put("attr_author", "Author");
        facetFieldToTitle.put("attr_last-modified-year", "Last modified");
        facetFieldToTitle.put("attr_" + IndexFields.EXTENSION, "File type");
        facetFieldToTitle.put("attr_entity_LOCATION", "Location");
        facetFieldToTitle.put("attr_entity_PERSON", "Person");
        facetFieldToTitle.put("attr_entity_ORGANIZATION", "Organization");
        facetFieldToTitle.put("attr_keywords", "Keywords");

        facetsConfig = new FacetsConfig();

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
        theDocument.add(new SortedSetDocValuesField(IndexFields.LANGUAGE, new BytesRef(theLanguage.name())));

        aContent.getMetadata().forEach(theEntry -> {
            if (!StringUtils.isEmpty(theEntry.key)) {
                final var theValue = theEntry.value;
                if (theValue instanceof String) {
                    final var theStringValue = (String) theValue;
                    if (!StringUtils.isEmpty(theStringValue)) {
                        theDocument.add(new SortedSetDocValuesField("attr_" + theEntry.key, new BytesRef(theStringValue)));
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

                        theDocument.add(new SortedSetDocValuesField("attr_" + theEntry.key+"-year-month-day", new BytesRef(thePathInfo)));
                    }
                    // Year
                    {
                        final var thePathInfo = String.format(
                                "%04d",
                                theCalendar.get(Calendar.YEAR));

                        theDocument.add(new SortedSetDocValuesField("attr_" + theEntry.key+"-year", new BytesRef(thePathInfo)));
                    }
                    // Year-month
                    {
                        final var thePathInfo = String.format(
                                "%04d/%02d",
                                theCalendar.get(Calendar.YEAR),
                                theCalendar.get(Calendar.MONTH) + 1);

                        theDocument.add(new SortedSetDocValuesField("attr_" + theEntry.key+"-year-month", new BytesRef(thePathInfo)));
                    }

                }
            }
        });

        theDocument.add(new TextField(IndexFields.CONTENT, aContent.getFileContent(), Field.Store.YES));

        try {
            indexWriter.addDocument(theDocument);
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

    private String encode(final String aValue) {
        final var theURLCodec = new URLCodec();
        try {
            return theURLCodec.encode(aValue);
        } catch (final EncoderException e) {
            return null;
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

    public QueryResult performQuery(final String aQueryString, final String aBasePath, final Configuration aConfiguration, final Map<String, Set<String>> aDrilldownFields) {

        try {
            indexWriter.flush();
            indexWriter.commit();

            final DirectoryReader newReader = DirectoryReader.openIfChanged(indexReader);
            if (newReader != null) {
                indexReader.close();
                indexReader = newReader;
                indexSearcher = new IndexSearcher(indexReader);
            }

            final long startTime = System.currentTimeMillis();
            final Query query = queryParser.parse(aQueryString, IndexFields.CONTENT);

            final List<QueryResultDocument> documents = new ArrayList<>();

            final Formatter formatter = new SimpleHTMLFormatter();
            final QueryScorer scorer = new QueryScorer(query);
            final Highlighter highlighter = new Highlighter(formatter, scorer);
            final Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 50);
            highlighter.setTextFragmenter(fragmenter);

            final FacetsCollectorManager facetsCollectorManager = new FacetsCollectorManager();
            final DrillDownQuery drillDownQuery = new DrillDownQuery(facetsConfig, query);

            final FacetsCollectorManager.FacetsResult facetResult = FacetsCollectorManager.search(indexSearcher, drillDownQuery, configuration.getNumberOfSearchResults(), facetsCollectorManager);
            final StoredFields storedFields = indexSearcher.storedFields();
            final TopDocs topDocs = facetResult.topDocs();
            for (final var scoreDoc : topDocs.scoreDocs) {
                final Document doc = storedFields.document(scoreDoc.doc);

                final var theFileName = doc.get(IndexFields.UNIQUEID);
                final var theStoredLastModified = Long.parseLong(doc.get(IndexFields.LASTMODIFIED));

                final var theNormalizedScore = (int) (
                        scoreDoc.score / topDocs.scoreDocs[0].score * 5);

                final var theHighlight = new StringBuilder();

                final TokenStream tokenStream = TokenSources.getAnyTokenStream(indexSearcher.getIndexReader(), scoreDoc.doc, IndexFields.CONTENT, analyzer);
                final String[] frags = highlighter.getBestFragments(tokenStream, doc.get(IndexFields.CONTENT), 10);
                for (final String frag : frags) {
                    if (!theHighlight.isEmpty()) {
                        theHighlight.append(" ... ");
                    }
                    theHighlight.append(frag);
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

                    /*if (configuration.isShowSimilarDocuments()) {
                        final var theMoreLikeThisDocuments = theQueryResponse.getMoreLikeThis().get(theFileName);
                        if (theMoreLikeThisDocuments != null) {
                            for (final var theMLt : theMoreLikeThisDocuments) {
                                theDocument.addSimilarFile(((String) theMLt.getFieldValue(IndexFields.UNIQUEID)));
                            }
                        }
                    }*/

                    documents.add(theDocument);

                } else {
                    // Document can be deleted, as it is no longer on the hard drive
                    removeFromIndex(theFileName);
                }
            }

            for (final String facetField : facetFields()) {
                try {
                    final SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(indexSearcher.getIndexReader(), facetField, facetsConfig);
                    final Facets facets = new SortedSetDocValuesFacetCounts(state, facetResult.facetsCollector());
                    for (final FacetResult facet : facets.getAllDims(configuration.getFacetCount())) {
                        // TODO: Generate facet dimensions....
                    }
                } catch (final Exception e) {
                    log.warn("Coult not get facets for field {}", facetField, e);
                }
            }

            final long elapsedTime = System.currentTimeMillis() - startTime;

            return new QueryResult(aQueryString, elapsedTime, documents, new ArrayList<>(), indexSize(), new ArrayList<>());

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
/*        final Map<String, Object> theParams = new HashMap<>();
        theParams.put("defType", "google");
        theParams.put("q", aQueryString);
        theParams.put("fl", "*,score");
        theParams.put("rows", Integer.toString(configuration.getNumberOfSearchResults()));
        theParams.put("facet", "true");
        theParams.put("facet.field", facetFields());
        theParams.put("facet.limit", Integer.toString(configuration.getFacetCount()));
        theParams.put("hl", "true");
        theParams.put("hl.method", "unified");
        theParams.put("hl.fl", IndexFields.CONTENT);
        theParams.put("hl.snippets", Integer.toString(NUMBER_OF_FRAGMENTS));
        theParams.put("hl.fragsize", "100");

        final List<QueryFilter> activeFilters = new ArrayList<>();

        if (aDrilldownFields != null) {
            final List<String> theFilters = new ArrayList<>();
            for (final var theField : aDrilldownFields.entrySet()) {
                final var dim = theField.getKey();
                for (final var f : theField.getValue()) {
                    theFilters.add(dim + ":" + ClientUtils.escapeQueryChars(f));
                    activeFilters.add(new QueryFilter(facetFieldToTitle.get(dim) + " : " + f, filterFacet(aBasePath, dim, encode(f))));
                }
            }
            if (!theFilters.isEmpty()) {
                theParams.put("fq", theFilters.toArray(new String[0]));
            }
        }

        if (aConfiguration.isShowSimilarDocuments()) {
            theParams.put("mlt", "true");
            theParams.put("mlt.count", "5");
            theParams.put("mlt.fl", IndexFields.CONTENT);
        }

        try {
            final var theStartTime = System.currentTimeMillis();
            final var theQueryResponse = solrClient.query(new SearchMapParams(theParams));

            final List<QueryResultDocument> theDocuments = new ArrayList<>();
            if (theQueryResponse.getResults() != null) {
                for (var i = 0; i < theQueryResponse.getResults().size(); i++) {
                    final var theSolrDocument = theQueryResponse.getResults().get(i);

                    final var theFileName = (String) theSolrDocument.getFieldValue(IndexFields.UNIQUEID);
                    final var theStoredLastModified = Long.parseLong((String) theSolrDocument.getFieldValue(IndexFields.LASTMODIFIED));

                    final var theNormalizedScore = (int) (
                            ((float) theSolrDocument.getFieldValue("score")) / theQueryResponse.getResults().getMaxScore() * 5);

                    final var theHighlight = new StringBuilder();
                    final var theHighlightPhrases = theQueryResponse.getHighlighting().get(theFileName);
                    if (theHighlightPhrases != null) {
                        final var theContentSpans = theHighlightPhrases.get(IndexFields.CONTENT);
                        if (theContentSpans != null) {
                            for (final var thePhrase : theContentSpans) {
                                if (theHighlight.length() > 0) {
                                    theHighlight.append(" ... ");
                                }
                                theHighlight.append(thePhrase.trim());
                            }
                        } else {
                            log.warn("No highligting for {}", theFileName);
                        }
                    }

                    final var theFileOnDisk = new File(theFileName);
                    if (theFileOnDisk.exists()) {

                        final var thePreviewAvailable = previewProcessor.previewAvailableFor(theFileOnDisk);

                        // Try to extract the title from the metadata
                        var theTitle = theFileName;
                        if (aConfiguration.isUseTitleAsFilename()) {
                            theTitle = getOrDefault(theSolrDocument, "attr_" + DublinCore.TITLE, "");
                            if (theTitle == null || theTitle.trim().length() == 0) {
                                theTitle = getOrDefault(theSolrDocument, "attr_" + PDF.DOC_INFO_TITLE.getName(), "");
                            }
                            if (theTitle == null || theTitle.trim().length() == 0) {
                                theTitle = getOrDefault(theSolrDocument, "attr_title", "");
                            }
                            if (theTitle == null || theTitle.trim().length() == 0) {
                                theTitle = getOrDefault(theSolrDocument,"attr_" + TikaCoreProperties.TITLE.getName(), "");
                            }
                            if (theTitle == null || theTitle.trim().length() == 0) {
                                theTitle = getOrDefault(theSolrDocument,"attr_" + DublinCore.TITLE.getName(), "");
                            }
                            if (theTitle == null || theTitle.trim().length() == 0) {
                                theTitle = getOrDefault(theSolrDocument, "attr_" + DublinCore.SUBJECT.getName(), "");
                            }
                            if (theTitle == null || theTitle.trim().length() == 0) {
                                theTitle = theFileName;
                            }
                        }

                        final var theDocument = new QueryResultDocument(i, theTitle, theFileName, theHighlight.toString().trim(),
                                theStoredLastModified, theNormalizedScore, theFileName, thePreviewAvailable);

                        if (configuration.isShowSimilarDocuments()) {
                            final var theMoreLikeThisDocuments = theQueryResponse.getMoreLikeThis().get(theFileName);
                            if (theMoreLikeThisDocuments != null) {
                                for (final var theMLt : theMoreLikeThisDocuments) {
                                    theDocument.addSimilarFile(((String) theMLt.getFieldValue(IndexFields.UNIQUEID)));
                                }
                            }
                        }

                        theDocuments.add(theDocument);

                    } else {

                        // Document can be deleted, as it is no longer on the hard drive
                        solrClient.deleteById(theFileName);
                    }
                }
            }

            final var theIndexSize = indexSize();

            final var theDuration = System.currentTimeMillis() - theStartTime;

            final List<FacetDimension> theDimensions = new ArrayList<>();
//            fillFacet(IndexFields.LANGUAGE, aBasePath, theQueryResponse, theDimensions, aDrilldownFields, t -> SupportedLanguage.valueOf(t).toLocale().getDisplayName());
            fillFacet("attr_author", aBasePath, theQueryResponse, theDimensions, aDrilldownFields, t -> t);
            fillFacet("attr_last-modified-year", aBasePath, theQueryResponse, theDimensions, aDrilldownFields, t -> t);
            fillFacet("attr_" + IndexFields.EXTENSION, aBasePath, theQueryResponse, theDimensions, aDrilldownFields, t -> t);
            fillFacet("attr_entity_LOCATION", aBasePath, theQueryResponse, theDimensions, aDrilldownFields, t -> t);
            fillFacet("attr_entity_PERSON", aBasePath, theQueryResponse, theDimensions, aDrilldownFields, t -> t);
            fillFacet("attr_entity_ORGANIZATION", aBasePath, theQueryResponse, theDimensions, aDrilldownFields, t -> t);
            fillFacet("attr_keywords", aBasePath, theQueryResponse, theDimensions, aDrilldownFields, t -> t);

            return new QueryResult(StringEscapeUtils.escapeHtml4(aQueryString), theDuration, theDocuments, theDimensions, theIndexSize, Lists.reverse(activeFilters));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }*/
    }

    public Suggestion[] findSuggestionTermsFor(final String aTerm) {
        // TODO: Implement this...
        return new Suggestion[0];
    }

    public File getFileOnDiskForDocument(final String aUniqueID) {
        return new File(aUniqueID);
    }
}
