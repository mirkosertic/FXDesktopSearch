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
import java.util.stream.Collectors;

public class RunRestriction {

    private final List<Term> terms;
    private Set<Long> runs;

    public RunRestriction(Term aRootTerm) {
        terms = new ArrayList<>();
        terms.add(aRootTerm);
        runs = new HashSet<>();
    }

    private Term lastTerm() {
        return terms.get(terms.size() - 1);
    }

    public void addTransitionTo(Term aTerm) {
        for (Map.Entry<Term, TermAssociation> theTransition : lastTerm().getAssociations().entrySet()) {
            if (terms.size() == 1) {
                runs = theTransition.getValue().getRuns();
            } else {
                runs.retainAll(theTransition.getValue().getRuns());
            }
            terms.add(aTerm);
            return;
        }
        throw new IllegalArgumentException("No transition found to " + aTerm);
    }

    public Set<Long> getRuns() {
        return Collections.unmodifiableSet(runs);
    }

    public boolean matches(Set<Long> aRuns) {
        for (Long theLong : runs) {
            if (aRuns.contains(theLong)) {
                return true;
            }
        }
        return runs.isEmpty();
    }

    public List<Prediction> predict(int aNumberOfTerms, int aNumberOfPredictions) {
        List<Prediction> thePredictions = new ArrayList<>();
        for (Map.Entry<Term, TermAssociation> theEntry : lastTerm().getAssociations().entrySet()) {
            Term theTerm = theEntry.getKey();
            if (matches(theEntry.getValue().getRuns())) {
                Prediction thePrediction = new Prediction(theTerm, theEntry.getValue().usages());
                theTerm.computePredictions(this, thePredictions, thePrediction, aNumberOfTerms - 1);
            }
        }

        return Collections.unmodifiableList(thePredictions.stream().filter(p -> p.getTerms().size() >= aNumberOfTerms).sorted().limit(aNumberOfPredictions).collect(Collectors.toList()));
    }

}
