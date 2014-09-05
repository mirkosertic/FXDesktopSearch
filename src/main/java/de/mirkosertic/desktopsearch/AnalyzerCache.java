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

    private static Analyzer configure(Analyzer aAnalyzer) {
        aAnalyzer.setVersion(IndexFields.LUCENE_VERSION);
        return aAnalyzer;
    }

    public AnalyzerCache(Configuration aConfiguration) {
        standardAnalyzer = configure(new StandardAnalyzer());
        analyzerByLanguage = new HashMap<>();

        registerIfEnabled(SupportedLanguage.ar, aConfiguration, configure(new ArabicAnalyzer()));
        registerIfEnabled(SupportedLanguage.bg, aConfiguration, configure(new BulgarianAnalyzer()));
        registerIfEnabled(SupportedLanguage.br, aConfiguration, configure(new BrazilianAnalyzer()));
        registerIfEnabled(SupportedLanguage.ca, aConfiguration, configure(new CatalanAnalyzer()));
        registerIfEnabled(SupportedLanguage.ckb, aConfiguration, configure(new SoraniAnalyzer()));
        registerIfEnabled(SupportedLanguage.cz, aConfiguration, configure(new CzechAnalyzer()));
        registerIfEnabled(SupportedLanguage.da, aConfiguration, configure(new DanishAnalyzer()));
        registerIfEnabled(SupportedLanguage.de, aConfiguration, configure(new GermanAnalyzer()));
        registerIfEnabled(SupportedLanguage.el, aConfiguration, configure(new GreekAnalyzer()));
        registerIfEnabled(SupportedLanguage.en, aConfiguration, configure(new EnglishAnalyzer()));
        registerIfEnabled(SupportedLanguage.es, aConfiguration, configure(new SpanishAnalyzer()));
        registerIfEnabled(SupportedLanguage.eu, aConfiguration, configure(new BasqueAnalyzer()));
        registerIfEnabled(SupportedLanguage.fa, aConfiguration, configure(new PersianAnalyzer()));
        registerIfEnabled(SupportedLanguage.fi, aConfiguration, configure(new FinnishAnalyzer()));
        registerIfEnabled(SupportedLanguage.fr, aConfiguration, configure(new FrenchAnalyzer()));
        registerIfEnabled(SupportedLanguage.ga, aConfiguration, configure(new IrishAnalyzer()));
        registerIfEnabled(SupportedLanguage.gl, aConfiguration, configure(new GalicianAnalyzer()));
        registerIfEnabled(SupportedLanguage.hi, aConfiguration, configure(new HindiAnalyzer()));
        registerIfEnabled(SupportedLanguage.hu, aConfiguration, configure(new HungarianAnalyzer()));
        registerIfEnabled(SupportedLanguage.hy, aConfiguration, configure(new ArmenianAnalyzer()));
        registerIfEnabled(SupportedLanguage.id, aConfiguration, configure(new IndonesianAnalyzer()));
        registerIfEnabled(SupportedLanguage.it, aConfiguration, configure(new ItalianAnalyzer()));
        registerIfEnabled(SupportedLanguage.lv, aConfiguration, configure(new LatvianAnalyzer()));
        registerIfEnabled(SupportedLanguage.nl, aConfiguration, configure(new DutchAnalyzer()));
        registerIfEnabled(SupportedLanguage.no, aConfiguration, configure(new NorwegianAnalyzer()));
        registerIfEnabled(SupportedLanguage.pt, aConfiguration, configure(new PortugueseAnalyzer()));
        registerIfEnabled(SupportedLanguage.ro, aConfiguration, configure(new RomanianAnalyzer()));
        registerIfEnabled(SupportedLanguage.ru, aConfiguration, configure(new RussianAnalyzer()));
        registerIfEnabled(SupportedLanguage.sv, aConfiguration, configure(new SwedishAnalyzer()));
        registerIfEnabled(SupportedLanguage.th, aConfiguration, configure(new ThaiAnalyzer()));
        registerIfEnabled(SupportedLanguage.tr, aConfiguration, configure(new TurkishAnalyzer()));
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