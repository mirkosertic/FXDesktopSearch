/**
 * FreeDesktopSearch - A Search Engine for your Desktop
 * Copyright (C) 2013 Mirko Sertic
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
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.ckb.SoraniAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.da.DanishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.eu.BasqueAnalyzer;
import org.apache.lucene.analysis.fa.PersianAnalyzer;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.ga.IrishAnalyzer;
import org.apache.lucene.analysis.gl.GalicianAnalyzer;
import org.apache.lucene.analysis.hi.HindiAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.hy.ArmenianAnalyzer;
import org.apache.lucene.analysis.id.IndonesianAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.lv.LatvianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AnalyzerCache {

    private static final String FIELD_PREFIX = IndexFields.CONTENT + "_";

    private final Map<SupportedLanguage, Analyzer> analyzerByLanguage;
    private final Analyzer standardAnalyzer;

    public AnalyzerCache(Configuration aConfiguration) {
        standardAnalyzer = new StandardAnalyzer(IndexFields.LUCENE_VERSION);
        analyzerByLanguage = new HashMap<>();

        registerIfEnabled(SupportedLanguage.ar, aConfiguration, new ArabicAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.bg, aConfiguration, new BulgarianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.br, aConfiguration, new BrazilianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.ca, aConfiguration, new CatalanAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.ckb, aConfiguration, new SoraniAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.cz, aConfiguration, new CzechAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.da, aConfiguration, new DanishAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.de, aConfiguration, new GermanAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.el, aConfiguration, new GreekAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.en, aConfiguration, new EnglishAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.es, aConfiguration, new SpanishAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.eu, aConfiguration, new BasqueAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.fa, aConfiguration, new PersianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.fi, aConfiguration, new FinnishAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.fr, aConfiguration, new FrenchAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.ga, aConfiguration, new IrishAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.gl, aConfiguration, new GalicianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.hi, aConfiguration, new HindiAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.hu, aConfiguration, new HungarianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.hy, aConfiguration, new ArmenianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.id, aConfiguration, new IndonesianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.it, aConfiguration, new ItalianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.lv, aConfiguration, new LatvianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.nl, aConfiguration, new DutchAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.no, aConfiguration, new NorwegianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.pt, aConfiguration, new PortugueseAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.ro, aConfiguration, new RomanianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.ru, aConfiguration, new RussianAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.sv, aConfiguration, new SwedishAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.th, aConfiguration, new ThaiAnalyzer(IndexFields.LUCENE_VERSION));
        registerIfEnabled(SupportedLanguage.tr, aConfiguration, new TurkishAnalyzer(IndexFields.LUCENE_VERSION));
    }

    private void registerIfEnabled(SupportedLanguage aLanguage, Configuration aConfiguration, Analyzer aAnalyzer) {
        if (aConfiguration.getEnabledLanguages().contains(aLanguage)) {
            analyzerByLanguage.put(aLanguage, aAnalyzer);
        }
    }

    public String getFieldNameFor(SupportedLanguage aLanguage) {
        return FIELD_PREFIX + aLanguage.name();
    }

    public Analyzer getAnalyzer() {
        Map<String, Analyzer> theFieldAnalyzer = new HashMap<>();
        analyzerByLanguage.entrySet().stream().forEach(e -> theFieldAnalyzer.put(getFieldNameFor(e.getKey()), e.getValue()));
        return new PerFieldAnalyzerWrapper(standardAnalyzer, theFieldAnalyzer);
    }

    public boolean supportsLanguage(SupportedLanguage aLanguage) {
        return analyzerByLanguage.containsKey(aLanguage);
    }

    public String[] getAllFieldNames() {
        List<String> theFieldNames = new ArrayList<>();
        theFieldNames.add(IndexFields.CONTENT);
        analyzerByLanguage.entrySet().stream().forEach(e -> theFieldNames.add(getFieldNameFor(e.getKey())));
        return theFieldNames.toArray(new String[theFieldNames.size()]);
    }

    public SupportedLanguage getLanguageFromFieldName(String aField) {
        if (aField.startsWith(FIELD_PREFIX)) {
            return SupportedLanguage.valueOf(aField.substring(FIELD_PREFIX.length()));
        }
        return null;
    }

    public Analyzer getAnalyzerFor(String aField) {
        if (aField.startsWith(FIELD_PREFIX)) {
            SupportedLanguage theLanguage = SupportedLanguage.valueOf(aField.substring(FIELD_PREFIX.length()));
            Analyzer theAnalyzer = analyzerByLanguage.get(theLanguage);
            if (theAnalyzer != null) {
                return theAnalyzer;
            }
        }
        return standardAnalyzer;
    }
}