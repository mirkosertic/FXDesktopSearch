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

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SolrEmbedded {

    public static class Config {

        private final File solrHome;

        public Config(final File solrHome) {
            this.solrHome = solrHome;
        }
    }

    private final CoreContainer coreContainer;
    private final EmbeddedSolrServer embeddedSolrServer;

    public SolrEmbedded(final Config config) throws IOException {
        // Copy all required files
        final var solrHome = config.solrHome;
        solrHome.mkdirs();

        copyResourceToFile("/solrhome/solr.xml", new File(solrHome, "solr.xml"));

        final var core1 = new File(solrHome, "core1");
        final var core1conf = new File(core1, "conf");
        final var core1data = new File(core1, "data");
        final var core1lang = new File(core1, "lang");

        core1conf.mkdirs();
        core1data.mkdirs();
        core1lang.mkdirs();

        // Core1
        copyResourceToFile("/solrhome/core1/core.properties", new File(core1, "core.properties"));
        copyResourceToFile("/solrhome/core1/currency.xml", new File(core1, "currency.xml"));
        copyResourceToFile("/solrhome/core1/protwords.txt", new File(core1, "protwords.txt"));
        copyResourceToFile("/solrhome/core1/solrconfig.xml", new File(core1, "solrconfig.xml"));
        copyResourceToFile("/solrhome/core1/stopwords.txt", new File(core1, "stopwords.txt"));
        copyResourceToFile("/solrhome/core1/synonyms.txt", new File(core1, "synonyms.txt"));
        copyResourceToFile("/solrhome/core1/update-script.js", new File(core1, "update-script.js"));

        // Core1 Config
        copyResourceToFile("/solrhome/core1/conf/elevate.xml", new File(core1conf, "elevate.xml"));
        copyResourceToFile("/solrhome/core1/conf/managed-schema", new File(core1conf, "managed-schema"));

        // Core1 Language
        copyResourceToFile("/solrhome/core1/lang/stopwords_en.txt", new File(core1lang, "stopwords_en.txt"));

        // Bootstrap
        coreContainer = new CoreContainer(solrHome.toString());
        coreContainer.load();

        embeddedSolrServer = new EmbeddedSolrServer(coreContainer, "core1");
    }

    public SolrClient solrClient() {
        return embeddedSolrServer;
    }

    public void shutdown() throws IOException {
        coreContainer.shutdown();
        embeddedSolrServer.close();
    }

    private static void copyResourceToFile(final String aResource, final File aTargetFile) throws IOException {
        try (final var theFos = new FileOutputStream(aTargetFile)) {
            IOUtils.copy(SolrEmbedded.class.getResourceAsStream(aResource), theFos);
        }
    }
}