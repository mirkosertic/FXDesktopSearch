package de.mirkosertic.desktopsearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SearchPhraseSuggester {

    private final IndexReader indexReader;
    private final Analyzer analyzer;
    private final Configuration configuration;

    public SearchPhraseSuggester(IndexReader aIndexReader, Analyzer aAnalyzer, Configuration aConfiguration) {
        indexReader = aIndexReader;
        analyzer = aAnalyzer;
        configuration = aConfiguration;
    }

    public List<Suggestion> suggestSearchPhrase(String aFieldName, String aPhrase) throws IOException {
        List<String> theTokens = toTokens(aFieldName, aPhrase);

        List<SpanQuery> theSpanQueries = theTokens.stream().map(s -> {
            if (QueryUtils.isWildCard(s)) {
                WildcardQuery theWildcardQuery = new WildcardQuery(new Term(aFieldName, s));
                SpanMultiTermQueryWrapper theWrapper = new SpanMultiTermQueryWrapper(theWildcardQuery);
                try {
                    return theWrapper.getRewriteMethod().rewrite(indexReader, theWildcardQuery);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return new SpanTermQuery(new Term(aFieldName, s));
        }).collect(Collectors.toList());

        SpanQuery theSpanQuery = new SpanNearQuery(theSpanQueries.toArray(new SpanQuery[theSpanQueries.size()]), configuration.getSuggestionSlop(), configuration.isSuggestionInOrder());
        AtomicReader theAtomicReader = SlowCompositeReaderWrapper.wrap(indexReader);

        Map<Term, TermContext> theTermContexts = new HashMap<>();
        Map<String, Long> theSpanFrequencies = new HashMap<>();

        // These are all the matching spans over all documents
        Spans theMatchingSpans = theSpanQuery
                .getSpans(theAtomicReader.getContext(), new Bits.MatchAllBits(indexReader.numDocs()), theTermContexts);

        while (theMatchingSpans.next()) {

            // This maps the position of a term and the term string itself
            // the positions must be in order, so we have to use a treemap.
            Map<Integer, String> theEntries = new TreeMap<>();

            Terms theAllTermsFromDocument = indexReader.getTermVector(theMatchingSpans.doc(), IndexFields.CONTENT_NOT_STEMMED);
            int theSpanStart = theMatchingSpans.start() - configuration.getSuggestionWindowBefore();
            int theSpanEnd = theMatchingSpans.end() + configuration.getSuggestionWindowAfter();
            TermsEnum theTermsEnum = theAllTermsFromDocument.iterator(null);
            BytesRef theTerm;
            while ((theTerm = theTermsEnum.next()) != null) {
                DocsAndPositionsEnum thePositionEnum = theTermsEnum.docsAndPositions(null, null);
                if (thePositionEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                    int i = 0;
                    int position;
                    while (i < thePositionEnum.freq() && (position = thePositionEnum.nextPosition()) != -1) {
                        if (position >= theSpanStart && position <= theSpanEnd) {
                            theEntries.put(position, theTerm.utf8ToString());
                        }
                        i++;
                    }
                }
            }

            StringBuilder theResultString = new StringBuilder();
            theEntries.entrySet().forEach(e -> {
                if (theResultString.length() > 0) {
                    theResultString.append(" ");
                }
                theResultString.append(e.getValue());
            });

            String theTotalSpan = theResultString.toString().trim();

            Long theFrequency = theSpanFrequencies.get(theTotalSpan);
            if (theFrequency == null) {
                theSpanFrequencies.put(theTotalSpan, 1L);
            } else {
                theSpanFrequencies.put(theTotalSpan, theFrequency + 1);
            }
        }

        return theSpanFrequencies.entrySet().stream().filter(t -> t.getValue() > 1).sorted(
                (o1, o2) -> o2.getValue().compareTo(o1.getValue())).limit(configuration.getNumberOfSuggestions()).map(
                T -> new Suggestion(highlight(T.getKey(), theTokens), T.getKey())).collect(Collectors.toList());


    }

    private String highlight(String aPhrase, List<String> aTokens) {
        String theResult = aPhrase;
        Set<String> theTokens = aTokens.stream().map(String::toLowerCase).collect(Collectors.toSet());

        for (String theToken : theTokens) {
            Pattern thePattern = Pattern.compile("(" + theToken+")", Pattern.CASE_INSENSITIVE);
            Matcher theMatcher = thePattern.matcher(aPhrase);
            Set<String> theReplacements = new HashSet<>();
            while(theMatcher.find()) {
                theReplacements.add(theMatcher.group());
            }
            for (String theReplacement : theReplacements) {
                theResult = theResult.replace(theReplacement, "<b>"+theReplacement+"</b>");
            }
        }
        return theResult;
    }

    private List<String> toTokens(String aFieldName, String aPhrase) throws IOException {
        List<String> theTokens = new ArrayList<>();

        TokenStream theTokenStream = analyzer.tokenStream(aFieldName, aPhrase);
        theTokenStream.reset();
        CharTermAttribute theCharTerms = theTokenStream.getAttribute(CharTermAttribute.class);
        while(theTokenStream.incrementToken()) {
            theTokens.add(theCharTerms.toString());
        }
        theTokenStream.end();
        theTokenStream.close();

        return theTokens;
    }
}
