package de.mirkosertic.desktopsearch;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DesktopSearchAnalyzerTest {

    @Test
    public void testExact() throws IOException {
        final var analyzer = new DesktopSearchAnalyzer();
        try (final var stream = analyzer.tokenStream("test", "hello world handschuhe")) {
            final var termAttribute = stream.getAttribute(CharTermAttribute.class);
            final var offsetAttribute = stream.getAttribute(OffsetAttribute.class);
            final var positionIncrement = stream.getAttribute(PositionIncrementAttribute.class);
            stream.reset();

            final List<Set<String>> tokens = new ArrayList<>();
            Set<String> current = null;
            while (stream.incrementToken()) {
                final int increment = positionIncrement.getPositionIncrement();
                if (increment >= 1) {
                    current = new HashSet<>();
                    tokens.add(current);;
                }

                final String token = termAttribute.toString();
                final int start = offsetAttribute.startOffset();

                current.add(token);
            }

            System.out.println(tokens);
            Assert.assertEquals("[[hello], [world], [schuhe, handschuhe, hand]]", tokens.toString());
        }
    }

    @Test
    public void testInfix() throws IOException {
        final var analyzer = new DesktopSearchAnalyzer();
        try (final var stream = analyzer.tokenStream("test_infix", "hello world")) {
            final var termAttribute = stream.getAttribute(CharTermAttribute.class);
            final var offsetAttribute = stream.getAttribute(OffsetAttribute.class);
            final var positionIncrement = stream.getAttribute(PositionIncrementAttribute.class);
            stream.reset();

            final List<Set<String>> tokens = new ArrayList<>();
            Set<String> current = null;
            while (stream.incrementToken()) {
                final int increment = positionIncrement.getPositionIncrement();
                if (increment >= 1) {
                    current = new HashSet<>();
                    tokens.add(current);;
                }

                final String token = termAttribute.toString();
                final int start = offsetAttribute.startOffset();

                current.add(token);
            }

            System.out.println(tokens);
            Assert.assertEquals("[[ll, lo, ell, ello, el, hello, he, hell, hel, llo], [world, or, worl, wo, orl, ld, rl, wor, rld, orld]]", tokens.toString());
        }
    }

    @Test
    public void testPrefix() throws IOException {
        final var analyzer = new DesktopSearchAnalyzer();
        try (final var stream = analyzer.tokenStream("test_prefix", "hello world handschuhe")) {
            final var termAttribute = stream.getAttribute(CharTermAttribute.class);
            final var offsetAttribute = stream.getAttribute(OffsetAttribute.class);
            final var positionIncrement = stream.getAttribute(PositionIncrementAttribute.class);
            stream.reset();

            final List<Set<String>> tokens = new ArrayList<>();
            Set<String> current = null;
            while (stream.incrementToken()) {
                final int increment = positionIncrement.getPositionIncrement();
                if (increment >= 1) {
                    current = new HashSet<>();
                    tokens.add(current);;
                }

                final String token = termAttribute.toString();
                final int start = offsetAttribute.startOffset();

                current.add(token);
            }

            System.out.println(tokens);
            Assert.assertEquals("[[hello, he, hell, hel], [world, worl, wo, wor], [handsch, hands, handschuh, schu, schuh, handschu, sc, han, ha, sch, schuhe, handschuhe, handsc, hand]]", tokens.toString());
        }
    }

}
