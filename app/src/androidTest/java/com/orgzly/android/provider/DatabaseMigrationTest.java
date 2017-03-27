package com.orgzly.android.provider;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DatabaseMigrationTest {
    @Test
    public void testParsePropertiesInNote1() {
        StringBuilder newContent = new StringBuilder();

        List<String[]> properties = DatabaseMigration.getPropertiesFromContent(
                "  :PROPERTIES:\n" +
                "  :CREATED:  [2016-05-23 Mon 11:31]\n" +
                "  :VOTES: 1\n" +
                "  :END: \n" +
                "  Whatever",
                newContent);

        assertEquals(2, properties.size());
        assertEquals("CREATED", properties.get(0)[0]);
        assertEquals("[2016-05-23 Mon 11:31]", properties.get(0)[1]);
        assertEquals("VOTES", properties.get(1)[0]);
        assertEquals("1", properties.get(1)[1]);
        assertEquals("  Whatever", newContent.toString());
    }

    @Test
    public void testParsePropertiesInNote2() {
        StringBuilder newContent = new StringBuilder();

        List<String[]> properties = DatabaseMigration.getPropertiesFromContent(
                "Blah\n" +
                ":PROPERTIES:\n" +
                ":CREATED:  [2016-05-23 Mon 11:31]\n" +
                ":VOTES: 1\n" +
                ":END:\n\n",
                newContent);

        assertEquals(0, properties.size());
        assertEquals("", newContent.toString());
    }

    @Test
    public void testParsePropertiesInNote3() {
        StringBuilder newContent = new StringBuilder();

        List<String[]> properties = DatabaseMigration.getPropertiesFromContent(
                ":PROPERTIES:\n" +
                ":CREATED:  [2016-05-23 Mon 11:31]\n" +
                ":END:\n" +
                "Whatever\n",
                newContent);

        assertEquals(1, properties.size());
        assertEquals("Whatever\n", newContent.toString());
    }
}