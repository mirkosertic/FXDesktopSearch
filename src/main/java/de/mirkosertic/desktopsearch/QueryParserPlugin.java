/*
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
    public void init(NamedList args) {
        super.init(args);
    }

    @Override
    public QParser createParser(String aQueryString, SolrParams aLocalParams, SolrParams aParams, SolrQueryRequest aRequest) {
        return new QParser(aQueryString, aLocalParams, aParams, aRequest) {
            @Override
            public Query parse() throws SyntaxError {
                IndexSchema theSchema = aRequest.getSchema();
                QueryParser theParser = new QueryParser(theSchema.getQueryAnalyzer());
                try {
                    Query theQuery = theParser.parse(aQueryString, IndexFields.CONTENT);
                    return theQuery;
                } catch (Exception e) {
                    throw new SyntaxError(e);
                }
            }
        };
    }
}
