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

import org.junit.Test;

import static org.junit.Assert.*;

public class QueryTokenizerTest {

    @Test
    public void testIsValid() {
        assertTrue(QueryTokenizer.isValid("A"));
        assertFalse(QueryTokenizer.isValid("?"));
        assertFalse(QueryTokenizer.isValid("*"));
        assertFalse(QueryTokenizer.isValid(""));
    }

    @Test
    public void testParse1() {
        final var theTokenizer = new QueryTokenizer("  a b c +d -e ");
        assertEquals(4, theTokenizer.getRequiredTerms().size());
        assertTrue(theTokenizer.getRequiredTerms().contains("a"));
        assertTrue(theTokenizer.getRequiredTerms().contains("b"));
        assertTrue(theTokenizer.getRequiredTerms().contains("c"));
        assertTrue(theTokenizer.getRequiredTerms().contains("d"));
        assertEquals(1, theTokenizer.getNotRequiredTerms().size());
        assertTrue(theTokenizer.getNotRequiredTerms().contains("e"));
    }

    @Test
    public void testParse2() {
        final var theTokenizer = new QueryTokenizer("test");
        assertEquals(1, theTokenizer.getRequiredTerms().size());
        assertEquals(0, theTokenizer.getNotRequiredTerms().size());
        assertTrue(theTokenizer.getRequiredTerms().contains("test"));
    }

    @Test
    public void testParse3() {
        final var theTokenizer = new QueryTokenizer("-test");
        assertEquals(0, theTokenizer.getRequiredTerms().size());
        assertEquals(1, theTokenizer.getNotRequiredTerms().size());
        assertTrue(theTokenizer.getNotRequiredTerms().contains("test"));
    }

    @Test
    public void testParse4() {
        final var theTokenizer = new QueryTokenizer("++test");
        assertEquals(1, theTokenizer.getRequiredTerms().size());
        assertEquals(0, theTokenizer.getNotRequiredTerms().size());
        assertTrue(theTokenizer.getRequiredTerms().contains("+test"));
    }

    @Test
    public void testParse5() {
        final var theTokenizer = new QueryTokenizer("--test");
        assertEquals(0, theTokenizer.getRequiredTerms().size());
        assertEquals(1, theTokenizer.getNotRequiredTerms().size());
        assertTrue(theTokenizer.getNotRequiredTerms().contains("-test"));
    }
}