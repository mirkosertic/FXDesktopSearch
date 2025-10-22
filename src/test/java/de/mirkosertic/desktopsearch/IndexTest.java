package de.mirkosertic.desktopsearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetsCollectorManager;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spell.DirectSpellChecker;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.search.spell.SuggestWord;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class IndexTest {

    @Test
    public void testSpelling() throws IOException {
        final File tempDirectory = new File("target/test-classes/test-data/temp");
        tempDirectory.mkdirs();
        final Directory directory = FSDirectory.open(tempDirectory.toPath());
        final Analyzer analyzer = new StandardAnalyzer();
        final IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        final IndexWriter indexWriter = new IndexWriter(directory, config);

        final FieldType contentFieldType = new FieldType(TextField.TYPE_STORED);
        contentFieldType.setStoreTermVectors(true);
        contentFieldType.setStoreTermVectorPositions(true);
        contentFieldType.setStoreTermVectorOffsets(true);

        final var theDocument = new Document();
        theDocument.add(new StringField(IndexFields.UNIQUEID, "uniqueid", Field.Store.YES));
        theDocument.add(new Field(IndexFields.CONTENT, "lala domain test driven design lala coredomain", contentFieldType));
        indexWriter.addDocument(theDocument);
        indexWriter.flush();
        indexWriter.commit();

        final DirectoryReader indexReader = DirectoryReader.open(indexWriter);
        final IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        final IntelligentQueryParser intelligentQueryParser = new IntelligentQueryParser(analyzer);
        final Query intelligentQuery = intelligentQueryParser.parse(IndexFields.CONTENT, "domin driven design", indexReader, 4);
        System.out.println("Intelligent query " + intelligentQuery);

        final FacetsCollectorManager facetsCollectorManager = new FacetsCollectorManager();
        final BooleanQuery.Builder drillDownQueryBuilder = new BooleanQuery.Builder();
        drillDownQueryBuilder.add(intelligentQuery, BooleanClause.Occur.MUST);
        final FacetsCollectorManager.FacetsResult facetResult = FacetsCollectorManager.search(indexSearcher, drillDownQueryBuilder.build(), 100, facetsCollectorManager);
        final TopDocs topDocs = facetResult.topDocs();

        final DirectSpellChecker checker = new DirectSpellChecker();
        checker.setMaxEdits(2);
        checker.setMinPrefix(1);
        checker.setMaxInspections(5);
        checker.setMinQueryLength(4);

        for (final SuggestWord suggest : checker.suggestSimilar(new Term(IndexFields.CONTENT, "domin"), 5, indexReader, SuggestMode.SUGGEST_ALWAYS)) {
            System.out.println("Found suggestion " + suggest.string + " with frequency " + suggest.freq);
        }

        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            final var scoreDoc = topDocs.scoreDocs[i];

            final Document doc = indexReader.storedFields().document(scoreDoc.doc);
            System.out.println("found Document " + doc.get(IndexFields.UNIQUEID) + " with score " + scoreDoc.score);
        }
        System.out.println("Finished");

    }
}
