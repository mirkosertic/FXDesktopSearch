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

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.janino.util.Producer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Slf4j
public abstract class NLP {

    private static final NLP DONOTHING = new NLP() {
        @Override
        public void addMetaDataTo(final String aStringData, final Content aContent) {
        }
    };

    private static class StanfordNLP extends NLP {
        private final StanfordCoreNLP nlp;
        private final EntityBlacklist blacklist;

        public StanfordNLP(final StanfordCoreNLP nlp, final EntityBlacklist blacklist) {
            this.nlp = nlp;
            this.blacklist = blacklist;
        }

        @Override
        public void addMetaDataTo(final String aStringData, final Content aContent) {
            log.info("Annotating document");
            final CoreDocument doc = new CoreDocument(aStringData);
            nlp.annotate(doc);
            log.info("Done with annotating, timing = {}", nlp.timingInformation());

            if (doc.entityMentions() != null) {
                final Map<String, Set<String>> entityMentions = new HashMap<>();
                for (final CoreEntityMention em : doc.entityMentions()) {
                    if (!blacklist.isBlacklisted(em.text())) {
                        final Set<String> mentions = entityMentions.computeIfAbsent(em.entityType(), k -> new HashSet<>());
                        mentions.add(em.text());
                    }
                }

                for (final Map.Entry<String, Set<String>> entry : entityMentions.entrySet()) {
                    aContent.addMetaData("entity_" + entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
        }
    }

    private static final Map<SupportedLanguage, StanfordCoreNLP> PIPELINES = new HashMap<>();

    private synchronized static StanfordCoreNLP cachedPipeLine(final SupportedLanguage aLanguage, final Producer<StanfordCoreNLP> aCreator) {
        StanfordCoreNLP nlp = PIPELINES.get(aLanguage);
        if (nlp == null) {
            log.info("No cached pipeline for {}", aLanguage);
            nlp = aCreator.produce();
            PIPELINES.put(aLanguage, nlp);
            log.info("Pipeline created!");
        }
        return nlp;
    }

    private static final Map<SupportedLanguage, EntityBlacklist> BLACKLISTS = new HashMap<>();

    private static synchronized EntityBlacklist cachedBlacklist(final SupportedLanguage aLanguage) {
        EntityBlacklist list = BLACKLISTS.get(aLanguage);
        if (list == null) {
            switch (aLanguage) {
                case de:
                    list = new EntityBlacklist(NLP.class.getResourceAsStream("/entity-blacklist-de.txt"));
                    break;
                case en:
                    list = new EntityBlacklist(NLP.class.getResourceAsStream("/entity-blacklist-en.txt"));
                    break;
                default:
                    throw new IllegalArgumentException("Not supported : ! " + aLanguage);
            }
            BLACKLISTS.put(aLanguage, list);
        }
        return list;
    }

    public static NLP forLanguage(final SupportedLanguage aLanguage) {
        switch (aLanguage) {
            case en:
                return new StanfordNLP(cachedPipeLine(aLanguage, () -> {
                    log.info("Ceating new English NLP Pipeline");
                    final Properties props = new Properties();
                    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
                    props.setProperty("ner.useSUTime", "false");
                    props.setProperty("ner.applyFineGrained", "false");
                    return new StanfordCoreNLP(props);
                }), cachedBlacklist(aLanguage));
            case de:
                return new StanfordNLP(cachedPipeLine(aLanguage, () -> {
                    try {
                        log.info("Ceating new German NLP Pipeline");
                        final Properties props = new Properties();
                        props.load(NLP.class.getResourceAsStream("/StanfordCoreNLP-german.properties"));
                        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
                        props.setProperty("ner.useSUTime", "false");
                        props.setProperty("ner.applyFineGrained", "false");
                        return new StanfordCoreNLP(props);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }), cachedBlacklist(aLanguage));
            default:
                return DONOTHING;
        }
    }

    public abstract void addMetaDataTo(String aStringData, Content aContent);
}
