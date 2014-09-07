package de.mirkosertic.desktopsearch.predict;

import de.mirkosertic.desktopsearch.IndexFields;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DocumentTermNGramTest {

    @Test
    public void testGenerate() throws IOException {
        DocumentTermNGram theNGram = new DocumentTermNGram();
        WhitespaceAnalyzer theAnalyzer = new WhitespaceAnalyzer();
        theAnalyzer.setVersion(IndexFields.LUCENE_VERSION);

        theNGram.build("der grosse wolf liegt auf der wiese", "content", theAnalyzer);
        theNGram.build("Wolf hat ohren", "content", theAnalyzer);

        Collection<Term> theTerms = theNGram.getTerms();
        Assert.assertEquals(8, theTerms.size());

        Term theTerm = theNGram.getTerm("wolf");
        Set<Term> theFollowingTerm = theTerm.getFollowingTerms();
        Assert.assertEquals(2, theFollowingTerm.size());
    }

    @Test
    public void testPredict() throws IOException {
        DocumentTermNGram theNGram = new DocumentTermNGram();
        WhitespaceAnalyzer theAnalyzer = new WhitespaceAnalyzer();
        theAnalyzer.setVersion(IndexFields.LUCENE_VERSION);

        theNGram.build("der grosse wolf liegt auf der wiese", "content", theAnalyzer);
        theNGram.build("Wolf hat ohren", "content", theAnalyzer);

        Term theTerm = theNGram.getTerm("wolf");
        List<Prediction> thePredictions = theTerm.predict(2, 5);
        Assert.assertEquals(4, thePredictions.size());
    }
}