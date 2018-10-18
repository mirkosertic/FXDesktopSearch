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
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.TokenGroup;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SearchPhraseSuggester {

    public static interface SearchPhraseSuggesterConfiguration {

        int getSuggestionSlop();

        boolean isSuggestionInOrder();

        int getNumberOfSuggestions();
    }

    private static final Logger LOGGER = Logger.getLogger(SearchPhraseSuggester.class);

    private final IndexSearcher indexSearcher;
    private final Analyzer analyzer;
    private final SearchPhraseSuggesterConfiguration configuration;

    public SearchPhraseSuggester(
            final IndexSearcher aIndexSearcher, final Analyzer aAnalyzer, final SearchPhraseSuggesterConfiguration aConfiguration) {
        indexSearcher = aIndexSearcher;
        analyzer = aAnalyzer;
        configuration = aConfiguration;
    }

    public List<Suggestion> suggestSearchPhrase(final String aFieldName, final String aPhrase) throws IOException {

        LOGGER.info("Trying to find suggestions for phrase " + aPhrase);

        final List<String> theTokens = toTokens(aFieldName, aPhrase);

        final List<SpanQuery> theSpanQueries = theTokens.stream().map(s -> {
            if (QueryUtils.isWildCard(s)) {
                WildcardQuery theWildcardQuery = new WildcardQuery(new Term(aFieldName, s));
                SpanMultiTermQueryWrapper theWrapper = new SpanMultiTermQueryWrapper(theWildcardQuery);
                try {
                    return theWrapper.getRewriteMethod().rewrite(indexSearcher.getIndexReader(), theWildcardQuery);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return new SpanTermQuery(new Term(aFieldName, s));
        }).collect(Collectors.toList());

        final Query theQuery;
        if (theSpanQueries.size() > 1) {
            theQuery = new SpanNearQuery(theSpanQueries.toArray(new SpanQuery[theSpanQueries.size()]), configuration.getSuggestionSlop(), configuration.isSuggestionInOrder());
        } else {
            theQuery = theSpanQueries.get(0);
        }


        LOGGER.info("created span query " + theQuery);

        final ArrayList<Suggestion> theResult = new ArrayList<>();

        final Highlighter theHighligher = new Highlighter((aSpan, tokenGroup) -> aSpan, new QueryScorer(theQuery));

        final TopDocs theDocs = indexSearcher.search(theQuery, configuration.getNumberOfSuggestions(), Sort.RELEVANCE);
        for (int i=0;i<theDocs.scoreDocs.length;i++) {
            final Document theDocument = indexSearcher.getIndexReader().document(theDocs.scoreDocs[i].doc);
            final String theOriginalContent = theDocument.getField(aFieldName).stringValue();

            try {
                for (String theFragment : theHighligher.getBestFragments(analyzer, aFieldName, theOriginalContent, 1)) {
                    // Erstes Token ermitteln
                    final int p = theFragment.toLowerCase().indexOf(theTokens.get(0).toLowerCase());
                    if (p>0) {
                        theFragment = theFragment.substring(p).trim();
                    }

                    theResult.add(new Suggestion(highlight(theFragment, theTokens), theFragment));
                }
            } catch (final Exception e) {
                LOGGER.error(e);
            }
        }

        return theResult;
    }

    private String highlight(final String aPhrase, final List<String> aTokens) {
        String theResult = aPhrase;
        final Set<String> theTokens = aTokens.stream().map(String::toLowerCase).collect(Collectors.toSet());

        for (final String theToken : theTokens) {
            final Pattern thePattern = Pattern.compile("(" + theToken+")", Pattern.CASE_INSENSITIVE);
            final Matcher theMatcher = thePattern.matcher(aPhrase);
            final Set<String> theReplacements = new HashSet<>();
            while(theMatcher.find()) {
                theReplacements.add(theMatcher.group());
            }
            for (final String theReplacement : theReplacements) {
                theResult = theResult.replace(theReplacement, "<b>"+theReplacement+"</b>");
            }
        }
        return theResult;
    }

    private String analyze(final String aFieldName, final String aString) throws IOException {
        final TokenStream theTokenStream = analyzer.tokenStream(aFieldName, aString);
        theTokenStream.reset();
        final CharTermAttribute theCharTerms = theTokenStream.getAttribute(CharTermAttribute.class);
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

    private List<String> toTokens(final String aFieldName, final String aPhrase) throws IOException {
        final List<String> theTokens = new ArrayList<>();

        final String[] theSplitTokens = StringUtils.split(aPhrase," ,:;?!.");
        for (int i=0;i<theSplitTokens.length;i++) {
            String theToken = theSplitTokens[i];
            // Mutate the last token to a wildcard
            if (theToken.length() > 2 && i == theSplitTokens.length - 1 && !QueryUtils.isWildCard(theToken)) {
                theToken = theToken + QueryUtils.ASTERISK;
            }
            if (!theToken.startsWith("-")) {
                if (QueryUtils.isWildCard(theToken)) {
                    theTokens.add(theToken);
                } else {
                    final String theAnalyzed = analyze(aFieldName, theToken);
                    if (theAnalyzed != null) {
                        theTokens.add(theAnalyzed);
                    }
                }
            }
        }

        return theTokens;
    }
}
