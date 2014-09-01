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
import org.apache.lucene.util.Version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AnalyzerCache {

    private final Map<String, Analyzer> analyzerByLanguage;
    private final Analyzer standardAnalyzer;

    public AnalyzerCache(Version aLuceneVersion) {
        standardAnalyzer = new StandardAnalyzer(aLuceneVersion);
        analyzerByLanguage = new HashMap<>();

        analyzerByLanguage.put("ar", new ArabicAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("bg", new BulgarianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("br", new BrazilianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("ca", new CatalanAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("ckb", new SoraniAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("cz", new CzechAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("da", new DanishAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("de", new GermanAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("el", new GreekAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("en", new EnglishAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("es", new SpanishAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("eu", new BasqueAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("fa", new PersianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("fi", new FinnishAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("fr", new FrenchAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("ga", new IrishAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("gl", new GalicianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("hi", new HindiAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("hu", new HungarianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("hy", new ArmenianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("id", new IndonesianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("it", new ItalianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("lv", new LatvianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("nl", new DutchAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("no", new NorwegianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("pt", new PortugueseAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("ro", new RomanianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("ru", new RussianAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("sv", new SwedishAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("th", new ThaiAnalyzer(aLuceneVersion));
        analyzerByLanguage.put("tr", new TurkishAnalyzer(aLuceneVersion));
    }

    public String getFieldNameFor(String aLanguage) {
        return IndexFields.CONTENT + "_" + aLanguage;
    }

    public Analyzer getAnalyzer() {
        Map<String, Analyzer> theFieldAnalyzer = new HashMap<>();
        analyzerByLanguage.entrySet().stream().forEach(e -> theFieldAnalyzer.put(getFieldNameFor(e.getKey()), e.getValue()));
        return new PerFieldAnalyzerWrapper(standardAnalyzer, theFieldAnalyzer);
    }

    public boolean supportsLanguage(String aLanguage) {
        return analyzerByLanguage.containsKey(aLanguage);
    }

    public String[] getAllFieldNames() {
        List<String> theFieldNames = new ArrayList<>();
        theFieldNames.add(IndexFields.CONTENT);
        analyzerByLanguage.entrySet().stream().forEach(e -> theFieldNames.add(getFieldNameFor(e.getKey())));
        return theFieldNames.toArray(new String[theFieldNames.size()]);
    }
}
