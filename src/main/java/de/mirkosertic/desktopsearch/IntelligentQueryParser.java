package de.mirkosertic.desktopsearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.queries.spans.SpanNearQuery;
import org.apache.lucene.queries.spans.SpanTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spell.DirectSpellChecker;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.search.spell.SuggestWord;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntelligentQueryParser {

    private final Analyzer analyzer;
    private final DirectSpellChecker directSpellChecker;

    public IntelligentQueryParser(final Analyzer analyzer) {
        this.analyzer = analyzer;
        this.directSpellChecker = new DirectSpellChecker();
        this.directSpellChecker.setMaxEdits(2);
        this.directSpellChecker.setMinPrefix(1);
        this.directSpellChecker.setMaxInspections(5);
        this.directSpellChecker.setMinQueryLength(4);
    }

    private Set<String> findTermsByPrefix(final IndexReader indexReader, final String fieldName, final String prefixStr) throws IOException {
        if (prefixStr.length() >= 4) {
            final Terms terms = MultiTerms.getTerms(indexReader, fieldName);
            if (terms != null) {
                final TermsEnum termsEnum = terms.iterator();
                final BytesRef prefix = new BytesRef(prefixStr);

                final Set<String> result = new HashSet<>();
                if (termsEnum.seekCeil(prefix) != TermsEnum.SeekStatus.END) {
                    BytesRef term;
                    while ((term = termsEnum.next()) != null && result.size() < 10) {
                        final String termStr = term.utf8ToString();
                        if (!termStr.startsWith(prefixStr)) {
                            break;
                        }
                        result.add(termStr);
                    }

                    return result;
                }
            }
        }
        return Collections.emptySet();
    }

    public Query parse(final String searchField, final String query, final IndexReader indexReader, final int slop) throws IOException {
        try (final var stream = analyzer.tokenStream(searchField, query)) {
            final var theAttribute = stream.getAttribute(CharTermAttribute.class);
            stream.reset();

            // TODO: Correctly handle PositionIncrementAttribute here....

            final List<Set<String>> tokens = new ArrayList<>();
            while (stream.incrementToken()) {
                final String token = theAttribute.toString();
                // Try to find spell corrections for this token
                final SuggestWord[] corrections = directSpellChecker.suggestSimilar(new Term(searchField, token), 5, indexReader, SuggestMode.SUGGEST_ALWAYS);
                if (corrections.length > 0) {
                    final Set<String> alternatives = new HashSet<>();
                    if (indexReader.docFreq(new Term(searchField, token)) > 0) {
                        // The original token seems to exist, so we add it
                        alternatives.add(token);
                    }
                    for (final SuggestWord correction : corrections) {
                        alternatives.add(correction.string);
                    }
                    alternatives.addAll(findTermsByPrefix(indexReader, searchField, token));
                    tokens.add(alternatives);
                } else {
                    // No alternatives, but we truly do not know if this term exists or not, so we have to check the doc frequencies
                    if (indexReader.docFreq(new Term(searchField, token)) > 0) {
                        // Term exist
                        final Set<String> result = new HashSet<>();
                        result.add(token);
                        result.addAll(findTermsByPrefix(indexReader, searchField, token));
                        tokens.add(result);
                    } else {
                        final Set<String> result = findTermsByPrefix(indexReader, searchField, token);
                        if (!result.isEmpty()) {
                            tokens.add(result);
                        }
                    }
                }
            }

            if (tokens.isEmpty()) {
                throw new IllegalArgumentException("Cannot parse query, no tokens or alternatives found : '" + query + "'");
            }

            // One token, special case
            if (tokens.size() == 1) {
                // One single token found
                final Set<String> token = tokens.getFirst();
                if (token.size() == 1) {
                    return new TermQuery(new Term(searchField, token.iterator().next()));
                }
                // Multiple alternatives found
                final List<BytesRef> terms = new ArrayList<>();
                for (final String t : token) {
                    terms.add(new BytesRef(t));
                }
                return new TermInSetQuery(searchField, terms);
            }

            // TODO: At this point we truly do not know if one of the positions is optional or not
            // So we create a span query and assume all positions are required
            // However it would make sense to generate a boolean expression here, with optional
            // variants of the generated queries

            // We have a minimum of two tokens
            SpanNearQuery.Builder queryBuilder = SpanNearQuery.newOrderedNearQuery(searchField).setSlop(slop);
            for (final Set<String> token : tokens) {
                if (token.size() == 1) {
                    queryBuilder = queryBuilder.addClause(new SpanTermQuery(new Term(searchField, token.iterator().next())));
                } else {
                    // Multiple alternatives found
                    final List<BytesRef> terms = new ArrayList<>();
                    for (final String t : token) {
                        terms.add(new BytesRef(t));
                    }
                    final TermInSetQuery termInSetQuery = new TermInSetQuery(searchField, terms);
                    queryBuilder = queryBuilder.addClause(new SpanMultiTermQueryWrapper<>(termInSetQuery));
                }
            }
            return queryBuilder.build();
        }
    }
}
