package de.mirkosertic.desktopsearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.compound.DictionaryCompoundWordTokenFilter;
import org.apache.lucene.analysis.de.GermanNormalizationFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.util.List;

public class DesktopSearchAnalyzer extends Analyzer {

    private final boolean indexTime;

    public DesktopSearchAnalyzer() {
        this(true);
    }

    public DesktopSearchAnalyzer(final boolean indexTime) {
        this.indexTime = indexTime;
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        final Tokenizer tokenizer = new StandardTokenizer();

        final CharArraySet dictionary = new CharArraySet(List.of("hand", "schuhe", "flasche", "zeug"), true);

        TokenStream filter = new LowerCaseFilter(tokenizer);
        filter = new GermanNormalizationFilter(filter);
        filter = new ASCIIFoldingFilter(filter);
        filter = new DictionaryCompoundWordTokenFilter(filter,
                dictionary, 6, 4, 15, false);

        if (indexTime) {
            if (fieldName.endsWith("_infix")) {
                filter = new NGramTokenFilter(filter, 2, 10, true);
            } else if (fieldName.endsWith("_prefix")) {
                filter = new EdgeNGramTokenFilter(filter, 2, 10, true);
            }
        }

        return new TokenStreamComponents(tokenizer, filter);
    }
}
