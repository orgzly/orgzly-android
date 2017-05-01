package com.orgzly.android.misc;

import com.orgzly.android.SearchQuery;
import com.orgzly.android.util.QuotedStringTokenizer;
import com.orgzly.org.datetime.OrgInterval;
import org.junit.Test;

import static junit.framework.Assert.*;

public class SearchQueryTest {
    @Test
    public void test1() {
        SearchQuery query = new SearchQuery("search string");

        assertEquals(2, query.getTextSearch().size());
        assertTrue(query.getTextSearch().contains("search"));
        assertTrue(query.getTextSearch().contains("string"));
    }

    @Test
    public void test2() {
        SearchQuery query = new SearchQuery("b.name");

        assertEquals("name", query.getBookName());
    }

    @Test
    public void test4() {
        SearchQuery query = new SearchQuery("s.today any   b.name1 t.tag .tag2.  i.todo tn.notetag string  b.\"name 2\"");

        assertEquals("b.\"name 2\" i.todo tn.notetag t.tag s.today any .tag2. string", query.toString());

        /* Free text. */
        assertTrue(query.hasTextSearch());
        assertEquals(3, query.getTextSearch().size());
        assertTrue(query.getTextSearch().contains("any"));
        assertTrue(query.getTextSearch().contains("string"));
        assertTrue(query.getTextSearch().contains(".tag2."));

        /* Book. */
        assertEquals("name 2", query.getBookName());

        /* State. */
        assertEquals("TODO", query.getState());

        /* Note tags. */
        assertEquals(1, query.getNoteTags().size());
        assertTrue(query.getNoteTags().contains("notetag"));

        /* Tags. */
        assertEquals(1, query.getTags().size());
        assertTrue(query.getTags().contains("tag"));

        /* Scheduled time. */
        assertTrue(query.hasScheduled());
        assertEquals(0, query.getScheduled().getValue());
        assertEquals(OrgInterval.Unit.DAY, query.getScheduled().getUnit());
    }

    @Test
    public void test5() {
        SearchQuery query = new SearchQuery("s.2w b.n1 t.tag .tag2.  i.todo   b.n2");

        assertEquals("b.n2 i.todo t.tag s.2w .tag2.", query.toString());

        /* Free text. */
        assertTrue(query.hasTextSearch());
        assertEquals(1, query.getTextSearch().size());
        assertTrue(query.getTextSearch().contains(".tag2."));

        /* Book. */
        assertEquals("n2", query.getBookName());

        /* State. */
        assertEquals("TODO", query.getState());

        /* Tags. */
        assertEquals(1, query.getTags().size());
        assertTrue(query.getTags().contains("tag"));

        /* Scheduled time. */
        assertTrue(query.hasScheduled());
        assertEquals(2, query.getScheduled().getValue());
        assertEquals(OrgInterval.Unit.WEEK, query.getScheduled().getUnit());
    }

    @Test
    public void testBookName1() {
        SearchQuery query = new SearchQuery("b.\"Org Manual\"");

        assertEquals("b.\"Org Manual\"", query.toString());

        /* Free text. */
        assertFalse(query.hasTextSearch());

        /* Book. */
        assertEquals("Org Manual", query.getBookName());
    }

    @Test
    public void testBookName2() {
        SearchQuery query = new SearchQuery();
        query.setBookName("Book \"Quote\" Book");
        // Book "Quote" Book

        assertEquals("b.\"Book \\\"Quote\\\" Book\"", query.toString());
        // b."Book \"Quote\" Book"

        /* Free text. */
        assertFalse(query.hasTextSearch());

        /* Book. */
        assertEquals("Book \"Quote\" Book", query.getBookName());
    }

    @Test
    public void testBookName3() {
        SearchQuery query = new SearchQuery();
        query.setBookName("Book \\\"Quote\" Book");
        // Book \"Quote" Book

        assertEquals("b.\"Book \\\\\\\"Quote\\\" Book\"", query.toString());
        // b."Book \\\"Quote\" Book"

        /* Free text. */
        assertFalse(query.hasTextSearch());

        /* Book. */
        assertEquals("Book \\\"Quote\" Book", query.getBookName());
    }

    @Test
    public void testBookName4() {
        SearchQuery query = new SearchQuery();
        query.setBookName("Book Book");

        assertEquals("b.\"Book Book\"", query.toString());

        /* Free text. */
        assertFalse(query.hasTextSearch());

        /* Book. */
        assertEquals("Book Book", query.getBookName());
    }

    @Test
    public void testBookName5() {
        SearchQuery query = new SearchQuery();
        query.setBookName("Book\"Book");
        // Book"Book

        assertEquals("b.\"Book\\\"Book\"", query.toString());
        // b."Book\"Book"

        /* Free text. */
        assertFalse(query.hasTextSearch());

        /* Book. */
        assertEquals("Book\"Book", query.getBookName());
    }

