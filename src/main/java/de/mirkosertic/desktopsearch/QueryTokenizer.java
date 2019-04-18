/*
 * FXDesktopSearch Copyright 2013 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

        var isFirstChar = true;
        var isNegated = false;
        var theCurrentTerm = new StringBuilder();

        for (var i = 0; i < aQuery.length(); i++) {
            final var theCurrentChar = Character.toLowerCase(aQuery.charAt(i));
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
