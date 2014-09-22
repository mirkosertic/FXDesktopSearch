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
import org.apache.lucene.search.*;
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

    public Query parse(String aQuery, String aSearchField) throws IOException {

        QueryTokenizer theTokenizer = new QueryTokenizer(aQuery);

        // Now we have the terms, lets construct the query

        BooleanQuery theResult = new BooleanQuery();

        if (!theTokenizer.getNotRequiredTerms().isEmpty()) {

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

            // This is the original span, so we boost it a lot
            SpanQuery theExactMatchQuery = new SpanNearQuery(theSpans.toArray(new SpanQuery[theSpans.size()]), 0, true);
            theExactMatchQuery.setBoost(61);
            theResult.add(theExactMatchQuery, BooleanClause.Occur.SHOULD);

            // We expect a maximum edit distance of 10 between the searched terms in any order
            // This seems to be the most useful value
            int theMaxEditDistance = 10;
            for (int theSlop=0;theSlop<theMaxEditDistance;theSlop++) {
                SpanQuery theNearQuery = new SpanNearQuery(theSpans.toArray(new SpanQuery[theSpans.size()]), theSlop, false);
                theNearQuery.setBoost(50 + theMaxEditDistance - theSlop);
                theResult.add(theNearQuery, BooleanClause.Occur.SHOULD);
            }

            // Finally, we just add simple term queries, but do not boost them
            // This makes sure that at least the searched terms
            // are found in the document
            for (String theTerm : theTokenizer.getRequiredTerms()) {
                if (QueryUtils.isWildCard(theTerm)) {
                    theResult.add(new WildcardQuery(new Term(aSearchField, toToken(theTerm, aSearchField))),
                            BooleanClause.Occur.MUST);
                } else if (QueryUtils.isFuzzy(theTerm)) {
                    theResult.add(new FuzzyQuery(new Term(aSearchField, theTerm)), BooleanClause.Occur.MUST);
                } else {
                    String theTokenizedTerm = toToken(theTerm, aSearchField);
                    if (!StringUtils.isEmpty(theTokenizedTerm)) {
                        theResult.add(new TermQuery(new Term(aSearchField, theTokenizedTerm)), BooleanClause.Occur.MUST);
                    }
                }
            }
        }

        for (String theTerm : theTokenizer.getNotRequiredTerms()) {
            if (QueryUtils.isWildCard(theTerm)) {
                theResult.add(new WildcardQuery(new Term(aSearchField, theTerm)), BooleanClause.Occur.MUST_NOT);
            } else if (QueryUtils.isFuzzy(theTerm)) {
                theResult.add(new FuzzyQuery(new Term(aSearchField, theTerm)), BooleanClause.Occur.MUST_NOT);
            } else {
                String theTokenizedTerm = toToken(theTerm, aSearchField);
                if (!StringUtils.isEmpty(theTokenizedTerm)) {
                    theResult.add(new TermQuery(new Term(aSearchField, theTokenizedTerm)), BooleanClause.Occur.MUST_NOT);
                }
            }
        }

        return theResult;
    }
}
