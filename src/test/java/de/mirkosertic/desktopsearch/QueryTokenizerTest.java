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
        final QueryTokenizer theTokenizer = new QueryTokenizer("  a b c +d -e ");
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
        final QueryTokenizer theTokenizer = new QueryTokenizer("test");
        assertEquals(1, theTokenizer.getRequiredTerms().size());
        assertEquals(0, theTokenizer.getNotRequiredTerms().size());
        assertTrue(theTokenizer.getRequiredTerms().contains("test"));
    }

    @Test
    public void testParse3() {
        final QueryTokenizer theTokenizer = new QueryTokenizer("-test");
        assertEquals(0, theTokenizer.getRequiredTerms().size());
        assertEquals(1, theTokenizer.getNotRequiredTerms().size());
        assertTrue(theTokenizer.getNotRequiredTerms().contains("test"));
    }

    @Test
    public void testParse4() {
        final QueryTokenizer theTokenizer = new QueryTokenizer("++test");
        assertEquals(1, theTokenizer.getRequiredTerms().size());
        assertEquals(0, theTokenizer.getNotRequiredTerms().size());
        assertTrue(theTokenizer.getRequiredTerms().contains("+test"));
    }

    @Test
    public void testParse5() {
        final QueryTokenizer theTokenizer = new QueryTokenizer("--test");
        assertEquals(0, theTokenizer.getRequiredTerms().size());
        assertEquals(1, theTokenizer.getNotRequiredTerms().size());
        assertTrue(theTokenizer.getNotRequiredTerms().contains("-test"));
    }
}