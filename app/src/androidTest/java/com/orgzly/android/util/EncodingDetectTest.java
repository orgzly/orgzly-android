package com.orgzly.android.util;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * chardet *
 * ascii.org: ascii (confidence: 1.00)
 * Chinese-Lipsum.org: utf-8 (confidence: 0.99)
 * few_chinese_characters.org: utf-8 (confidence: 0.99)
 * org-blog-articles.org: ISO-8859-2 (confidence: 0.79)
 * org-people.org: ISO-8859-2 (confidence: 0.85)
 */
public class EncodingDetectTest {
    private static final String PATH = "assets/encoding";

    private InputStream getFromResource(String name) {
        String resourcePath = new File(PATH, name).getPath();

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (is == null) {
            throw new IllegalArgumentException("Resource " + resourcePath + " could not be loaded");
        }

        return is;
    }

    @Test
    public void testOrgBlogArticles() throws FileNotFoundException {
        EncodingDetect detect = EncodingDetect.getInstance(getFromResource("org-blog-articles.org"));

        switch (EncodingDetect.USED_METHOD) {
//            case ICU:
//                assertTrue(detect.isDetected());
//                assertEquals("ISO-8859-1", detect.getEncoding());
//                break;
//            case JCHARDET:
//                /* This is not detected and UTF-8 is being used by default, which produces weird chars. */
//                assertTrue(detect.isDetected());
//                assertEquals("windows-1252", detect.getEncoding());
//                break;
            case JUNIVERSALCHARDET:
                assertTrue(detect.isDetected());
                assertEquals("WINDOWS-1252", detect.getEncoding());
                break;
        }
    }

    @Test
    public void testPeople() throws FileNotFoundException {
        EncodingDetect detect = EncodingDetect.getInstance(getFromResource("org-people.org"));

        switch (EncodingDetect.USED_METHOD) {
//            case ICU:
//                assertTrue(detect.isDetected());
//                assertEquals("ISO-8859-1", detect.getEncoding());
//                break;
//            case JCHARDET:
//                assertTrue(detect.isDetected());
//                assertEquals("windows-1252", detect.getEncoding());
//                break;
            case JUNIVERSALCHARDET:
                assertTrue(detect.isDetected());
                assertEquals("WINDOWS-1252", detect.getEncoding());
                break;
        }
    }

    @Test
    public void testAscii() throws FileNotFoundException {
        EncodingDetect detect = EncodingDetect.getInstance(getFromResource("ascii.org"));

        switch (EncodingDetect.USED_METHOD) {
//            case ICU:
//                assertTrue(detect.isDetected());
//                assertEquals("ISO-8859-1", detect.getEncoding());
//                break;
//            case JCHARDET:
//                assertTrue(detect.isDetected());
//                assertEquals("ASCII", detect.getEncoding());
//                break;
            case JUNIVERSALCHARDET:
                assertFalse(detect.isDetected());
                break;
        }
    }

    @Test
    public void testChinese() throws FileNotFoundException {
        EncodingDetect detect = EncodingDetect.getInstance(getFromResource("few_chinese_characters.org"));

        assertTrue(detect.isDetected());
        assertEquals("UTF-8", detect.getEncoding());
    }

    @Test
    public void testChineseLipsum() throws FileNotFoundException {
        EncodingDetect detect = EncodingDetect.getInstance(getFromResource("Chinese-Lipsum.org"));

        assertTrue(detect.isDetected());
        assertEquals("UTF-8", detect.getEncoding());
    }

//    public void testAll() {
//        for (String s: EncodingDetect.getAll()) {
//            System.out.println(s);
//        }
//    }

    @Test
    public void ISO_8859_15_dos() throws UnsupportedEncodingException {
        EncodingDetect detect = EncodingDetect.getInstance(getFromResource("iso-8859-15-dos.org"));

        assertTrue(detect.isDetected());
        assertEquals("WINDOWS-1252", detect.getEncoding());
    }
}
