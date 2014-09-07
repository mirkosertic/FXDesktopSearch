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
package de.mirkosertic.desktopsearch.predict;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DocumentTermNGram {

    private final Map<String, Term> knownTerms;

    public DocumentTermNGram() {
        knownTerms = new HashMap<>();
    }

    private Term getOrCreateTerm(String aTerm) {
        Term theTerm = knownTerms.get(aTerm.toLowerCase());
        if (theTerm == null) {
            theTerm = new Term(aTerm);
            knownTerms.put(aTerm, theTerm);
        }
        return theTerm;
    }

    public void build(String aContentString, String aField, Analyzer aAnalyzer) throws IOException {
        TokenStream theTokenStream = aAnalyzer.tokenStream(aField, aContentString);
        theTokenStream.reset();
        CharTermAttribute theCharTerms = theTokenStream.getAttribute(CharTermAttribute.class);
        Term thePreviousTerm = null;
        while(theTokenStream.incrementToken()) {
            String theToken = theCharTerms.toString();

            if (thePreviousTerm == null) {
                thePreviousTerm = getOrCreateTerm(theToken);
            } else {
                Term theNextTerm = getOrCreateTerm(theToken);
                thePreviousTerm.buildUsage(theNextTerm);
                thePreviousTerm = theNextTerm;
            }
        }
        theTokenStream.end();
        theTokenStream.close();

    }

    public Term getTerm(String aTerm) {
        return knownTerms.get(aTerm);
    }

    public Collection<Term> getTerms() {
        return Collections.unmodifiableCollection(knownTerms.values());
    }
}