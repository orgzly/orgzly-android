package com.orgzly.android.util;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class UriUtilsTest {
    private String uriString;
    private String dirString;

    public UriUtilsTest(String uriString, String dirString) {
        this.uriString = uriString;
        this.dirString = dirString;
    }

    @Parameterized.Parameters(name= "{index}: #{0} is inside #{1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "dropbox:/dir1/dir2/file.org", "dropbox:/dir1/dir2" },
                { "dropbox:/dir/file.org",       "dropbox:/dir"       },
                { "dropbox:/dir",                "dropbox:"           },
                { "dropbox:/dir/",               "dropbox:"           },
                { "dropbox:/",                   "dropbox:"           }
        });
    }

    @Test
    public void testDirUri() throws Exception {
        assertEquals(dirString, UriUtils.dirUri(Uri.parse(uriString)).toString());
    }
}