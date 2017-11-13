package com.orgzly.android.util;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MiscUtilsTest {
    @Test
    public void testLineCount1() {
        assertEquals(0, MiscUtils.lineCount(null));
    }

    @Test
    public void testLineCount2() {
        assertEquals(1, MiscUtils.lineCount(""));
    }

    @Test
    public void testLineCount3() {
        assertEquals(2, MiscUtils.lineCount("\n"));
    }

    @Test
    public void testLineCount4() {
        assertEquals(2, MiscUtils.lineCount("Lorem ipsum\ndolor sit amet."));
    }

    @Test
    public void testLineCount5() {
        assertEquals(3, MiscUtils.lineCount("Lorem ipsum\ndolor sit amet.\n"));
    }

    @Test
    public void testLineCount6() {
        assertEquals(3, MiscUtils.lineCount("\nLorem ipsum dolor sit amet.\n"));
    }

    @Test
    public void testLineCount7() {
        assertEquals(4, MiscUtils.lineCount("\n\nLorem ipsum dolor sit amet.\n"));
    }

    @Test
    public void testLineCount8() {
        assertEquals(3, MiscUtils.lineCount("\n\n"));
    }


    @Test
    public void testEncodeUri() {
        Assert.assertEquals(
                "file:/storage/emulated/0/org/file%20%231%20is%20%3Athis%3As.org",
                MiscUtils.encodeUri("file:/storage/emulated/0/org/file #1 is :this:s.org"));

        Assert.assertEquals(
                "file:/storage/emulated/0/org/file%20%231%20is%20this.org",
                MiscUtils.encodeUri("file:/storage/emulated/0/org/file #1 is this.org"));

        Assert.assertEquals(
                "file:/storage/emulated/0/org/dir%23dir/file%20name.org",
                MiscUtils.encodeUri("file:/storage/emulated/0/org/dir#dir/file name.org"));

        Assert.assertEquals(
                "file:/storage/emulated/0/org/dir%25dir/file%20%25%20name.org",
                MiscUtils.encodeUri("file:/storage/emulated/0/org/dir%dir/file % name.org"));

        Assert.assertEquals(
                "file:/storage/emulated/0/org/dir/file%25name.org",
                MiscUtils.encodeUri("file:/storage/emulated/0/org/dir/file%name.org"));

        Assert.assertEquals(
                "file:/storage/emulated/0/org/dir/%25.org",
                MiscUtils.encodeUri("file:/storage/emulated/0/org/dir/%.org"));

        Assert.assertEquals(
                "file:/storage/emulated/0/org-enc/name%20with%20percent%20%25%20in%20it.org",
                MiscUtils.encodeUri("file:/storage/emulated/0/org-enc/name%20with%20percent%20%25%20in%20it.org"));
    }

    @Test
    public void testUriNeedsEncoding() {
        assertFalse(MiscUtils.uriPathNeedsEncoding("/dir"));
        assertFalse(MiscUtils.uriPathNeedsEncoding("/dir/%200"));

        assertTrue(MiscUtils.uriPathNeedsEncoding("/dir/%"));
        assertTrue(MiscUtils.uriPathNeedsEncoding("/dir/%%"));
        assertTrue(MiscUtils.uriPathNeedsEncoding("/dir/%%%"));
        assertTrue(MiscUtils.uriPathNeedsEncoding("/dir/+"));
        assertTrue(MiscUtils.uriPathNeedsEncoding("/dir/%l0"));
        assertTrue(MiscUtils.uriPathNeedsEncoding("/dir/%0l"));
        assertTrue(MiscUtils.uriPathNeedsEncoding("/dir/%0 0"));
        assertTrue(MiscUtils.uriPathNeedsEncoding("/dir/file name"));
    }
}