    @Test
    public void testNotBookName() {
        SearchQuery query = new SearchQuery("t.tag1 .b.\"Book Name\" t.tag2 .b.bookname");

        assertTrue(query.hasNotBookName());
        assertEquals(2, query.getNotBookName().size());
        assertTrue(query.getNotBookName().contains("bookname"));
        assertTrue(query.getNotBookName().contains("Book Name"));

        assertEquals(".b.\"Book Name\" .b.bookname t.tag1 t.tag2", query.toString());
    }

    @Test
    public void testTokenizer() {
        assertTokens("i.done b.Word .t.tag",
                new String[] { "i.done", "b.Word", ".t.tag" });

        assertTokens("i.done b.\"Some Words\" .t.tag",
                new String[] { "i.done", "b.\"Some Words\"", ".t.tag" });

        assertTokens("i.done   b.\"Some  Words\"   .t.tag",
                new String[] { "i.done", "b.\"Some  Words\"", ".t.tag" });

        assertTokens("i.done b.\"Some \\\"Quoted\\\" Words\" .t.tag",
                new String[] { "i.done", "b.\"Some \\\"Quoted\\\" Words\"", ".t.tag" });

        assertTokens("i.done b.\\Some\\Quoted\\Words t.\"tag\"",
                new String[] { "i.done", "b.\\Some\\Quoted\\Words", "t.\"tag\"" });

        assertTokens("( i.done   OR b.\"Some \\\"Quoted\\\" Words\" ) AND .t.tag",
                new String[] { "(", "i.done", "OR", "b.\"Some \\\"Quoted\\\" Words\"", ")", "AND", ".t.tag" });
    }

    private void assertTokens(String str, String[] expectedTokens) {
        QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(str, " ", false, true);

        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            assertEquals(expectedTokens[i++], tokenizer.nextToken());
        }

        assertEquals(expectedTokens.length, i);
    }

    @Test
    public void testEmptyQueryString() {
        SearchQuery query = new SearchQuery("");

        assertEquals("", query.toString());
        assertFalse(query.hasTextSearch());
        assertFalse(query.hasBookName());
        assertFalse(query.hasTags());
        assertFalse(query.hasNotState());
        assertFalse(query.hasState());
        assertFalse(query.hasScheduled());
    }

    @Test
    public void testPriority() {
        SearchQuery query = new SearchQuery("p.A");
        assertEquals("a", query.getPriority());
    }

    @Test
    public void testScheduledTomorrow() {
        SearchQuery query = new SearchQuery("s.tom");

        assertEquals("s.tomorrow", query.toString());

        assertTrue(query.hasScheduled());
        assertEquals(1, query.getScheduled().getValue());
        assertEquals(OrgInterval.Unit.DAY, query.getScheduled().getUnit());
    }

    @Test
    public void testScheduledDays() {
        SearchQuery query = new SearchQuery("s.5d");

        assertEquals("s.5d", query.toString());

        assertTrue(query.hasScheduled());
        assertEquals(5, query.getScheduled().getValue());
        assertEquals(OrgInterval.Unit.DAY, query.getScheduled().getUnit());
    }

    @Test
    public void testDeadline() {
        SearchQuery query = new SearchQuery("s.1w d.2d");

        assertEquals("s.1w d.2d", query.toString());

        assertTrue(query.hasDeadline());
        assertEquals(2, query.getDeadline().getValue());
        assertEquals(OrgInterval.Unit.DAY, query.getDeadline().getUnit());
    }

    @Test
    public void testScheduledNone() {
        SearchQuery query = new SearchQuery("s.none");
        assertEquals("s.none", query.toString());
        assertTrue(query.hasScheduled());
        assertTrue(query.getScheduled().none());
    }

    @Test
    public void testScheduledInvalid() {
        SearchQuery query = new SearchQuery("s.5z");
        assertEquals("", query.toString());
        assertFalse(query.hasScheduled());
    }

    @Test
    public void testMultipleNotState() {
        SearchQuery query = new SearchQuery(".i.done .i.cancel i.today");
        assertEquals("i.today .i.cancel .i.done", query.toString());
    }

    @Test
    public void testSortOrderScheduled() {
        SearchQuery query = new SearchQuery("note o.scheduled");

        assertEquals(1, query.getSortOrder().size());

        SearchQuery.SortOrder sortOrder = query.getSortOrder().get(0);
        assertEquals(SearchQuery.SortOrder.Type.SCHEDULED, sortOrder.getType());
        assertTrue(sortOrder.isAscending());

        assertEquals("note o.scheduled", query.toString());
    }

    @Test
    public void testSortOrderScheduledDesc() {
        SearchQuery query = new SearchQuery("note .o.scheduled");

        assertEquals(1, query.getSortOrder().size());

        SearchQuery.SortOrder sortOrder = query.getSortOrder().get(0);
        assertEquals(SearchQuery.SortOrder.Type.SCHEDULED, sortOrder.getType());
        assertTrue(sortOrder.isDescending());

        assertEquals("note .o.scheduled", query.toString());
    }

    @Test
    public void testDotAsBookName() {
        assertNotNull(new SearchQuery("b.."));
    }

    @Test
    public void testSingleQuoteInBookName() {
        assertEquals("Book'Name", new SearchQuery("b.Book'Name").getBookName());
    }
}
