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
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.tika.utils.DateUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

class LuceneIndexHandler {

    private static final Version LUCENE_VERSION = Version.LUCENE_4_9;
    private static final int NUMBER_OF_FRAGMENTS = 5;

    private final File indexLocation;
    private final Analyzer analyzer;
    private IndexWriter indexWriter;
    private SearcherManager searcherManager;
    private final FacetsConfig facetsConfig;

    private Thread commitThread;

    public LuceneIndexHandler(File aIndexDir) throws IOException {
        indexLocation = aIndexDir;

        File theIndexDir = new File(aIndexDir, "index");
        theIndexDir.mkdirs();

        analyzer = new StandardAnalyzer(LUCENE_VERSION);
        Directory theIndexFSDirectory = new NRTCachingDirectory(FSDirectory.open(theIndexDir), 100, 100);
        try {
            theIndexFSDirectory.clearLock(IndexWriter.WRITE_LOCK_NAME);
        } catch (IOException e) {
            // No Lock there
        }

        IndexWriterConfig theConfig = new IndexWriterConfig(LUCENE_VERSION, analyzer);
        indexWriter = new IndexWriter(theIndexFSDirectory, theConfig);

        searcherManager = new SearcherManager(indexWriter, true, new SearcherFactory());

        commitThread = new Thread() {
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

        theDocument.add(new StringField(IndexFields.FILENAME, aContent.getFileName(), Field.Store.YES));

        theDocument.add(new TextField(IndexFields.CONTENT, aContent.getFileContent(), Field.Store.YES));
        theDocument.add(new StringField(IndexFields.LOCATIONID, aLocationId, Field.Store.YES));
        theDocument.add(new LongField(IndexFields.FILESIZE, aContent.getFileSize(), Field.Store.YES));
        theDocument.add(new StringField(IndexFields.LASTMODIFIED, "" + aContent.getLastModified(), Field.Store.YES));

        aContent.getMetadata().forEach(theEntry -> {
            if (!StringUtils.isEmpty(theEntry.key)) {
                Object theValue = theEntry.value;
                if (theValue instanceof String) {
                    facetsConfig.setMultiValued(theEntry.key, true);
                    String theStringValue = (String) theValue;
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

                        facetsConfig.setMultiValued(theEntry.key+"-year-month-day", true);
                        theDocument.add(new SortedSetDocValuesFacetField(theEntry.key+"-year-month-day", thePathInfo));
                    }
                    // Year
                    {
                        String thePathInfo = String.format(
                                "%04d",
                                theCalendar.get(Calendar.YEAR));

                        facetsConfig.setMultiValued(theEntry.key+"-year", true);
                        theDocument.add(new SortedSetDocValuesFacetField(theEntry.key+"-year", thePathInfo));
                    }
                    // Year-month
                    {
                        String thePathInfo = String.format(
                                "%04d/%02d",
                                theCalendar.get(Calendar.YEAR),
                                theCalendar.get(Calendar.MONTH) + 1);

                        facetsConfig.setMultiValued(theEntry.key+"-year-month", true);
                        theDocument.add(new SortedSetDocValuesFacetField(theEntry.key+"-year-month", thePathInfo));
                    }

                }
            }
        });

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
        }
    }

    public UpdateCheckResult checkIfModified(String aFilename, long aLastModified) throws IOException {

        IndexSearcher theSearcher = searcherManager.acquire();
        try {
            Query theQuery = new TermQuery(new Term(IndexFields.FILENAME, aFilename));
            TopDocs theDocs = theSearcher.search(theQuery, null, 100);
            if (theDocs.scoreDocs.length == 0) {
                return UpdateCheckResult.UPDATED;
            }
            if (theDocs.scoreDocs.length > 1) {
                // Multiple documents in index, we need to clean up
                return UpdateCheckResult.UPDATED;
            }
            ScoreDoc theFirstScore = theDocs.scoreDocs[0];
            Document theDocument = theSearcher.doc(theFirstScore.doc);

            long theStoredLastModified = Long.parseLong(theDocument.getField(IndexFields.LASTMODIFIED).stringValue());
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

    public QueryResult performQuery(String aQueryString, String aBacklink, String aBasePath, boolean aIncludeSimilarDocuments, int aMaxDocs, Map<String, String> aDrilldownFields) throws IOException {

        searcherManager.maybeRefreshBlocking();
        IndexSearcher theSearcher = searcherManager.acquire();
        SortedSetDocValuesReaderState theSortedSetState = new DefaultSortedSetDocValuesReaderState(theSearcher.getIndexReader());

        List<QueryResultDocument> theResultDocuments = new ArrayList<>();

        long theStartTime = System.currentTimeMillis();

        DateFormat theDateFormat = new SimpleDateFormat("dd.MMMM.yyyy", Locale.ENGLISH);

        try {

            List<FacetDimension> theDimensions = new ArrayList<>();

            // Search only if a search query is given
            if (!StringUtils.isEmpty(aQueryString)) {

                QueryParser theParser = new QueryParser();

                Query theQuery = theParser.parse(aQueryString, IndexFields.CONTENT);

                DrillDownQuery theDrilldownQuery = new DrillDownQuery(facetsConfig, theQuery);
                aDrilldownFields.entrySet().stream().forEach(aEntry -> {
                    theDrilldownQuery.add(aEntry.getKey(), aEntry.getValue());
                });

                FacetsCollector theFacetCollector = new FacetsCollector();

                TopDocs theDocs = FacetsCollector.search(theSearcher, theDrilldownQuery, null, aMaxDocs, theFacetCollector);
                SortedSetDocValuesFacetCounts theFacetCounts = new SortedSetDocValuesFacetCounts(theSortedSetState, theFacetCollector);

                List<Facet> theAuthorFacets = new ArrayList<>();
                List<Facet> theFileTypesFacets = new ArrayList<>();
                List<Facet> theLastModifiedYearFacet = new ArrayList<>();

                ForkJoinPool theCommonPool = ForkJoinPool.commonPool();

                for (int i = 0; i < theDocs.scoreDocs.length; i++) {
                    ScoreDoc theScoreDoc = theDocs.scoreDocs[i];
                    Document theDocument = theSearcher.doc(theScoreDoc.doc);

                    String theFoundFileName = theDocument.getField(IndexFields.FILENAME).stringValue();
                    Date theLastModified = new Date(Long.parseLong(theDocument.getField(IndexFields.LASTMODIFIED).stringValue()));

                    String theOriginalContent = theDocument.getField(IndexFields.CONTENT).stringValue();

                    ForkJoinTask<String> theHighligherResult = theCommonPool.submit(() -> {
                        StringBuilder theResult = new StringBuilder(theDateFormat.format(theLastModified));
                        theResult.append("&nbsp;-&nbsp;");
                        Highlighter theHighlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(theQuery));
                        for (String theFragment : theHighlighter.getBestFragments(analyzer, IndexFields.CONTENT, theOriginalContent, NUMBER_OF_FRAGMENTS)) {
                            if (theResult.length() > 0) {
                                theResult = theResult.append("...");
                            }
                            theResult = theResult.append(theFragment);
                        }
                        return theResult.toString();
                    });

                    List<String> theSimilarFiles = new ArrayList<>();

                    if (aIncludeSimilarDocuments) {

                        MoreLikeThis theMoreLikeThis = new MoreLikeThis(theSearcher.getIndexReader());
                        theMoreLikeThis.setAnalyzer(analyzer);
                        theMoreLikeThis.setMinTermFreq(1);
                        theMoreLikeThis.setMinDocFreq(1);
                        theMoreLikeThis.setFieldNames(new String[]{IndexFields.CONTENT});

                        Query theMoreLikeThisQuery = theMoreLikeThis.like(theDocs.scoreDocs[i].doc);
                        TopDocs theMoreLikeThisTopDocs = theSearcher.search(theMoreLikeThisQuery, 5);
                        for (ScoreDoc theMoreLikeThisScoreDoc : theMoreLikeThisTopDocs.scoreDocs) {
                            Document theMoreLikeThisDocument = theSearcher.doc(theMoreLikeThisScoreDoc.doc);

                            String theFilename = theMoreLikeThisDocument.getField(IndexFields.FILENAME).stringValue();
                            if (!theFoundFileName.equals(theFilename)) {
                                if (!theSimilarFiles.contains(theFilename)) {
                                    theSimilarFiles.add(theFilename);
                                }
                            }
                        }
                    }

                    theResultDocuments.add(new QueryResultDocument(theFoundFileName, theHighligherResult, Long.parseLong(theDocument.getField(IndexFields.LASTMODIFIED).stringValue()), theSimilarFiles));
                }

                System.out.println("Dimensions");
                for (FacetResult theResult : theFacetCounts.getAllDims(20000)) {
                    String theDimension = theResult.dim;
                    if ("author".equals(theDimension)) {
                        for (LabelAndValue theLabelAndValue : theResult.labelValues) {
                            theAuthorFacets.add(new Facet(theLabelAndValue.label, theLabelAndValue.value.intValue(), aBasePath+"/"+encode(
                                    FacetSearchUtils.encode(theDimension, theLabelAndValue.label))));
                        }
                    }
                    if ("extension".equals(theDimension)) {
                        for (LabelAndValue theLabelAndValue : theResult.labelValues) {
                            theFileTypesFacets.add(new Facet(theLabelAndValue.label, theLabelAndValue.value.intValue(), aBasePath+"/"+encode(
                                    FacetSearchUtils.encode(theDimension, theLabelAndValue.label))));
                        }
                    }
                    if ("last-modified-year".equals(theDimension)) {
                        for (LabelAndValue theLabelAndValue : theResult.labelValues) {
                            theLastModifiedYearFacet.add(new Facet(theLabelAndValue.label, theLabelAndValue.value.intValue(), aBasePath+"/"+encode(
                                    FacetSearchUtils.encode(theDimension, theLabelAndValue.label))));
                        }
                    }

                    System.out.println(" "+theDimension);
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

                // Wait for all Tasks to complete for the search result highlighter
                ForkJoinTask.helpQuiesce();
            }

            return new QueryResult(System.currentTimeMillis() - theStartTime, theResultDocuments, theDimensions, theSearcher.getIndexReader().numDocs(), aBacklink);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            searcherManager.release(theSearcher);
        }
    }

    public File getIndexLocation() {
        return indexLocation;
    }

    public DocFlareElement getDocFlare() throws IOException {
        DocFlareElement theRoot = new DocFlareElement("Index");

        searcherManager.maybeRefreshBlocking();
        IndexSearcher theSearcher = searcherManager.acquire();
        try {

            DocFlareElement theAuthorsElement = new DocFlareElement("Authors");
            Terms theAuthors = MultiFields.getTerms(theSearcher.getIndexReader(), IndexFields.META_PREFIX + "author");
            if (theAuthors != null) {
                BytesRef theTerm;
                TermsEnum theTerms = theAuthors.iterator(null);
                while ((theTerm = theTerms.next()) != null) {
                    theAuthorsElement.getChildren().add(new DocFlareElement(theTerm.utf8ToString(), theTerms.docFreq()));
                }
            }
            if (theAuthorsElement.getChildren().size() != 0) {
                theRoot.getChildren().add(theAuthorsElement);
            }

            DocFlareElement theFiletypesElements = new DocFlareElement("File types");
            Terms TheFileTypes = MultiFields.getTerms(theSearcher.getIndexReader(), IndexFields.META_PREFIX + IndexFields.EXTENSION);
            if (TheFileTypes != null) {
                BytesRef theTerm;
                TermsEnum theTerms = TheFileTypes.iterator(null);
                while ((theTerm = theTerms.next()) != null) {
                    theFiletypesElements.getChildren().add(new DocFlareElement(theTerm.utf8ToString(), theTerms.docFreq()));
                }
            }
            if (theFiletypesElements.getChildren().size() != 0) {
                theRoot.getChildren().add(theFiletypesElements);
            }

            DocFlareElement theLastEditedElement = new DocFlareElement("Last Edited");
            Terms theLastEdited = MultiFields.getTerms(theSearcher.getIndexReader(), IndexFields.LASTMODIFIED);
            if (theLastEdited != null) {
                BytesRef theTerm;
                TermsEnum theTerms = theLastEdited.iterator(null);
                Calendar theCalendar = Calendar.getInstance();

                Map<Integer, DocFlareElement> theYearMappings = new HashMap<>();
                Map<String, DocFlareElement> theYearMonthMappings = new HashMap<>();

                while ((theTerm = theTerms.next()) != null) {
                    long theTimestamp = Long.parseLong(theTerm.utf8ToString());
                    theCalendar.setTimeInMillis(theTimestamp);

                    int theYear = theCalendar.get(Calendar.YEAR);
                    int theMonth = theCalendar.get(Calendar.MONTH) + 1;

                    DocFlareElement theYearElement = theYearMappings.get(theYear);
                    if (theYearElement == null) {
                        theYearElement = new DocFlareElement("" + theYear);
                        theYearMappings.put(theYear, theYearElement);

                        theLastEditedElement.getChildren().add(theYearElement);
                    }
                    DocFlareElement theYearMonthElement = theYearMonthMappings.get(theYear + "_" + theMonth);
                    if (theYearMonthElement == null) {
                        theYearMonthElement = new DocFlareElement(theCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH), 1);
                        theYearElement.getChildren().add(theYearMonthElement);
                        theYearMonthMappings.put(theYear + "_" + theMonth, theYearMonthElement);
                    } else {
                        theYearMonthElement.incrementWeight();
                    }
                }
            }

            if (theLastEditedElement.getChildren().size() != 0) {
                theRoot.getChildren().add(theLastEditedElement);
            }

        } finally {
            searcherManager.release(theSearcher);
        }

        return theRoot;
    }
}