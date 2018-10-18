/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2014 Mirko Sertic
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

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SolrServerTest {

    public static void main(final String[] args) throws IOException, SolrServerException {

        final File theTempFile = new File("/tmp" , "test");

        final SolrEmbedded theEmbedded = new SolrEmbedded(new SolrEmbedded.Config(theTempFile));

        final SolrClient server = theEmbedded.solrClient();

        SolrInputDocument theDoc = new SolrInputDocument("id","42L", "content", "this is a test", "language", "en");
        UpdateResponse theResponse = server.add(theDoc);
        System.out.println(theResponse);

        theDoc = new SolrInputDocument("id","43L", "content", "this is a test", "language", "de");
        theResponse = server.add(theDoc);
        System.out.println(theResponse);


        final Map<String, Object> theParams = new HashMap<>();
        theParams.put("q", "content:test");
        theParams.put("facet", "true");
        theParams.put("facet.field", new String[] {"language"});
        theParams.put("hl", "true");
        theParams.put("hl.method", "unified");
        theParams.put("hl.fl", "content");
        theParams.put("hl.snippets", "5");
        theParams.put("hl.fragsize", "100");
        theParams.put("mlt", "true");
        theParams.put("mlt.count", "5");
        theParams.put("mlt.fl", "content");
        final QueryResponse theQueryResponse = server.query(new SearchMapParams(theParams));
        System.out.println(theQueryResponse);
    }
}
