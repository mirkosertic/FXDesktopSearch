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

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LegacyLongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.tika.utils.DateUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ForkJoinTask;

class LuceneIndexHandler {

    private static final Logger LOGGER = Logger.getLogger(LuceneIndexHandler.class);

    private static final int NUMBER_OF_FRAGMENTS = 5;

    private final IndexWriter indexWriter;
    private final SearcherManager searcherManager;
    private final AnalyzerCache analyzerCache;
    private final Analyzer analyzer;
    private final FacetsConfig facetsConfig;
    private final Thread commitThread;
    private final FieldType contentFieldType;
    private final ExecutorPool executorPool;
    private final Configuration configuration;
    private final PreviewProcessor previewProcessor;

    public LuceneIndexHandler(Configuration aConfiguration, AnalyzerCache aAnalyzerCache, ExecutorPool aExecutorPool, PreviewProcessor aPreviewProcessor) throws IOException {
        previewProcessor = aPreviewProcessor;
        configuration = aConfiguration;
        analyzerCache = aAnalyzerCache;
        executorPool = aExecutorPool;

        contentFieldType = new FieldType();
        contentFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        contentFieldType.setStored(true);
        contentFieldType.setTokenized(true);
        contentFieldType.setStoreTermVectorOffsets(true);
        contentFieldType.setStoreTermVectorPayloads(true);
        contentFieldType.setStoreTermVectorPositions(true);
        contentFieldType.setStoreTermVectors(true);

        analyzer = analyzerCache.getAnalyzer();

        File theIndexDirectory = new File(aConfiguration.getConfigDirectory(), "index");
        theIndexDirectory.mkdirs();

        Directory theIndexFSDirectory = new NRTCachingDirectory(FSDirectory.open(theIndexDirectory.toPath()), 100, 100);

        IndexWriterConfig theConfig = new IndexWriterConfig(analyzer);
        indexWriter = new IndexWriter(theIndexFSDirectory, theConfig);

        searcherManager = new SearcherManager(indexWriter, true, true, new SearcherFactory());

        commitThread = new Thread("Lucene Commit Thread") {
            @Override
            public void run() {
                while (!isInterrupted()) {

                    if (indexWriter.hasUncommittedChanges()) {
                        try {
                            indexWriter.commit();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // Do nothing here
                    }
                }
            }
        };
        commitThread.start();

        facetsConfig = new FacetsConfig();
    }

    public void crawlingStarts() throws IOException {
        searcherManager.maybeRefreshBlocking();
    }

    public void addToIndex(String aLocationId, Content aContent) throws IOException {
        Document theDocument = new Document();

        SupportedLanguage theLanguage = aContent.getLanguage();

        theDocument.add(new StringField(IndexFields.UNIQUEID, UUID.randomUUID().toString(), Field.Store.YES));
        theDocument.add(new StringField(IndexFields.FILENAME, aContent.getFileName(), Field.Store.YES));
        theDocument.add(new SortedSetDocValuesFacetField(IndexFields.LANGUAGEFACET, theLanguage.name()));
        theDocument.add(new TextField(IndexFields.LANGUAGESTORED, theLanguage.name(), Field.Store.YES));
        theDocument.add(new TextField(IndexFields.CONTENTMD5, DigestUtils.md5Hex(aContent.getFileContent()), Field.Store.YES));

        StringBuilder theContentAsString = new StringBuilder(aContent.getFileContent());

        aContent.getMetadata().forEach(theEntry -> {
            if (!StringUtils.isEmpty(theEntry.key)) {
                Object theValue = theEntry.value;
                if (theValue instanceof String) {
                    facetsConfig.setMultiValued(theEntry.key, true);
                    String theStringValue = (String) theValue;
                    theContentAsString.append(" ").append(theStringValue);
                    if (!StringUtils.isEmpty(theStringValue)) {
                        theDocument.add(new SortedSetDocValuesFacetField(theEntry.key, theStringValue));
                    }
                }
                if (theValue instanceof Date) {
                    facetsConfig.setHierarchical(theEntry.key, true);
                    Date theDateValue = (Date) theValue;
                    Calendar theCalendar = GregorianCalendar.getInstance(DateUtils.UTC, Locale.US);
                    theCalendar.setTime(theDateValue);

                    // Full-Path
                    {
                        String thePathInfo = String.format(
                                "%04d/%02d/%02d",
                                theCalendar.get(Calendar.YEAR),
                                theCalendar.get(Calendar.MONTH) + 1,
                                theCalendar.get(Calendar.DAY_OF_MONTH));

                        theContentAsString.append(" ").append(thePathInfo);

                        facetsConfig.setMultiValued(theEntry.key+"-year-month-day", true);
                        theDocument.add(new SortedSetDocValuesFacetField(theEntry.key+"-year-month-day", thePathInfo));
                    }
                    // Year
                    {
                        String thePathInfo = String.format(
                                "%04d",
                                theCalendar.get(Calendar.YEAR));

                        theContentAsString.append(" ").append(thePathInfo);

                        facetsConfig.setMultiValued(theEntry.key+"-year", true);
                        theDocument.add(new SortedSetDocValuesFacetField(theEntry.key+"-year", thePathInfo));
                    }
                    // Year-month
                    {
                        String thePathInfo = String.format(
                                "%04d/%02d",
                                theCalendar.get(Calendar.YEAR),
                                theCalendar.get(Calendar.MONTH) + 1);

                        theContentAsString.append(" ").append(thePathInfo);

                        facetsConfig.setMultiValued(theEntry.key+"-year-month", true);
                        theDocument.add(new SortedSetDocValuesFacetField(theEntry.key+"-year-month", thePathInfo));
                    }

                }
            }
        });

        if (analyzerCache.supportsLanguage(theLanguage)) {
            LOGGER.info("Language and analyzer " + theLanguage+" detected for " + aContent.getFileName()+", using the corresponding language index field");
            String theFieldName = analyzerCache.getFieldNameFor(theLanguage);
            theDocument.add(new Field(theFieldName, theContentAsString.toString(), contentFieldType));
        } else {
            LOGGER.info("No matching language and analyzer detected for " + theLanguage+" and " + aContent.getFileName()+", using the default index field and analyzer");
            theDocument.add(new Field(IndexFields.CONTENT, theContentAsString.toString(), contentFieldType));
        }

        theDocument.add(new Field(IndexFields.CONTENT_NOT_STEMMED, theContentAsString.toString(), contentFieldType));

        theDocument.add(new TextField(IndexFields.CONTENTMD5, DigestUtils.md5Hex(aContent.getFileContent()), Field.Store.YES));
        theDocument.add(new StringField(IndexFields.LOCATIONID, aLocationId, Field.Store.YES));
        theDocument.add(new LegacyLongField(IndexFields.FILESIZE, aContent.getFileSize(), Field.Store.YES));
        theDocument.add(new LegacyLongField(IndexFields.LASTMODIFIED, aContent.getLastModified(), Field.Store.YES));

        // Update the document in our search index
        indexWriter.updateDocument(new Term(IndexFields.FILENAME, aContent.getFileName()), facetsConfig.build(theDocument));
    }

    public void removeFromIndex(String aFileName) throws IOException {
        indexWriter.deleteDocuments(new Term(IndexFields.FILENAME, aFileName));
    }

    public void shutdown() {
        commitThread.interrupt();
        try {
            indexWriter.close();
        } catch (Exception e) {
            LOGGER.error("Error while closing IndexWriter", e);
        }
    }

    public UpdateCheckResult checkIfModified(String aFilename, long aLastModified) throws IOException {

        IndexSearcher theSearcher = searcherManager.acquire();
        try {
            Query theQuery = new TermQuery(new Term(IndexFields.FILENAME, aFilename));
            TopDocs theDocs = theSearcher.search(theQuery, 100, Sort.INDEXORDER);
            if (theDocs.scoreDocs.length == 0) {
                return UpdateCheckResult.UPDATED;
            }
            if (theDocs.scoreDocs.length > 1) {
                // Multiple documents in index, we need to clean up
                return UpdateCheckResult.UPDATED;
            }
            ScoreDoc theFirstScore = theDocs.scoreDocs[0];
            Document theDocument = theSearcher.doc(theFirstScore.doc);

            long theStoredLastModified = theDocument.getField(IndexFields.LASTMODIFIED).numericValue().longValue();
            if (theStoredLastModified != aLastModified) {
                return UpdateCheckResult.UPDATED;
            }
            return UpdateCheckResult.UNMODIFIED;
        } finally {
            searcherManager.release(theSearcher);
        }
    }

    private String encode(String aValue) {
        URLCodec theURLCodec = new URLCodec();
        try {
            return theURLCodec.encode(aValue);
        } catch (EncoderException e) {
            return null;
        }
    }

    private BooleanQuery computeBooleanQueryFor(String aQueryString) throws IOException {
        QueryParser theParser = new QueryParser(analyzer);

        BooleanQuery.Builder theBooleanQuery = new BooleanQuery.Builder();
        theBooleanQuery.setMinimumNumberShouldMatch(1);

        for (String theFieldName : analyzerCache.getAllFieldNames()) {
            Query theSingle = theParser.parse(aQueryString, theFieldName);
            theBooleanQuery.add(theSingle, BooleanClause.Occur.SHOULD);
        }
        return theBooleanQuery.build();
    }

    public QueryResult performQuery(String aQueryString, String aBacklink, String aBasePath, Configuration aConfiguration, Map<String, String> aDrilldownFields) throws IOException {

        searcherManager.maybeRefreshBlocking();
        IndexSearcher theSearcher = searcherManager.acquire();
        SortedSetDocValuesReaderState theSortedSetState = new DefaultSortedSetDocValuesReaderState(theSearcher.getIndexReader());

        List<QueryResultDocument> theResultDocuments = new ArrayList<>();

        long theStartTime = System.currentTimeMillis();

        LOGGER.info("Querying for "+aQueryString);

        DateFormat theDateFormat = new SimpleDateFormat("dd.MMMM.yyyy", Locale.ENGLISH);

        try {

            List<FacetDimension> theDimensions = new ArrayList<>();

            // Search only if a search query is given
            if (!StringUtils.isEmpty(aQueryString)) {

                Query theQuery = computeBooleanQueryFor(aQueryString);

                LOGGER.info(" query is " + theQuery);

                theQuery = theQuery.rewrite(theSearcher.getIndexReader());

                LOGGER.info(" rewritten query is " + theQuery);

                DrillDownQuery theDrilldownQuery = new DrillDownQuery(facetsConfig, theQuery);
                aDrilldownFields.entrySet().stream().forEach(aEntry -> {
                    LOGGER.info(" with Drilldown "+aEntry.getKey()+" for "+aEntry.getValue());
                    theDrilldownQuery.add(aEntry.getKey(), aEntry.getValue());
                });

                FacetsCollector theFacetCollector = new FacetsCollector();

                TopDocs theDocs = FacetsCollector.search(theSearcher, theDrilldownQuery, aConfiguration.getNumberOfSearchResults(), Sort.RELEVANCE, true, true, theFacetCollector);
                SortedSetDocValuesFacetCounts theFacetCounts = new SortedSetDocValuesFacetCounts(theSortedSetState, theFacetCollector);

                List<Facet> theAuthorFacets = new ArrayList<>();
                List<Facet> theFileTypesFacets = new ArrayList<>();
                List<Facet> theLastModifiedYearFacet = new ArrayList<>();
                List<Facet> theLanguageFacet = new ArrayList<>();

                LOGGER.info("Found " + theDocs.scoreDocs.length + " documents");

                // We need this cache to detect duplicate documents while searching for similarities
                Set<Integer> theUniqueDocumentsFound = new HashSet<>();

                Map<String, QueryResultDocument> theDocumentsByHash = new HashMap<>();

                for (int i = 0; i < theDocs.scoreDocs.length; i++) {
                    int theDocumentID = theDocs.scoreDocs[i].doc;
                    theUniqueDocumentsFound.add(theDocumentID);
                    Document theDocument = theSearcher.doc(theDocumentID);

                    String theUniqueID = theDocument.getField(IndexFields.UNIQUEID).stringValue();
                    String theFoundFileName = theDocument.getField(IndexFields.FILENAME).stringValue();
                    String theHash = theDocument.getField(IndexFields.CONTENTMD5).stringValue();
                    QueryResultDocument theExistingDocument = theDocumentsByHash.get(theHash);
                    if (theExistingDocument != null) {
                        theExistingDocument.addFileName(theFoundFileName);
                    } else {
                        Date theLastModified = new Date(theDocument.getField(IndexFields.LASTMODIFIED).numericValue().longValue());
                        SupportedLanguage theLanguage = SupportedLanguage.valueOf(theDocument.getField(IndexFields.LANGUAGESTORED).stringValue());
                        String theFieldName;
                        if (analyzerCache.supportsLanguage(theLanguage)) {
                            theFieldName = analyzerCache.getFieldNameFor(theLanguage);
                        } else {
                            theFieldName = IndexFields.CONTENT;
                        }

                        String theOriginalContent = theDocument.getField(theFieldName).stringValue();

                        final Query theFinalQuery = theQuery;

                        ForkJoinTask<String> theHighligherResult = executorPool.submit(() -> {
                            StringBuilder theResult = new StringBuilder(theDateFormat.format(theLastModified));
                            theResult.append("&nbsp;-&nbsp;");
                            Highlighter theHighlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(theFinalQuery));
                            for (String theFragment : theHighlighter.getBestFragments(analyzer, theFieldName, theOriginalContent, NUMBER_OF_FRAGMENTS)) {
                                if (theResult.length() > 0) {
                                    theResult = theResult.append("...");
                                }
                                theResult = theResult.append(theFragment);
                            }
                            return theResult.toString();
                        });

                        int theNormalizedScore = (int)(theDocs.scoreDocs[i].score / theDocs.getMaxScore() * 5);

                        File theFileOnDisk = new File(theFoundFileName);
                        if (theFileOnDisk.exists()) {

                            boolean thePreviewAvailable = previewProcessor.previewAvailableFor(theFileOnDisk);

                            theExistingDocument = new QueryResultDocument(theDocumentID, theFoundFileName, theHighligherResult,
                                    Long.parseLong(theDocument.getField(IndexFields.LASTMODIFIED).stringValue()),
                                    theNormalizedScore, theUniqueID, thePreviewAvailable);
                            theDocumentsByHash.put(theHash, theExistingDocument);
                            theResultDocuments.add(theExistingDocument);
                        }
                    }
                }

                if (aConfiguration.isShowSimilarDocuments()) {

                    MoreLikeThis theMoreLikeThis = new MoreLikeThis(theSearcher.getIndexReader());
                    theMoreLikeThis.setAnalyzer(analyzer);
                    theMoreLikeThis.setMinTermFreq(1);
                    theMoreLikeThis.setMinDocFreq(1);
                    theMoreLikeThis.setFieldNames(analyzerCache.getAllFieldNames());

                    for (QueryResultDocument theDocument : theResultDocuments) {
                        Query theMoreLikeThisQuery = theMoreLikeThis.like(theDocument.getDocumentID());
                        TopDocs theMoreLikeThisTopDocs = theSearcher.search(theMoreLikeThisQuery, 5);
                        for (ScoreDoc theMoreLikeThisScoreDoc : theMoreLikeThisTopDocs.scoreDocs) {
                            int theSimilarDocument = theMoreLikeThisScoreDoc.doc;
                            if (theUniqueDocumentsFound.add(theSimilarDocument)) {
                                Document theMoreLikeThisDocument = theSearcher.doc(theSimilarDocument);
                                String theFilename = theMoreLikeThisDocument.getField(IndexFields.FILENAME).stringValue();
                                theDocument.addSimilarFile(theFilename);
                            }
                        }
                    }
                }

                LOGGER.info("Got Dimensions");
                for (FacetResult theResult : theFacetCounts.getAllDims(20000)) {
                    String theDimension = theResult.dim;
                    if ("author".equals(theDimension)) {
                        for (LabelAndValue theLabelAndValue : theResult.labelValues) {
                            if (!StringUtils.isEmpty(theLabelAndValue.label)) {
                                theAuthorFacets.add(new Facet(theLabelAndValue.label, theLabelAndValue.value.intValue(),
                                        aBasePath + "/" + encode(
                                                FacetSearchUtils.encode(theDimension, theLabelAndValue.label))));
                            }
                        }
                    }
                    if ("extension".equals(theDimension)) {
                        for (LabelAndValue theLabelAndValue : theResult.labelValues) {
                            if (!StringUtils.isEmpty(theLabelAndValue.label)) {
                                theFileTypesFacets.add(new Facet(theLabelAndValue.label, theLabelAndValue.value.intValue(),
                                        aBasePath + "/" + encode(
                                                FacetSearchUtils.encode(theDimension, theLabelAndValue.label))));
                            }
                        }
                    }
                    if ("last-modified-year".equals(theDimension)) {
                        for (LabelAndValue theLabelAndValue : theResult.labelValues) {
                            if (!StringUtils.isEmpty(theLabelAndValue.label)) {
                                theLastModifiedYearFacet.add(new Facet(theLabelAndValue.label, theLabelAndValue.value.intValue(),
                                        aBasePath + "/" + encode(
                                                FacetSearchUtils.encode(theDimension, theLabelAndValue.label))));
                            }
                        }
                    }
                    if (IndexFields.LANGUAGEFACET.equals(theDimension)) {
                        for (LabelAndValue theLabelAndValue : theResult.labelValues) {
                            if (!StringUtils.isEmpty(theLabelAndValue.label)) {
                                Locale theLocale = new Locale(theLabelAndValue.label);
                                theLanguageFacet.add(new Facet(theLocale.getDisplayLanguage(Locale.ENGLISH),
                                        theLabelAndValue.value.intValue(), aBasePath + "/" + encode(
                                        FacetSearchUtils.encode(theDimension, theLabelAndValue.label))));
                            }
                        }
                    }

                    LOGGER.info(" "+theDimension);
                }

                if (!theAuthorFacets.isEmpty()) {
                    theDimensions.add(new FacetDimension("Author", theAuthorFacets));
                }
                if (!theLastModifiedYearFacet.isEmpty()) {
                    theDimensions.add(new FacetDimension("Last modified", theLastModifiedYearFacet));
                }
                if (!theFileTypesFacets.isEmpty()) {
                    theDimensions.add(new FacetDimension("File types", theFileTypesFacets));
                }
                if (!theLanguageFacet.isEmpty()) {
                    theDimensions.add(new FacetDimension("Language", theLanguageFacet));
                }

                // Wait for all Tasks to complete for the search result highlighter
                ForkJoinTask.helpQuiesce();
            }

            long theDuration = System.currentTimeMillis() - theStartTime;

            LOGGER.info("Total amount of time : "+theDuration+"ms");

            return new QueryResult(System.currentTimeMillis() - theStartTime, theResultDocuments, theDimensions, theSearcher.getIndexReader().numDocs(), aBacklink);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            searcherManager.release(theSearcher);
        }
    }

