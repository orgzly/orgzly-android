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
@Ignore("Not a test")
public class OrgFormatterSpeedTest {
    private static String markup;
    private static String links;

    private static final int ITERATIONS = 5;
    private static final int SKIP_FIRST = 2;


    @BeforeClass
    public static void setup() throws Exception {
        markup = readStringFromResource("assets/org/markup-heavy-content.org");
        links = readStringFromResource("assets/org/links-heavy-content.org");
    }

    @Test
    public void markupHeavy() {
        test(markup);
    }

    @Test
    public void linksHeavy() {
        test(links);
    }

    public void test(String str) {
        long t1, t2;

        long[] times = new long[ITERATIONS];

        for (int i = 0; i < ITERATIONS; i++) {
            t1 = System.currentTimeMillis();

            OrgFormatter.parse(str);

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