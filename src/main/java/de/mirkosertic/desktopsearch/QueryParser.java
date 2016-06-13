/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2013 Mirko Sertic
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses/>.
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

    public QueryParser(Analyzer aAnalyzer) {
        analyzer = aAnalyzer;
    }

    private String toToken(String aToken, String aSearchField) throws IOException {
        try (TokenStream theStream = analyzer.tokenStream(aSearchField, aToken)) {
            CharTermAttribute theAttribute = theStream.getAttribute(CharTermAttribute.class);
            theStream.reset();
            if (theStream.incrementToken()) {
                return theAttribute.toString();
            }
        }
        return "";
    }

    private void addToBooleanQuery(List<String> aTermList, String aFieldName, BooleanQuery.Builder aQuery, BooleanClause.Occur aOccour)
            throws IOException {
        for (String theTerm : aTermList) {
            if (QueryUtils.isWildCard(theTerm)) {
                aQuery.add(new WildcardQuery(new Term(aFieldName, theTerm)), aOccour);
            } else if (QueryUtils.isFuzzy(theTerm)) {
                aQuery.add(new FuzzyQuery(new Term(aFieldName, theTerm)), aOccour);
            } else {
                String theTokenizedTerm = toToken(theTerm, aFieldName);
                if (!StringUtils.isEmpty(theTokenizedTerm)) {
                    aQuery.add(new TermQuery(new Term(aFieldName, theTokenizedTerm)), aOccour);
                }
            }
        }

    }

    public Query parse(String aQuery, String aSearchField) throws IOException {

        QueryTokenizer theTokenizer = new QueryTokenizer(aQuery);

        // Now we have the terms, lets construct the query

        BooleanQuery.Builder theResult = new BooleanQuery.Builder();

        if (!theTokenizer.getRequiredTerms().isEmpty()) {

            List<SpanQuery> theSpans = new ArrayList<>();
            for (String theTerm : theTokenizer.getRequiredTerms()) {
                if (QueryUtils.isWildCard(theTerm)) {
                    theSpans.add(new SpanMultiTermQueryWrapper<>(new WildcardQuery(new Term(aSearchField, theTerm))));
                } else if (QueryUtils.isFuzzy(theTerm)) {
                    theSpans.add(new SpanMultiTermQueryWrapper<>(new FuzzyQuery(new Term(aSearchField, theTerm))));
                } else {
                    // Ok, we need to check of the token would be removed due to stopwords and so on
                    String theTokenizedTerm = toToken(theTerm, aSearchField);
                    if (!StringUtils.isEmpty(theTokenizedTerm)) {
                        theSpans.add(new SpanTermQuery(new Term(aSearchField, theTokenizedTerm)));
                    }
                }
            }

            if (theSpans.size() > 1) {
                // This is the original span, so we boost it a lot
                SpanQuery theExactMatchQuery = new SpanNearQuery(theSpans.toArray(new SpanQuery[theSpans.size()]), 0, true);
                theResult.add(new BoostQuery(theExactMatchQuery, 61), BooleanClause.Occur.SHOULD);

                // We expect a maximum edit distance of 10 between the searched terms in any order
                // This seems to be the most useful value
                int theMaxEditDistance = 10;
                for (int theSlop = 0; theSlop < theMaxEditDistance; theSlop++) {
                    SpanQuery theNearQuery = new SpanNearQuery(theSpans.toArray(new SpanQuery[theSpans.size()]), theSlop, false);
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