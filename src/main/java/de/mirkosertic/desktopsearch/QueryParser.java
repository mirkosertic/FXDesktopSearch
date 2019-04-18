/*
 * FXDesktopSearch Copyright 2013 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.desktopsearch;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class QueryParser {

    private final Analyzer analyzer;

    public QueryParser(final Analyzer aAnalyzer) {
        analyzer = aAnalyzer;
    }

    private String toToken(final String aToken, final String aSearchField) throws IOException {
        try (final var theStream = analyzer.tokenStream(aSearchField, aToken)) {
            final var theAttribute = theStream.getAttribute(CharTermAttribute.class);
            theStream.reset();
            if (theStream.incrementToken()) {
                return theAttribute.toString();
            }
        }
        return "";
    }

    private void addToBooleanQuery(
            final List<String> aTermList, final String aFieldName, final BooleanQuery.Builder aQuery, final BooleanClause.Occur aOccour)
            throws IOException {
        for (final var theTerm : aTermList) {
            if (QueryUtils.isWildCard(theTerm)) {
                aQuery.add(new WildcardQuery(new Term(aFieldName, theTerm)), aOccour);
            } else if (QueryUtils.isFuzzy(theTerm)) {
                aQuery.add(new FuzzyQuery(new Term(aFieldName, theTerm)), aOccour);
            } else {
                final var theTokenizedTerm = toToken(theTerm, aFieldName);
                if (!StringUtils.isEmpty(theTokenizedTerm)) {
                    aQuery.add(new TermQuery(new Term(aFieldName, theTokenizedTerm)), aOccour);
                }
            }
        }

    }

    public Query parse(final String aQuery, final String aSearchField) throws IOException {

        final var theTokenizer = new QueryTokenizer(aQuery);

        // Now we have the terms, lets construct the query

        final var theResult = new BooleanQuery.Builder();

        if (!theTokenizer.getRequiredTerms().isEmpty()) {

            final List<SpanQuery> theSpans = new ArrayList<>();
            for (final var theTerm : theTokenizer.getRequiredTerms()) {
                if (QueryUtils.isWildCard(theTerm)) {
                    theSpans.add(new SpanMultiTermQueryWrapper<>(new WildcardQuery(new Term(aSearchField, theTerm))));
                } else if (QueryUtils.isFuzzy(theTerm)) {
                    theSpans.add(new SpanMultiTermQueryWrapper<>(new FuzzyQuery(new Term(aSearchField, theTerm))));
                } else {
                    // Ok, we need to check of the token would be removed due to stopwords and so on
                    final var theTokenizedTerm = toToken(theTerm, aSearchField);
                    if (!StringUtils.isEmpty(theTokenizedTerm)) {
                        theSpans.add(new SpanTermQuery(new Term(aSearchField, theTokenizedTerm)));
                    }
                }
            }

            if (theSpans.size() > 1) {
                // This is the original span, so we boost it a lot
                final SpanQuery theExactMatchQuery = new SpanNearQuery(theSpans.toArray(new SpanQuery[theSpans.size()]), 0, true);
                theResult.add(new BoostQuery(theExactMatchQuery, 61), BooleanClause.Occur.SHOULD);

                // We expect a maximum edit distance of 10 between the searched terms in any order
                // This seems to be the most useful value
                final var theMaxEditDistance = 10;
                for (var theSlop = 0; theSlop < theMaxEditDistance; theSlop++) {
                    final SpanQuery theNearQuery = new SpanNearQuery(theSpans.toArray(new SpanQuery[theSpans.size()]), theSlop, false);
                    theResult.add(new BoostQuery(theNearQuery, 50 + theMaxEditDistance - theSlop), BooleanClause.Occur.SHOULD);
                }
            }

            // Finally, we just add simple term queries, but do not boost them
            // This makes sure that at least the searched terms
            // are found in the document
            addToBooleanQuery(theTokenizer.getRequiredTerms(), aSearchField, theResult, BooleanClause.Occur.MUST);
        }


        // Finally, add the terms that must not occur in the search result
        addToBooleanQuery(theTokenizer.getNotRequiredTerms(), aSearchField, theResult, BooleanClause.Occur.MUST_NOT);

        return theResult.build();
    }
}