    public Suggestion[] findSuggestionTermsFor(String aTerm) throws IOException {

        searcherManager.maybeRefreshBlocking();
        IndexSearcher theSearcher = searcherManager.acquire();

        try {
            SearchPhraseSuggester theSuggester = new SearchPhraseSuggester(theSearcher, analyzer, configuration);
            List<Suggestion> theResult = theSuggester.suggestSearchPhrase(IndexFields.CONTENT_NOT_STEMMED, aTerm);

            return theResult.toArray(new Suggestion[theResult.size()]);

        } finally {
            searcherManager.release(theSearcher);
        }
    }

    public File getFileOnDiskForDocument(String aUniqueID) throws IOException {
        searcherManager.maybeRefreshBlocking();
        IndexSearcher theSearcher = searcherManager.acquire();

        try {
            TermQuery theTermQuery = new TermQuery(new Term(IndexFields.UNIQUEID, aUniqueID));
            TopDocs theTopDocs = theSearcher.search(theTermQuery, 1, Sort.INDEXORDER);
            if (theTopDocs.totalHits == 1) {
                Document theDocument = theSearcher.doc(theTopDocs.scoreDocs[0].doc);
                if (theDocument != null) {
                    return new File(theDocument.get(IndexFields.FILENAME));
                }
            }
            return null;
        } finally {
            searcherManager.release(theSearcher);
        }
    }

    public void cleanupDeadContent() throws IOException {
        searcherManager.maybeRefreshBlocking();
        IndexSearcher theSearcher = searcherManager.acquire();

        try {
            IndexReader theReader = theSearcher.getIndexReader();
            for (int i = 0; i < theReader.maxDoc(); i++) {
                Document theDocument = theReader.document(i);
                File theFile = new File(theDocument.getField(IndexFields.FILENAME).stringValue());
                if (!theFile.exists()) {
                    LOGGER.info("Removing file "+theFile+" from index as it does not exist anymore.");
                    String theUniqueID = theDocument.getField(IndexFields.UNIQUEID).stringValue();
                    indexWriter.deleteDocuments(new Term(IndexFields.UNIQUEID, theUniqueID));
                }
            }
        } finally {
            searcherManager.release(theSearcher);
        }
    }
}