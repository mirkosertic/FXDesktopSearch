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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
class SearchPhraseSuggester {

    public interface SearchPhraseSuggesterConfiguration {

        int getSuggestionSlop();

        boolean isSuggestionInOrder();

        int getNumberOfSuggestions();
    }

    private final IndexSearcher indexSearcher;
    private final Analyzer analyzer;
    private final SearchPhraseSuggesterConfiguration configuration;

    public SearchPhraseSuggester(
            final IndexSearcher aIndexSearcher, final Analyzer aAnalyzer, final SearchPhraseSuggesterConfiguration aConfiguration) {
        indexSearcher = aIndexSearcher;
        analyzer = aAnalyzer;
        configuration = aConfiguration;
    }

    public List<Suggestion> suggestSearchPhrase(final String aFieldName, final String aPhrase) throws IOException {

        log.info("Trying to find suggestions for phrase {}", aPhrase);

        final var theTokens = toTokens(aFieldName, aPhrase);

        final var theSpanQueries = theTokens.stream().map(s -> {
            if (QueryUtils.isWildCard(s)) {
                var theWildcardQuery = new WildcardQuery(new Term(aFieldName, s));
                var theWrapper = new SpanMultiTermQueryWrapper(theWildcardQuery);
                try {
                    return theWrapper.getRewriteMethod().rewrite(indexSearcher.getIndexReader(), theWildcardQuery);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return new SpanTermQuery(new Term(aFieldName, s));
        }).collect(Collectors.toList());

        final Query theQuery;
        if (theSpanQueries.size() > 1) {
            theQuery = new SpanNearQuery(theSpanQueries.toArray(new SpanQuery[theSpanQueries.size()]), configuration.getSuggestionSlop(), configuration.isSuggestionInOrder());
        } else {
            theQuery = theSpanQueries.get(0);
        }


        log.info("created span query {}", theQuery);

        final var theResult = new ArrayList<Suggestion>();

        final var theHighligher = new Highlighter((aSpan, tokenGroup) -> aSpan, new QueryScorer(theQuery));

        final TopDocs theDocs = indexSearcher.search(theQuery, configuration.getNumberOfSuggestions(), Sort.RELEVANCE);
        for (var i = 0; i<theDocs.scoreDocs.length; i++) {
            final var theDocument = indexSearcher.getIndexReader().document(theDocs.scoreDocs[i].doc);
            final var theOriginalContent = theDocument.getField(aFieldName).stringValue();

            try {
                for (var theFragment : theHighligher.getBestFragments(analyzer, aFieldName, theOriginalContent, 1)) {
                    // Erstes Token ermitteln
                    final var p = theFragment.toLowerCase().indexOf(theTokens.get(0).toLowerCase());
                    if (p>0) {
                        theFragment = theFragment.substring(p).trim();
                    }

                    theResult.add(new Suggestion(highlight(theFragment, theTokens), theFragment));
                }
            } catch (final Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        return theResult;
    }

    private String highlight(final String aPhrase, final List<String> aTokens) {
        var theResult = aPhrase;
        final var theTokens = aTokens.stream().map(String::toLowerCase).collect(Collectors.toSet());

        for (final var theToken : theTokens) {
            final var thePattern = Pattern.compile("(" + theToken+")", Pattern.CASE_INSENSITIVE);
            final var theMatcher = thePattern.matcher(aPhrase);
            final Set<String> theReplacements = new HashSet<>();
            while(theMatcher.find()) {
                theReplacements.add(theMatcher.group());
            }
            for (final var theReplacement : theReplacements) {
                theResult = theResult.replace(theReplacement, "<b>"+theReplacement+"</b>");
            }
        }
        return theResult;
    }

    private String analyze(final String aFieldName, final String aString) throws IOException {
        final var theTokenStream = analyzer.tokenStream(aFieldName, aString);
        theTokenStream.reset();
        final var theCharTerms = theTokenStream.getAttribute(CharTermAttribute.class);
        try {
            if (theTokenStream.incrementToken()) {
                return theCharTerms.toString();
            }
            return null;
        } finally {
            theTokenStream.end();
            theTokenStream.close();
        }
    }

    private List<String> toTokens(final String aFieldName, final String aPhrase) throws IOException {
        final List<String> theTokens = new ArrayList<>();

        final var theSplitTokens = StringUtils.split(aPhrase," ,:;?!.");
        for (var i = 0; i<theSplitTokens.length; i++) {
            var theToken = theSplitTokens[i];
            // Mutate the last token to a wildcard
            if (theToken.length() > 2 && i == theSplitTokens.length - 1 && !QueryUtils.isWildCard(theToken)) {
                theToken = theToken + QueryUtils.ASTERISK;
            }
            if (!theToken.startsWith("-")) {
                if (QueryUtils.isWildCard(theToken)) {
                    theTokens.add(theToken);
                } else {
                    final var theAnalyzed = analyze(aFieldName, theToken);
                    if (theAnalyzed != null) {
                        theTokens.add(theAnalyzed);
                    }
                }
            }
        }

        return theTokens;
    }
}
