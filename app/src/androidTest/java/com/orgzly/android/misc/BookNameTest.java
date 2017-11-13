package com.orgzly.android.misc;

import com.orgzly.android.BookName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class BookNameTest {

    private String name;
    private boolean isSupported;

    public BookNameTest(String name, boolean isSupported) {
        this.name = name;
        this.isSupported = isSupported;
    }

    @Parameterized.Parameters(name= "{index}: Filename {0} supported: #{1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "filename.org", true },
                { "filename.txt", false },
                { "filename.org.txt", true },
                { ".#filename.org", false },
        });
    }

    @Test
    public void testIsSupportedFormatFileName() throws Exception {
        assertEquals(BookName.isSupportedFormatFileName(name), isSupported);
    }
}