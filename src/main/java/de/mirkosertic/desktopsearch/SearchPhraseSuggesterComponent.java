/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2017 Mirko Sertic
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
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
        final SolrParams theRequestParams = aResponseBuilder.req.getParams();
        if (!"true".equals(theRequestParams.get(ENABLED_PARAM))) {
            return;
        }

        final int theSlop = Integer.valueOf(theRequestParams.get(SLOP_PARAM));
        final boolean theInOrder = Boolean.valueOf(theRequestParams.get(INORDER_PARAM));
        final int theNumberSuggest = Integer.valueOf(theRequestParams.get(NUMBERSUGGEST_PARAM));

        final IndexSearcher theSearcher = aResponseBuilder.req.getSearcher();
        final Analyzer theAnalyzer = aResponseBuilder.req.getSchema().getQueryAnalyzer();

        final SearchPhraseSuggester theSuggester = new SearchPhraseSuggester(theSearcher, theAnalyzer,
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

        final NamedList theResponse = new NamedList();

        final List<Suggestion> theResult = theSuggester.suggestSearchPhrase(IndexFields.CONTENT, theRequestParams.get(TOKEN_PARAM));
        for (int i=0;i<theResult.size();i++) {
            final Suggestion theSuggestion = theResult.get(i);
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
