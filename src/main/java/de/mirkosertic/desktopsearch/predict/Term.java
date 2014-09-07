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

    public void buildUsage(Term aTerm, long aRun) {
        TermAssociation theAssociation = associations.get(aTerm);
        if (theAssociation == null) {
            theAssociation = new TermAssociation(1, aRun);
            associations.put(aTerm, theAssociation);
        } else {
            theAssociation.incrementUsageByOne(aRun);
        }
    }

    public Set<Term> getFollowingTerms() {
        return Collections.unmodifiableSet(associations.keySet());
    }

    void computePredictions(RunRestriction aRestriction, List<Prediction> aPredictions, Prediction aPrediction, long aNumberOfTerms) {
        if (aNumberOfTerms<0) {
            return;
        }

        aPredictions.add(aPrediction);

        for (Map.Entry<Term, TermAssociation> theEntry : associations.entrySet()) {
            Term theTerm = theEntry.getKey();
            if (aRestriction.matches(theEntry.getValue().getRuns())) {
                Prediction theClonePrediction = new Prediction(aPrediction);
                theClonePrediction.addTerm(theTerm, theEntry.getValue());
                theTerm.computePredictions(aRestriction, aPredictions, theClonePrediction, aNumberOfTerms - 1);
            }
        }
    }

    @Override
    public String toString() {
        return term;
    }

    Map<Term, TermAssociation> getAssociations() {
        return associations;
    }
}