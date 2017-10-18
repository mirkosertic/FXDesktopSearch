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
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import java.io.File;
import java.io.IOException;

public class SolrServerTest {

    public static void main(String[] args) throws IOException, SolrServerException {

        File theTempFile = new File("/tmp" , "test");

        SolrEmbedded theEmbedded = new SolrEmbedded(new SolrEmbedded.Config(theTempFile));

/*        String theCoreName = "core1";

        CoreContainer coreContainer = new CoreContainer("/home/sertic/Development/Projects/FXDesktopSearch/src/main/resources/solrhome");
        coreContainer.load();

        if (coreContainer.getCore("core1") == null) {
            Map<String, String> coreParameters = new HashMap<>();
            coreContainer.create("core1", coreParameters);
        }*/

        SolrClient server = theEmbedded.solrClient();

        SolrInputDocument theDoc = new SolrInputDocument("id","42L", "content", "this is a test", "language", "en");
        UpdateResponse theResponse = server.add(theDoc);

        System.out.println(theResponse);
    }
}
