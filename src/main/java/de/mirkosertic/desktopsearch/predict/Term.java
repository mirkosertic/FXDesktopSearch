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

import java.util.*;

public class Term {

    private final String term;
    private final Map<Term, TermAssociation> associations;

    public Term(String aTerm) {
        term = aTerm;
        associations = new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Term term1 = (Term) o;

        if (!term.equalsIgnoreCase(term1.term)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return term.toLowerCase().hashCode();
    }

    public void buildUsage(Term aTerm) {
        TermAssociation theAssociation = associations.get(aTerm);
        if (theAssociation == null) {
            theAssociation = new TermAssociation(1);
            associations.put(aTerm, theAssociation);
        } else {
            theAssociation.incrementUsageByOne();
        }
    }

    public Set<Term> getFollowingTerms() {
        return Collections.unmodifiableSet(associations.keySet());
    }

    private List<Map.Entry<Term, TermAssociation>> getTopNAssociations(int aN) {
        List<Map.Entry<Term, TermAssociation>> theResult = new ArrayList<>(associations.entrySet());
        Collections.sort(theResult, (Map.Entry<Term, TermAssociation> aO1, Map.Entry<Term, TermAssociation> aO2) -> {
            if (aO1.getValue().usages() < aO2.getValue().usages()) {
                return -1;
            }
            if (aO1.getValue().usages() > aO2.getValue().usages()) {
                return 1;
            }
            return 0;
        });
        while (theResult.size() > aN) {
            theResult.remove(theResult.size() - 1);
        }
        return theResult;
    }

    private void computePredictions(List<Prediction> aPredictions, Prediction aPrediction, long aNumberOfTerms, int aTopN) {
        if (aNumberOfTerms<1) {
            return;
        }

        aPredictions.add(aPrediction);

        for (Map.Entry<Term, TermAssociation> theEntry : getTopNAssociations(aTopN)) {
            Term theTerm = theEntry.getKey();

            Prediction theClonePrediction = new Prediction(aPrediction);
            theClonePrediction.addTerm(theTerm, theEntry.getValue());
            theTerm.computePredictions(aPredictions, theClonePrediction, aNumberOfTerms - 1, aTopN);

            aPredictions.add(theClonePrediction);
        }
    }

    public List<Prediction> predict(int aNumberOfTerms, int aNumberOfPredictions) {
        int theTopN = 10;
        List<Prediction> thePredictions = new ArrayList<>();
        for (Map.Entry<Term, TermAssociation> theEntry : getTopNAssociations(theTopN)) {
            Term theTerm = theEntry.getKey();
            Prediction thePrediction = new Prediction(theTerm, theEntry.getValue().usages());
            theTerm.computePredictions(thePredictions, thePrediction, aNumberOfTerms - 1, theTopN);
        }

        Collections.sort(thePredictions);
        while(thePredictions.size() > aNumberOfPredictions) {
            thePredictions.remove(thePredictions.size() - 1);
        }

        return Collections.unmodifiableList(thePredictions);
    }
}