package com.orgzly.android.util;

import android.net.Uri;

import com.orgzly.android.query.Condition;
import com.orgzly.android.query.Query;
import com.orgzly.android.query.user.DottedQueryParser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class QueryUtilsTest {

    private final String query;
    private final String expectedBookName;

    public QueryUtilsTest(String query, String expectedBookName) {
        this.query = query;
        this.expectedBookName = expectedBookName;
    }

    @Parameterized.Parameters(name = "{index}: query {0} should return book name {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"b.foo", "foo"},
                {"b.foo b.bar", "foo"},
                {"", null}
        });
    }

    @Test
    public void testExtractFirstBookNameFromQuery() throws Exception {
        Condition condition = new DottedQueryParser().parse(query).getCondition();
        String result = QueryUtils.extractFirstBookNameFromQuery(condition);

        assertEquals(expectedBookName, result);
    }
}