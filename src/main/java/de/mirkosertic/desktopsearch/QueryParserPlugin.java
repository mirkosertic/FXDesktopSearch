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

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;

public class QueryParserPlugin extends QParserPlugin {

    @Override
    public void init(final NamedList args) {
        super.init(args);
    }

    @Override
    public QParser createParser(
            final String aQueryString, final SolrParams aLocalParams, final SolrParams aParams, final SolrQueryRequest aRequest) {
        return new QParser(aQueryString, aLocalParams, aParams, aRequest) {
            @Override
            public Query parse() throws SyntaxError {
                final var theSchema = aRequest.getSchema();
                final var theParser = new QueryParser(theSchema.getQueryAnalyzer());
                try {
                    final var theQuery = theParser.parse(aQueryString, IndexFields.CONTENT);
                    return theQuery;
                } catch (final Exception e) {
                    throw new SyntaxError(e);
                }
            }
        };
    }
}
