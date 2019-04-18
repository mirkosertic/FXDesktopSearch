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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchPhraseSuggesterComponent extends SearchComponent {

    public static final String ENABLED_PARAM = "fxsuggest.enabled";
    public static final String TOKEN_PARAM = "fxsuggest.q";
    public static final String SLOP_PARAM = "fxsuggest.slop";
    public static final String INORDER_PARAM = "fxsuggest.inorder";
    public static final String NUMBERSUGGEST_PARAM = "fxsuggest.numbersuggest";

    @Override
    public void prepare(final ResponseBuilder aResponseBuilder) {

    }

    @Override
    public void process(final ResponseBuilder aResponseBuilder) throws IOException {
        final var theRequestParams = aResponseBuilder.req.getParams();
        if (!"true".equals(theRequestParams.get(ENABLED_PARAM))) {
            return;
        }

        final int theSlop = Integer.valueOf(theRequestParams.get(SLOP_PARAM));
        final boolean theInOrder = Boolean.valueOf(theRequestParams.get(INORDER_PARAM));
        final int theNumberSuggest = Integer.valueOf(theRequestParams.get(NUMBERSUGGEST_PARAM));

        final IndexSearcher theSearcher = aResponseBuilder.req.getSearcher();
        final var theAnalyzer = aResponseBuilder.req.getSchema().getQueryAnalyzer();

        final var theSuggester = new SearchPhraseSuggester(theSearcher, theAnalyzer,
                new SearchPhraseSuggester.SearchPhraseSuggesterConfiguration() {
                    @Override
                    public int getSuggestionSlop() {
                        return theSlop;
                    }

                    @Override
                    public boolean isSuggestionInOrder() {
                        return theInOrder;
                    }

                    @Override
                    public int getNumberOfSuggestions() {
                        return theNumberSuggest;
                    }
                });

        final var theResponse = new NamedList();

        final var theResult = theSuggester.suggestSearchPhrase(IndexFields.CONTENT, theRequestParams.get(TOKEN_PARAM));
        for (var i = 0; i<theResult.size(); i++) {
            final var theSuggestion = theResult.get(i);
            final Map<String, String> theEntry = new HashMap<>();
            theEntry.put("value", theSuggestion.getValue());
            theEntry.put("label", theSuggestion.getLabel());

            theResponse.add(Integer.toString(i), theEntry);
        }

        aResponseBuilder.rsp.add("fxsuggest", theResponse);
    }

    @Override
    public String getDescription() {
        return "FXDesktopSearch Suggester";
    }
}
