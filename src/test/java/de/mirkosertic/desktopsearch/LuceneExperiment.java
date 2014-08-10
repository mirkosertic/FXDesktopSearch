package de.mirkosertic.desktopsearch;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

public class LuceneExperiment {

    public static void main(String[] args) throws IOException {
        StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
        FSDirectory fsDirectory = FSDirectory.open(new File("D:\\Temp\\indextest"));
        if (fsDirectory.fileExists(IndexWriter.WRITE_LOCK_NAME)) {
            fsDirectory.clearLock(IndexWriter.WRITE_LOCK_NAME);
        }
        IndexWriterConfig theConfig = new IndexWriterConfig(Version.LUCENE_42, analyzer);
        IndexWriter indexWriter = new IndexWriter(fsDirectory, theConfig);

        SearcherManager searcherManager = new SearcherManager(indexWriter, true, new SearcherFactory());

        System.out.println("Step1");
        searcherManager.maybeRefreshBlocking();
        IndexSearcher theSearcher = searcherManager.acquire();
        System.out.println(theSearcher.getIndexReader().numDocs()+" / " + theSearcher.getIndexReader().numDeletedDocs());
        searcherManager.release(theSearcher);

        Document theDocument = new Document();
        theDocument.add(new Field("ID","lala", Field.Store.YES, Field.Index.ANALYZED));
        indexWriter.addDocument(theDocument);

        System.out.println("Step2");
        searcherManager.maybeRefreshBlocking();
        theSearcher = searcherManager.acquire();
        System.out.println(theSearcher.getIndexReader().numDocs()+" / " + theSearcher.getIndexReader().numDeletedDocs());
        searcherManager.release(theSearcher);

        indexWriter.commit();

        System.out.println("Step3");
        searcherManager.maybeRefreshBlocking();
        theSearcher = searcherManager.acquire();
        System.out.println(theSearcher.getIndexReader().numDocs()+" / " + theSearcher.getIndexReader().numDeletedDocs());
        searcherManager.release(theSearcher);

        System.out.println("Step4");

        indexWriter.deleteDocuments(new Term("ID","lala"));
        searcherManager.maybeRefreshBlocking();
        theSearcher = searcherManager.acquire();
        System.out.println(theSearcher.getIndexReader().numDocs()+" / " + theSearcher.getIndexReader().numDeletedDocs());
        searcherManager.release(theSearcher);

        System.out.println("Step5");
        searcherManager.maybeRefreshBlocking();
        theSearcher = searcherManager.acquire();
        Query theQuery = new TermQuery(new Term("ID", "lala"));
        TopDocs theDocs = theSearcher.search(theQuery, null, 100);
        System.out.println(theDocs.totalHits+" "+theSearcher.getIndexReader().numDocs());
        searcherManager.release(theSearcher);
    }
}
