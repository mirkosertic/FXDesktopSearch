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
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class LuceneIndexHandler {

    private static final Version LUCENE_VERSION = Version.LUCENE_48;
    private static final int NUMBER_OF_FRAGMENTS = 5;

    private File indexLocation;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private SearcherManager searcherManager;

    private Thread commitThread;

    public LuceneIndexHandler(File aIndexDir) throws IOException {
        indexLocation = aIndexDir;

        analyzer = new StandardAnalyzer(LUCENE_VERSION);
        FSDirectory theIndexFSDirectory = FSDirectory.open(indexLocation);
        if (theIndexFSDirectory.fileExists(IndexWriter.WRITE_LOCK_NAME)) {
            theIndexFSDirectory.clearLock(IndexWriter.WRITE_LOCK_NAME);
        }
        IndexWriterConfig theConfig = new IndexWriterConfig(LUCENE_VERSION, analyzer);
        indexWriter = new IndexWriter(theIndexFSDirectory, theConfig);
        searcherManager = new SearcherManager(indexWriter, true, new SearcherFactory());

        commitThread = new Thread() {
            @Override
            public void run() {
                while(!isInterrupted()) {

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

        for (Map.Entry<String, String> theEntry : aContent.getMetadata().entrySet()) {
            theDocument.add(new StringField(IndexFields.META_PREFIX + theEntry.getKey(), theEntry.getValue(), Field.Store.YES));
        }

        indexWriter.updateDocument(new Term(IndexFields.FILENAME, aContent.getFileName()), theDocument);
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

    public QueryResult performQuery(String aQueryString, boolean aIncludeSimilarDocuments, int aMaxDocs) throws IOException {

        searcherManager.maybeRefreshBlocking();
        IndexSearcher theSearcher = searcherManager.acquire();

        List<QueryResultDocument> theResultDocuments = new ArrayList<>();

        long theStartTime = System.currentTimeMillis();

        DateFormat theDateFormat = new SimpleDateFormat("dd.MMMM.yyyy", Locale.ENGLISH);

        try {
            // Search only if a search query is given
            if (!StringUtils.isEmpty(aQueryString)) {

                QueryParser theParser = new QueryParser();

                Query theQuery = theParser.parse(aQueryString, IndexFields.CONTENT);

                MoreLikeThis theMoreLikeThis = new MoreLikeThis(theSearcher.getIndexReader());
                theMoreLikeThis.setAnalyzer(analyzer);
                theMoreLikeThis.setMinTermFreq(1);
                theMoreLikeThis.setMinDocFreq(1);
                theMoreLikeThis.setFieldNames(new String[]{IndexFields.CONTENT});

                TopDocs theDocs = theSearcher.search(theQuery, null, aMaxDocs);

                for (int i=0;i<theDocs.scoreDocs.length;i++) {
                    ScoreDoc theScoreDoc = theDocs.scoreDocs[i];
                    Document theDocument = theSearcher.doc(theScoreDoc.doc);

                    String theFoundFileName = theDocument.getField(IndexFields.FILENAME).stringValue();
                    Date theLastModified = new Date(Long.parseLong(theDocument.getField(IndexFields.LASTMODIFIED).stringValue()));

                    String theOriginalContent = theDocument.getField(IndexFields.CONTENT).stringValue();

                    StringBuilder theHighlightedResult = new StringBuilder(theDateFormat.format(theLastModified));
                    theHighlightedResult.append("&nbsp;-&nbsp;");
                    Highlighter theHighlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(theQuery));
                    for (String theFragment : theHighlighter.getBestFragments(analyzer, IndexFields.CONTENT, theOriginalContent, NUMBER_OF_FRAGMENTS)) {
                        if (theHighlightedResult.length() > 0) {
                            theHighlightedResult = theHighlightedResult.append("...");
                        }
                        theHighlightedResult = theHighlightedResult.append(theFragment);
                    }

                    List<String> theSimilarFiles = new ArrayList<>();

                    if (aIncludeSimilarDocuments) {

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

                    theResultDocuments.add(new QueryResultDocument(theFoundFileName, theHighlightedResult.toString(), Long.parseLong(theDocument.getField(IndexFields.LASTMODIFIED).stringValue()), theSimilarFiles));
                }
            }

            return new QueryResult(System.currentTimeMillis() - theStartTime, theResultDocuments, theSearcher.getIndexReader().numDocs());
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
                        theYearElement = new DocFlareElement(""+theYear);
                        theYearMappings.put(theYear, theYearElement);

                        theLastEditedElement.getChildren().add(theYearElement);
                    }
                    DocFlareElement theYearMonthElement = theYearMonthMappings.get(theYear+"_"+theMonth);
                    if (theYearMonthElement == null) {
                        theYearMonthElement = new DocFlareElement(theCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH), 1);
                        theYearElement.getChildren().add(theYearMonthElement);
                        theYearMonthMappings.put(theYear+"_"+theMonth, theYearMonthElement);
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
