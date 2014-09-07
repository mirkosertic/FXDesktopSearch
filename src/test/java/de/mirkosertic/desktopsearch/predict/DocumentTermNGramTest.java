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
        theNGram.build("der wolf liegt neben dem stuhl", "content", theAnalyzer);

        Term theTerm = theNGram.getTerm("wolf");
        RunRestriction theRestriction = new RunRestriction(theTerm);
        List<Prediction> thePredictions = theRestriction.predict(2, 5);
        Assert.assertEquals(2, thePredictions.size());
    }

    @Test
    public void testPredict2() throws IOException {
        DocumentTermNGram theNGram = new DocumentTermNGram();
        WhitespaceAnalyzer theAnalyzer = new WhitespaceAnalyzer();
        theAnalyzer.setVersion(IndexFields.LUCENE_VERSION);

        theNGram.build("der grosse wolf steht auf der wiese", "content", theAnalyzer);
        theNGram.build("der wolf liegt neben dem stuhl", "content", theAnalyzer);
        theNGram.build("der wolf liegt auf dem stuhl", "content", theAnalyzer);

        Term theWolfTerm = theNGram.getTerm("wolf");
        RunRestriction theRestriction = new RunRestriction(theWolfTerm);
        Assert.assertTrue(theRestriction.getRuns().isEmpty());
        theRestriction.addTransitionTo(theNGram.getTerm("liegt"));

        Assert.assertEquals(2, theRestriction.getRuns().size());

        List<Prediction> thePredictions = theRestriction.predict(2, 5);
        Assert.assertEquals(2, thePredictions.size());
    }

    @Test
    public void testPredict3() throws IOException {
        DocumentTermNGram theNGram = new DocumentTermNGram();
        WhitespaceAnalyzer theAnalyzer = new WhitespaceAnalyzer();
        theAnalyzer.setVersion(IndexFields.LUCENE_VERSION);

        theNGram.build("der grosse wolf steht auf der wiese", "content", theAnalyzer);
        theNGram.build("der wolf liegt neben dem stuhl", "content", theAnalyzer);
        theNGram.build("der wolf liegt auf dem stuhl", "content", theAnalyzer);

        Term theWolfTerm = theNGram.getTerm("wolf");
        RunRestriction theRestriction = new RunRestriction(theWolfTerm);
        Assert.assertTrue(theRestriction.getRuns().isEmpty());
        theRestriction.addTransitionTo(theNGram.getTerm("liegt"));
        theRestriction.addTransitionTo(theNGram.getTerm("auf"));

        Assert.assertEquals(1, theRestriction.getRuns().size());

        List<Prediction> thePredictions = theRestriction.predict(2, 5);
        Assert.assertEquals(1, thePredictions.size());
    }
}