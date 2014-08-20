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
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

import java.util.StringTokenizer;

class QueryParser {

    private void addSubQuery(BooleanQuery aQuery, String aTerm, boolean aNegated, String aSearchField) {
        if (!StringUtils.isEmpty(aTerm)) {
            if (!aTerm.contains("*")) {
                if (aTerm.contains(" ")) {
                    PhraseQuery thePhraseQuery = new PhraseQuery();
                    for (StringTokenizer theTokenizer = new StringTokenizer(aTerm, " "); theTokenizer.hasMoreTokens();) {
                        String theToken = theTokenizer.nextToken();
                        thePhraseQuery.add(new Term(aSearchField, theToken));
                    }
                    thePhraseQuery.setSlop(1);
                    if (aNegated) {
                        aQuery.add(thePhraseQuery, BooleanClause.Occur.MUST_NOT);
                    } else {
                        aQuery.add(thePhraseQuery, BooleanClause.Occur.MUST);
                    }
                } else {
                    Query theQuery;
                    if (!aTerm.endsWith("~")) {
                        theQuery = new TermQuery(new Term(aSearchField, aTerm));
                    } else {
                        theQuery = new FuzzyQuery(new Term(aSearchField, aTerm.substring(0, aTerm.length() - 1)));
                    }
                    if (aNegated) {
                        aQuery.add(theQuery, BooleanClause.Occur.MUST_NOT);
                    } else {
                        aQuery.add(theQuery, BooleanClause.Occur.MUST);
                    }
                }
            } else {
                WildcardQuery theWildcardQuery = new WildcardQuery(new Term(aSearchField, aTerm));
                if (aNegated) {
                    aQuery.add(theWildcardQuery, BooleanClause.Occur.MUST_NOT);
                } else {
                    aQuery.add(theWildcardQuery, BooleanClause.Occur.MUST);
                }
            }
        }
    }

    public Query parse(String aQuery, String aSearchField) {

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
