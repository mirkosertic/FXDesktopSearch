package de.mirkosertic.desktopsearch;

import junit.framework.Assert;
import org.junit.Test;

public class ContentExtractorTest {

    @Test
    public void testSupportedFileTyes() {
        ContentExtractor theExtractor = new ContentExtractor();
        Assert.assertTrue(theExtractor.supportsFile("lala.pdf"));
        Assert.assertTrue(theExtractor.supportsFile("lala.PDF"));
        Assert.assertTrue(theExtractor.supportsFile("lala.msg"));
        Assert.assertTrue(theExtractor.supportsFile("lala.doc"));
        Assert.assertTrue(theExtractor.supportsFile("lala.docx"));
        Assert.assertTrue(theExtractor.supportsFile("lala.ppt"));
        Assert.assertTrue(theExtractor.supportsFile("lala.pptx"));
        Assert.assertTrue(theExtractor.supportsFile("lala.rtf"));
        Assert.assertTrue(theExtractor.supportsFile("lala.html"));
        Assert.assertFalse(theExtractor.supportsFile(".lala"));
        Assert.assertFalse(theExtractor.supportsFile("lala."));
        Assert.assertFalse(theExtractor.supportsFile(""));
    }
}
