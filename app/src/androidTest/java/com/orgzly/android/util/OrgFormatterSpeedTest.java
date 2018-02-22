package com.orgzly.android.util;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.LongSummaryStatistics;


/**
 * Simple benchmark for {@link OrgFormatter}, written as a failing test (to display the results).
 */
@Ignore
public class OrgFormatterSpeedTest {
    private static String string;

    private static final int ITERATIONS = 5;
    private static final int SKIP_FIRST = 2;

    private static final String RESOURCE = "assets/org/markup-heavy-content.org";

    @BeforeClass
    public static void setup() throws Exception {
        string = readStringFromResource(RESOURCE);
    }

    @Test
    public void testSpeed() throws IOException {
        long t1, t2;

        long[] times = new long[ITERATIONS];

        for (int i = 0; i < ITERATIONS; i++) {
            t1 = System.currentTimeMillis();

            OrgFormatter.INSTANCE.parse(string);

            t2 = System.currentTimeMillis();

            times[i] = t2 - t1;
        }

        LongSummaryStatistics stats = Arrays.stream(times).skip(SKIP_FIRST).summaryStatistics();

        Assert.fail(stats.toString());
    }

    private static String readStringFromResource(String filename) throws IOException {
        return MiscUtils.readStream(OrgFormatterSpeedTest.class.getClassLoader().getResourceAsStream(filename));
    }
}