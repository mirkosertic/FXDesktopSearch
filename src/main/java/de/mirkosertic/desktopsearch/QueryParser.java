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

    private boolean isWildCard(String aTerm) {
        return aTerm.contains("*") || aTerm.contains("?");
    }

    private boolean isFuzzy(String aTerm) {
        return aTerm.startsWith("~") && aTerm.length() > 1;
    }

    private boolean isValid(String aTerm) {
        return !StringUtils.isEmpty(aTerm) && !"*".equals(aTerm) && !"?".equals(aTerm);
    }

    private void addSubQuery(BooleanQuery aQuery, String aTerm, boolean aNegated, String aSearchField) throws IOException {
        if (!StringUtils.isEmpty(aTerm)) {
            if (aTerm.contains(" ")) {
                // Seems to be a phrase query
                List<SpanQuery> theQueries = new ArrayList<>();
                String[] theQueryTerms = StringUtils.split(aTerm," ");
                for (String thePhraseTerm : theQueryTerms) {
                    thePhraseTerm = toToken(StringUtils.strip(thePhraseTerm), aSearchField);
                    if (isValid(thePhraseTerm)) {
                        SpanQuery theQuery;
                        if (isWildCard(thePhraseTerm)) {
                            theQuery = new SpanMultiTermQueryWrapper<>(new WildcardQuery(new Term(aSearchField, thePhraseTerm)));
                        } else if (isFuzzy(thePhraseTerm)) {
                            theQuery = new SpanMultiTermQueryWrapper<>(new FuzzyQuery(new Term(aSearchField, thePhraseTerm.substring(1))));
                        } else {
                            theQuery = new SpanTermQuery(new Term(aSearchField, thePhraseTerm));
                        }
                        theQueries.add(theQuery);
                    }
                }
                if (!theQueries.isEmpty()) {
                    SpanQuery theSpanQuery = new SpanNearQuery(theQueries.toArray(new SpanQuery[theQueries.size()]), 1, true);
                    theSpanQuery.setBoost(theQueryTerms.length * 5);
                    if (aNegated) {
                        aQuery.add(theSpanQuery, BooleanClause.Occur.MUST_NOT);
                    } else {
                        aQuery.add(theSpanQuery, BooleanClause.Occur.MUST);
                    }
                }

                int theMaxRequired = Math.min(theQueries.size(), 7);
                for (int i = 2;i <= theMaxRequired; i++) {
                    BooleanQuery theQuery = new BooleanQuery();
                    for (String theTerm : theQueryTerms) {
                        theTerm = toToken(StringUtils.strip(theTerm), aSearchField);
                        theQuery.add(new TermQuery(new Term(aSearchField, theTerm)), BooleanClause.Occur.SHOULD);
                    }
                    theQuery.setBoost(i * 3);
                    theQuery.setMinimumNumberShouldMatch(i);
                    if (aNegated) {
                        aQuery.add(theQuery, BooleanClause.Occur.MUST_NOT);
                    } else {
                        aQuery.add(theQuery, BooleanClause.Occur.SHOULD);
                    }
                }

                for (String theTerm : theQueryTerms) {
                    theTerm = toToken(StringUtils.strip(theTerm), aSearchField);
                    aQuery.add(new TermQuery(new Term(aSearchField, theTerm)), BooleanClause.Occur.SHOULD);
                }

            } else {
                if (isValid(aTerm)) {
                    // Single term
                    Query theQuery;
                    if (isWildCard(aTerm)) {
                        theQuery = new WildcardQuery(new Term(aSearchField, toToken(aTerm, aSearchField)));
                    } else if (isFuzzy(aTerm)) {
                        theQuery = new FuzzyQuery(new Term(aSearchField, toToken(aTerm.substring(1), aSearchField)));
                    } else {
                        theQuery = new TermQuery(new Term(aSearchField, toToken(aTerm, aSearchField)));
                    }

                    if (aNegated) {
                        aQuery.add(theQuery, BooleanClause.Occur.MUST_NOT);
                    } else {
                        aQuery.add(theQuery, BooleanClause.Occur.MUST);
                    }
                }
            }
        }
    }

    public Query parse(String aQuery, String aSearchField) throws IOException {

        BooleanQuery theResult = new BooleanQuery();

        boolean isStringMode = false;
        boolean isNegated = false;
        StringBuilder theCurrentTerm = new StringBuilder();

        for (int i = 0; i < aQuery.length(); i++) {
            char theCurrentChar = Character.toLowerCase(aQuery.charAt(i));
            if (theCurrentChar == '\"') {
                isStringMode = !isStringMode;
            } else {
                if (!isStringMode) {
                    switch (theCurrentChar) {
                    case '-': {
                        if (theCurrentTerm.length() == 0) {
                            isNegated = true;
                        } else {
                            theCurrentTerm.append(theCurrentChar);
                        }
                        break;
                    }
                    case '+':
                        if (theCurrentTerm.length() == 0) {
                            isNegated = false;
                        } else {
                            theCurrentTerm.append(theCurrentChar);
                        }
                        break;
                    case ' ': {
                        addSubQuery(theResult, theCurrentTerm.toString(), isNegated, aSearchField);
                        theCurrentTerm = new StringBuilder();
                        isNegated = false;
                        break;
                    }
                    default: {
                        theCurrentTerm.append(theCurrentChar);
                        break;
                    }
                    }
                } else {
                    theCurrentTerm.append(theCurrentChar);
                }
            }
        }

        if (theCurrentTerm.length() > 0) {
            addSubQuery(theResult, theCurrentTerm.toString(), isNegated, aSearchField);
        }

        return theResult;
    }
}
