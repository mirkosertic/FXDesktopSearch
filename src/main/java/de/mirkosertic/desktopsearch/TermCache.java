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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TermCache {

    private String lastPrefix;
    private List<Map.Entry<String, Long>> cachedTermFrequencies;
    private boolean valid;

    public TermCache() {
        cachedTermFrequencies = new ArrayList<>();
        valid = false;
    }

    public void invalidate() {
        synchronized (this) {
            valid = false;
        }
    }

    public List<Map.Entry<String, Long>> getFrequenciesFor(String aPrefix, IndexReader aIndexReader) throws IOException {

        synchronized (this) {
            if (lastPrefix != null && !aPrefix.startsWith(lastPrefix)) {
                valid = false;
            }

            if (!valid) {
                Map<String, Long> theTerms = new HashMap<>();
                Terms theLuceneTerms = MultiFields.getTerms(aIndexReader, IndexFields.CONTENT_NOT_STEMMED);
                if (theLuceneTerms != null) {
                    TermsEnum theTermsEnum = theLuceneTerms.iterator(null);
                    BytesRef text;
                    while ((text = theTermsEnum.next()) != null) {
                        String theTermString = text.utf8ToString();
                        if (theTermString.toLowerCase().startsWith(aPrefix)) {
                            long theTotalFrequency = theTermsEnum.totalTermFreq();
                            Long theCount = theTerms.get(theTermString);
                            if (theCount == null) {
                                theTerms.put(theTermString, theTotalFrequency);
                            } else {
                                theTerms.put(theTermString, theCount + theTotalFrequency);
                            }
                        }
                    }
                }
                cachedTermFrequencies = new ArrayList<>(theTerms.entrySet());
                valid = true;
            } else {
                List<Map.Entry<String, Long>> theNewFilter = new ArrayList<>();
                cachedTermFrequencies.stream().filter(e -> e.getKey().toLowerCase().startsWith(aPrefix))
                        .forEach(theNewFilter::add);
                cachedTermFrequencies = theNewFilter;
            }
            lastPrefix = aPrefix;

            return cachedTermFrequencies;
        }
    }
}
