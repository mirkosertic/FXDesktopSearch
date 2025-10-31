package de.mirkosertic.desktopsearch;

import org.apache.lucene.queries.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.queries.spans.SpanNearQuery;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import java.util.List;

public class QueryParserTest {

    @Test
    public void testPhraseQuery() throws QueryNodeException {
        final SpanNearQuery.Builder builder = new SpanNearQuery.Builder(IndexFields.CONTENT, true);
        builder.setSlop(3);
        builder.addClause(new SpanMultiTermQueryWrapper<>(new TermInSetQuery(IndexFields.CONTENT, List.of(new BytesRef("domain")))));
        builder.addClause(new SpanMultiTermQueryWrapper<>(new TermInSetQuery(IndexFields.CONTENT, List.of(new BytesRef("driven")))));
        builder.addClause(new SpanMultiTermQueryWrapper<>(new TermInSetQuery(IndexFields.CONTENT, List.of(new BytesRef("design")))));
        final Query query = builder.build();
        System.out.println(query);
    }
}
