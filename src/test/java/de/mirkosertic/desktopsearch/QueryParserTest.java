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

import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.search.Query;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class QueryParserTest {

    @Test
    public void testParse() throws IOException {
        final GermanAnalyzer theAnalyzer = new GermanAnalyzer();
        theAnalyzer.setVersion(IndexFields.LUCENE_VERSION);

        final QueryParser theParser = new QueryParser(theAnalyzer);
        final Query theQuery = theParser.parse("der a +b -c dudel* ~nudel -~yahoo -*wildcard hello","field");

        assertEquals("(spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 0, true))^61.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 0, false))^60.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 1, false))^59.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 2, false))^58.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 3, false))^57.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 4, false))^56.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 5, false))^55.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 6, false))^54.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 7, false))^53.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 8, false))^52.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 9, false))^51.0 +field:a +field:b +field:dudel* +field:~nudel~2 +field:hello -field:c -field:~yahoo~2 -field:*wildcard", theQuery.toString());
    }
}