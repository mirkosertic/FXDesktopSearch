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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.*;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SearchPhraseSuggester {

    private static final Logger LOGGER = Logger.getLogger(SearchPhraseSuggester.class);

    private final IndexReader indexReader;
    private final Analyzer analyzer;
    private final Configuration configuration;

    public SearchPhraseSuggester(IndexReader aIndexReader, Analyzer aAnalyzer, Configuration aConfiguration) {
        indexReader = aIndexReader;
        analyzer = aAnalyzer;
        configuration = aConfiguration;
    }

    public List<Suggestion> suggestSearchPhrase(String aFieldName, String aPhrase) throws IOException {

        LOGGER.info("Trying to find suggestions for phrase " + aPhrase);

        long theStartTime = System.currentTimeMillis();
        try {
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

            LOGGER.info("created span query " + theSpanQuery);

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
        } finally {
            long theDuration = System.currentTimeMillis() - theStartTime;
            LOGGER.info("Took "+theDuration+"ms");
        }
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

    private String analyze(String aFieldName, String aString) throws IOException {
        TokenStream theTokenStream = analyzer.tokenStream(aFieldName, aString);
        theTokenStream.reset();
        CharTermAttribute theCharTerms = theTokenStream.getAttribute(CharTermAttribute.class);
        try {
            if (theTokenStream.incrementToken()) {
                return theCharTerms.toString();
            }
            return null;
        } finally {
            theTokenStream.end();
            theTokenStream.close();
        }
    }

    private List<String> toTokens(String aFieldName, String aPhrase) throws IOException {
        List<String> theTokens = new ArrayList<>();

        String[] theSplitTokens = StringUtils.split(aPhrase," ,:;?!.");
        for (int i=0;i<theSplitTokens.length;i++) {
            String theToken = theSplitTokens[i];
            // Mutate the last token to a wildcard
            if (theToken.length() > 2 && i == theSplitTokens.length - 1 && !QueryUtils.isWildCard(theToken)) {
                theToken = theToken+QueryUtils.ASTERISK;
            }
            if (!theToken.startsWith("-")) {
                if (QueryUtils.isWildCard(theToken)) {
                    theTokens.add(theToken);
                } else {
                    String theAnalyzed = analyze(aFieldName, theToken);
                    if (theAnalyzed != null) {
                        theTokens.add(theAnalyzed);
                    }
                }
            }
        }

        return theTokens;
    }
}
