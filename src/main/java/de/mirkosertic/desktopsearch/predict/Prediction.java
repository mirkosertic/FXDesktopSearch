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

import java.util.ArrayList;
import java.util.List;

class Prediction implements Comparable<Prediction> {

    private final List<Term> terms;
    private long score;

    Prediction(Term aRootTerm, long aUsages) {
        terms =new ArrayList<>();
        terms.add(aRootTerm);
        score = aUsages;
    }

    Prediction(Prediction aPrediction) {
        terms = new ArrayList<>(aPrediction.terms);
        score = aPrediction.score;
    }

    @Override
    public int compareTo(Prediction aOtherPrediction) {
        if (aOtherPrediction.score < score) {
            return -1;
        }
        if (aOtherPrediction.score > score) {
            return 1;
        }
        return 0;
    }

    public void addTerm(Term aTerm, TermAssociation aAssociation) {
        terms.add(aTerm);
        score = score + aAssociation.usages();
    }
}
