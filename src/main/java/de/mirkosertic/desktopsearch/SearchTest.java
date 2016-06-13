package de.mirkosertic.desktopsearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermAutomatonQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;

public class SearchTest {

    public static void main(String[] args) throws IOException {
        Directory theDirectory = FSDirectory.open(new File("/home/sertic/FreeSearchIndexDir/index").toPath());
        IndexReader theReader = DirectoryReader.open(theDirectory);
        IndexSearcher theSearcher = new IndexSearcher(theReader);

        // Term Query maxScore = 94.1545 / 0.3337ms per Query
        PhraseQuery.Builder theQuery = new PhraseQuery.Builder();
        theQuery.add(new Term(IndexFields.CONTENT_NOT_STEMMED, "mirko"));
        theQuery.add(new Term(IndexFields.CONTENT_NOT_STEMMED, "sertic"));

        // Phrase Query maxScore = 54.360126 / 0.3248ms per Query
        SpanQuery[] theQueries = new SpanQuery[2];
        theQueries[0] = new SpanTermQuery(new Term(IndexFields.CONTENT_NOT_STEMMED, "mirko"));
        theQueries[1] = new SpanTermQuery(new Term(IndexFields.CONTENT_NOT_STEMMED, "sertic"));
        SpanNearQuery theQuery2 = new SpanNearQuery(theQueries, 0, true);

        Analyzer theStandardAnalyzer = new StandardAnalyzer();
        theStandardAnalyzer.setVersion(IndexFields.LUCENE_VERSION);
        QueryParser theParser = new QueryParser(theStandardAnalyzer);
        Query thequery3 = theParser.parse("mirko sertic", IndexFields.CONTENT_NOT_STEMMED);

        System.out.println(thequery3);

        // Termautomaton Query
        TermAutomatonQuery theQuery3 = new TermAutomatonQuery(IndexFields.CONTENT_NOT_STEMMED);
        int theInit = theQuery3.createState();
        int theState2 = theQuery3.createState();
        int theState3 = theQuery3.createState();
        theQuery3.addTransition(theInit, theState2, "mirko");
        theQuery3.setAccept(theState3, true);
        theQuery3.addTransition(theState2, theState3, "sertic");
        theQuery3.finish();

        int theMax = 1;

        long theStart = System.currentTimeMillis();

        for (int count=theMax;count >= 0 ;count--) {
            FacetsCollector theFacetCollector = new FacetsCollector();

            TopDocs theDocs = FacetsCollector.search(theSearcher, thequery3, 5, null, theFacetCollector);
            if (count == 0) {
                System.out.println("maxScore = " + theDocs.getMaxScore());
                System.out.println("Size = " + theDocs.scoreDocs.length);
                for (int i = 0; i < theDocs.scoreDocs.length; i++) {
                    System.out.println(theDocs.scoreDocs[i].score + " " + theDocs.scoreDocs[i].doc);
                }
            }
        }

        long theDuration = System.currentTimeMillis() - theStart;

        System.out.println("Duration for "+theMax+" queries = "+theDuration);
        System.out.println(((double) theDuration / theMax) + " ms / query");
    }
}