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

import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.search.Query;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class QueryParserTest {

    @Test
    public void testParse() throws IOException {
        final var theAnalyzer = new GermanAnalyzer();
        theAnalyzer.setVersion(IndexFields.LUCENE_VERSION);

        final var theParser = new QueryParser(theAnalyzer);
        final var theQuery = theParser.parse("der a +b -c dudel* ~nudel -~yahoo -*wildcard hello","field");

        assertEquals("(spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 0, true))^61.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 0, false))^60.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 1, false))^59.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 2, false))^58.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 3, false))^57.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 4, false))^56.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 5, false))^55.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 6, false))^54.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 7, false))^53.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 8, false))^52.0 (spanNear([field:a, field:b, SpanMultiTermQueryWrapper(field:dudel*), SpanMultiTermQueryWrapper(field:~nudel~2), field:hello], 9, false))^51.0 +field:a +field:b +field:dudel* +field:~nudel~2 +field:hello -field:c -field:~yahoo~2 -field:*wildcard", theQuery.toString());
    }
}