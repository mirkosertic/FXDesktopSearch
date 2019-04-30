package de.mirkosertic.desktopsearch;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;
import java.util.stream.Collectors;

public class NERPipelineTest {

    @Test
    public void testGerman() throws IOException {
        // set up pipeline properties
        final Properties props = new Properties();
        props.load(NERPipelineTest.class.getResourceAsStream("/StanfordCoreNLP-german.properties"));
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        props.setProperty("ner.useSUTime", "false");
        props.setProperty("ner.applyFineGrained", "false");

        // set up pipeline
        System.out.println("A");
        final StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // make an example document
        System.out.println("B");
        //CoreDocument doc = new CoreDocument("Mirko Sertic is living in Münster, Germany. He is almost 40 years old. He likes movies. It is strange how things go wrong in New York City.");
        final CoreDocument doc = new CoreDocument("Mirko Sertic lebt und wohnt in der Stadt Münster, Deutschland.");

        // annotate the document
        System.out.println("C");
        pipeline.annotate(doc);
        // view results
        System.out.println("D");
        System.out.println("---");
        System.out.println("entities found");
        for (final CoreEntityMention em : doc.entityMentions())
            System.out.println("\tdetected entity: \t"+em.text()+"\t"+em.entityType());
        System.out.println("---");
        System.out.println("tokens and ner tags");
        final String tokensAndNERTags = doc.tokens().stream().map(token -> "("+token.word()+","+token.ner()+")").collect(
                Collectors.joining(" "));
        System.out.println(tokensAndNERTags);
    }

    @Test
    public void testEnglish() throws IOException {
        // set up pipeline properties
        final Properties props = new Properties();
        props.load(NERPipelineTest.class.getResourceAsStream("/StanfordCoreNLP.properties"));
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        props.setProperty("ner.useSUTime", "false");
        props.setProperty("ner.applyFineGrained", "false");

        // set up pipeline
        System.out.println("A");
        final StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // make an example document
        System.out.println("B");
        //CoreDocument doc = new CoreDocument("Mirko Sertic is living in Münster, Germany. He is almost 40 years old. He likes movies. It is strange how things go wrong in New York City.");
        final CoreDocument doc = new CoreDocument("Mirko Sertic lebt und wohnt in der Stadt Münster, Deutschland.");

        // annotate the document
        System.out.println("C");
        pipeline.annotate(doc);
        // view results
        System.out.println("D");
        System.out.println("---");
        System.out.println("entities found");
        for (final CoreEntityMention em : doc.entityMentions())
            System.out.println("\tdetected entity: \t"+em.text()+"\t"+em.entityType());
        System.out.println("---");
        System.out.println("tokens and ner tags");
        final String tokensAndNERTags = doc.tokens().stream().map(token -> "("+token.word()+","+token.ner()+")").collect(
                Collectors.joining(" "));
        System.out.println(tokensAndNERTags);
    }
}
