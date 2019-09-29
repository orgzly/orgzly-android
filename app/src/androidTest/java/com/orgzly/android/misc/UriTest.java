package com.orgzly.android.misc;

import android.net.Uri;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UriTest {
    @Test
    public void testUriParseDropbox2() {
        Uri uri = Uri.parse("dropbox:/1/2/3.org");

        assertEquals("URI", "dropbox:/1/2/3.org", uri.toString());
        assertEquals("Scheme", "dropbox", uri.getScheme());
        assertNull("Authority", uri.getAuthority());
        assertEquals("Path", "/1/2/3.org", uri.getPath());
        assertEquals("Filename", "3.org", uri.getLastPathSegment());
    }

    @Test
    public void testUriParseDropbox4() {
        Uri uri = Uri.parse("dropbox:/1");

        assertEquals("URI", "dropbox:/1", uri.toString());
        assertEquals("Scheme", "dropbox", uri.getScheme());
        assertNull("Authority", uri.getAuthority());
        assertEquals("Path", "/1", uri.getPath());
        assertEquals("Filename", "1", uri.getLastPathSegment());
    }

    @Test
    public void testUriParseDropbox5() {
        Uri uri = Uri.parse("dropbox:");

        assertEquals("URI", "dropbox:", uri.toString());
        assertEquals("Scheme", "dropbox", uri.getScheme());
        assertNull("Authority", uri.getAuthority());
        assertNull("Path", uri.getPath());
        assertNull("Filename", uri.getLastPathSegment());
    }

    @Test
    public void testUriParse2() {
        Uri uri = Uri.parse("db://dbname/1/2/3.org");

        assertEquals("URI", "db://dbname/1/2/3.org", uri.toString());
        assertEquals("Scheme", "db", uri.getScheme());
        assertEquals("Authority", "dbname", uri.getAuthority());
        assertEquals("Path", "/1/2/3.org", uri.getPath());
        assertEquals("Filename", "3.org", uri.getLastPathSegment());
    }

    @Test
    public void testUriParse3() {
        Uri uri = Uri.parse("file://dirname/1/2/3.org");

        assertEquals("URI", "file://dirname/1/2/3.org", uri.toString());
        assertEquals("Scheme", "file", uri.getScheme());
        assertEquals("Authority", "dirname", uri.getAuthority());
        assertEquals("Path", "/1/2/3.org", uri.getPath());
        assertEquals("Filename", "3.org", uri.getLastPathSegment());
    }

    @Test
    public void testUriParse4() {
        Uri uri = Uri.parse("file://dirname");

        assertEquals("URI", "file://dirname", uri.toString());
        assertEquals("Scheme", "file", uri.getScheme());
        assertEquals("Authority", "dirname", uri.getAuthority());
        assertEquals("Path", "", uri.getPath());
        assertNull("Filename", uri.getLastPathSegment());
    }

    @Test
    public void testUriParse5() {
        Uri uri = Uri.parse("ssh://user:pass@hostname/1/2/3.org");

        assertEquals("URI", "ssh://user:pass@hostname/1/2/3.org", uri.toString());
        assertEquals("Scheme", "ssh", uri.getScheme());
        assertEquals("Authority", "user:pass@hostname", uri.getAuthority());
        assertEquals("Path", "/1/2/3.org", uri.getPath());
        assertEquals("Filename", "3.org", uri.getLastPathSegment());
    }

    @Test
    public void testAppendPath1() {
        Uri uri = Uri.parse("ssh://user:pass@hostname/1").buildUpon().appendPath("2").appendPath("3.org").build();

        assertEquals("URI", "ssh://user:pass@hostname/1/2/3.org", uri.toString());
        assertEquals("Scheme", "ssh", uri.getScheme());
        assertEquals("Authority", "user:pass@hostname", uri.getAuthority());
        assertEquals("Path", "/1/2/3.org", uri.getPath());
        assertEquals("Filename", "3.org", uri.getLastPathSegment());
    }

    @Test
    public void testAppendPath12() {
        Uri uri = Uri.parse("ssh://user:pass@hostname").buildUpon().appendPath("1").appendPath("2").appendPath("3.org").build();

        assertEquals("URI", "ssh://user:pass@hostname/1/2/3.org", uri.toString());
        assertEquals("Scheme", "ssh", uri.getScheme());
        assertEquals("Authority", "user:pass@hostname", uri.getAuthority());
        assertEquals("Path", "/1/2/3.org", uri.getPath());
        assertEquals("Filename", "3.org", uri.getLastPathSegment());
    }

    @Test
    public void testAppendPath2() {
        Uri uri = Uri.parse("ssh://user:pass@hostname/1").buildUpon().appendPath("").appendPath("2").appendPath("3.org").build();

        assertEquals("URI", "ssh://user:pass@hostname/1/2/3.org", uri.toString());
        assertEquals("Scheme", "ssh", uri.getScheme());
        assertEquals("Authority", "user:pass@hostname", uri.getAuthority());
        assertEquals("Path", "/1/2/3.org", uri.getPath());
        assertEquals("Filename", "3.org", uri.getLastPathSegment());
    }

    @Test
    public void testAppendPath6() {
        Uri uri = Uri.parse("file://dirname").buildUpon().appendPath("1").appendPath("2.org").build();

        assertEquals("URI", "file://dirname/1/2.org", uri.toString());
        assertEquals("Scheme", "file", uri.getScheme());
        assertEquals("Authority", "dirname", uri.getAuthority());
        assertEquals("Path", "/1/2.org", uri.getPath());
        assertEquals("Filename", "2.org", uri.getLastPathSegment());
    }

    @Test
    public void testScheme1() {
        Uri uri = new Uri.Builder().scheme("dropbox").appendPath("1").appendPath("2.org").build();

        assertEquals("URI", "dropbox:/1/2.org", uri.toString());
        assertEquals("Scheme", "dropbox", uri.getScheme());
        assertNull("Authority", uri.getAuthority());
        assertEquals("Path", "/1/2.org", uri.getPath());
        assertEquals("Filename", "2.org", uri.getLastPathSegment());
    }

    @Test
    public void testScheme2() {
        Uri uri = new Uri.Builder().scheme("dropbox").appendPath("").appendPath("1").appendPath("2.org").build();

        assertEquals("URI", "dropbox:/1/2.org", uri.toString());
        assertEquals("Scheme", "dropbox", uri.getScheme());
        assertNull("Authority", uri.getAuthority());
        assertEquals("Path", "/1/2.org", uri.getPath());
        assertEquals("Filename", "2.org", uri.getLastPathSegment());
    }

//    @Test
//    public void testFileOwncloud() {
//        Uri.parse("file:/storage/emulated/0/owncloud/rene@renans.eu%2Fowncloud\nNotes");
//    }
}