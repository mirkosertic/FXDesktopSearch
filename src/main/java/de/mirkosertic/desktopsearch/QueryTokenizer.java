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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryTokenizer {

    private final List<String> requiredTerms;
    private final List<String> notRequiredTerms;

    QueryTokenizer(final String aQuery) {

        requiredTerms = new ArrayList<>();
        notRequiredTerms = new ArrayList<>();

        boolean isFirstChar = true;
        boolean isNegated = false;
        StringBuilder theCurrentTerm = new StringBuilder();

        for (int i = 0; i < aQuery.length(); i++) {
            final char theCurrentChar = Character.toLowerCase(aQuery.charAt(i));
            switch (theCurrentChar) {
                case '-': {
                    if (isFirstChar) {
                        isFirstChar = false;
                        isNegated = true;
                    } else {
                        theCurrentTerm.append(theCurrentChar);
                    }
                    break;
                }
                case '+':
                    if (isFirstChar) {
                        isNegated = false;
                        isFirstChar = false;
                    } else {
                        theCurrentTerm.append(theCurrentChar);
                    }
                    break;
                case ' ': {
                    if (isValid(theCurrentTerm.toString())) {
                        if (isNegated) {
                            notRequiredTerms.add(theCurrentTerm.toString());
                        } else {
                            requiredTerms.add(theCurrentTerm.toString());
                        }
                    }
                    theCurrentTerm = new StringBuilder();
                    isNegated = false;
                    isFirstChar = true;
                    break;
                }
                default: {
                    theCurrentTerm.append(theCurrentChar);
                    isFirstChar = false;
                    break;
                }
            }
        }

        if (isValid(theCurrentTerm.toString())) {
            if (isNegated) {
                notRequiredTerms.add(theCurrentTerm.toString());
            } else {
                requiredTerms.add(theCurrentTerm.toString());
            }
        }
    }

    static boolean isValid(final String aTerm) {
        return !StringUtils.isEmpty(aTerm) && !"*".equals(aTerm) && !"?".equals(aTerm);
    }

    public List<String> getRequiredTerms() {
        return Collections.unmodifiableList(requiredTerms);
    }

    public List<String> getNotRequiredTerms() {
        return Collections.unmodifiableList(notRequiredTerms);
    }
}
