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

        assertEquals(2, query.currentGroup.getTextSearch().size());
        assertTrue(query.currentGroup.getTextSearch().contains("search"));
        assertTrue(query.currentGroup.getTextSearch().contains("string"));
    }

    @Test
    public void test2() {
        SearchQuery query = new SearchQuery("b.name");

        assertEquals("name", query.currentGroup.getBookName());
    }

    @Test
    public void test4() {
        SearchQuery query = new SearchQuery("s.today any   b.name1 t.tag .tag2.  i.todo tn.notetag string  b.\"name 2\"");

        assertEquals("b.\"name 2\" i.todo tn.notetag t.tag s.today any .tag2. string", query.toString());

        /* Free text. */
        assertTrue(query.currentGroup.hasTextSearch());
        assertEquals(3, query.currentGroup.getTextSearch().size());
        assertTrue(query.currentGroup.getTextSearch().contains("any"));
        assertTrue(query.currentGroup.getTextSearch().contains("string"));
        assertTrue(query.currentGroup.getTextSearch().contains(".tag2."));

        /* Book. */
        assertEquals("name 2", query.currentGroup.getBookName());

        /* State. */
        assertEquals("TODO", query.currentGroup.getState());

        /* Note tags. */
        assertEquals(1, query.currentGroup.getNoteTags().size());
        assertTrue(query.currentGroup.getNoteTags().contains("notetag"));

        /* Tags. */
        assertEquals(1, query.currentGroup.getTags().size());
        assertTrue(query.currentGroup.getTags().contains("tag"));

        /* Scheduled time. */
        assertTrue(query.currentGroup.hasScheduled());
        assertEquals(0, query.currentGroup.getScheduled().getValue());
        assertEquals(OrgInterval.Unit.DAY, query.currentGroup.getScheduled().getUnit());
    }

    @Test
    public void test5() {
        SearchQuery query = new SearchQuery("s.2w b.n1 t.tag .tag2.  i.todo   b.n2");

        assertEquals("b.n2 i.todo t.tag s.2w .tag2.", query.currentGroup.toString());

        /* Free text. */
        assertTrue(query.currentGroup.hasTextSearch());
        assertEquals(1, query.currentGroup.getTextSearch().size());
        assertTrue(query.currentGroup.getTextSearch().contains(".tag2."));

        /* Book. */
        assertEquals("n2", query.currentGroup.getBookName());

        /* State. */
        assertEquals("TODO", query.currentGroup.getState());

        /* Tags. */
        assertEquals(1, query.currentGroup.getTags().size());
        assertTrue(query.currentGroup.getTags().contains("tag"));

        /* Scheduled time. */
        assertTrue(query.currentGroup.hasScheduled());
        assertEquals(2, query.currentGroup.getScheduled().getValue());
        assertEquals(OrgInterval.Unit.WEEK, query.currentGroup.getScheduled().getUnit());
    }

    @Test
    public void testBookName1() {
        SearchQuery query = new SearchQuery("b.\"Org Manual\"");

        assertEquals("b.\"Org Manual\"", query.currentGroup.toString());

        /* Free text. */
        assertFalse(query.currentGroup.hasTextSearch());

        /* Book. */
        assertEquals("Org Manual", query.currentGroup.getBookName());
    }

    @Test
    public void testBookName2() {
        SearchQuery query = new SearchQuery();
        query.currentGroup.setBookName("Book \"Quote\" Book");
        // Book "Quote" Book

        assertEquals("b.\"Book \\\"Quote\\\" Book\"", query.currentGroup.toString());
        // b."Book \"Quote\" Book"

        /* Free text. */
        assertFalse(query.currentGroup.hasTextSearch());

        /* Book. */
        assertEquals("Book \"Quote\" Book", query.currentGroup.getBookName());
    }

    @Test
    public void testBookName3() {
        SearchQuery query = new SearchQuery();
        query.currentGroup.setBookName("Book \\\"Quote\" Book");
        // Book \"Quote" Book

        assertEquals("b.\"Book \\\\\\\"Quote\\\" Book\"", query.currentGroup.toString());
        // b."Book \\\"Quote\" Book"

        /* Free text. */
        assertFalse(query.currentGroup.hasTextSearch());

        /* Book. */
        assertEquals("Book \\\"Quote\" Book", query.currentGroup.getBookName());
    }

    @Test
    public void testBookName4() {
        SearchQuery query = new SearchQuery();
        query.currentGroup.setBookName("Book Book");

        assertEquals("b.\"Book Book\"", query.currentGroup.toString());

        /* Free text. */
        assertFalse(query.currentGroup.hasTextSearch());

        /* Book. */
        assertEquals("Book Book", query.currentGroup.getBookName());
    }

    @Test
    public void testBookName5() {
        SearchQuery query = new SearchQuery();
        query.currentGroup.setBookName("Book\"Book");
        // Book"Book

        assertEquals("b.\"Book\\\"Book\"", query.currentGroup.toString());
        // b."Book\"Book"

        /* Free text. */
        assertFalse(query.currentGroup.hasTextSearch());

        /* Book. */
        assertEquals("Book\"Book", query.currentGroup.getBookName());
    }

    @Test
    public void testNotBookName() {
        SearchQuery query = new SearchQuery("t.tag1 .b.\"Book Name\" t.tag2 .b.bookname");

        assertTrue(query.currentGroup.hasNotBookName());
        assertEquals(2, query.currentGroup.getNotBookName().size());
        assertTrue(query.currentGroup.getNotBookName().contains("bookname"));
        assertTrue(query.currentGroup.getNotBookName().contains("Book Name"));

        assertEquals(".b.\"Book Name\" .b.bookname t.tag1 t.tag2", query.currentGroup.toString());
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

        assertEquals("", query.currentGroup.toString());
        assertFalse(query.currentGroup.hasTextSearch());
        assertFalse(query.currentGroup.hasBookName());
        assertFalse(query.currentGroup.hasTags());
        assertFalse(query.currentGroup.hasNotState());
        assertFalse(query.currentGroup.hasState());
        assertFalse(query.currentGroup.hasScheduled());
    }

    @Test
    public void testPriority() {
        SearchQuery query = new SearchQuery("p.A");
        assertEquals("a", query.currentGroup.getPriority());
    }

    @Test
    public void testScheduledTomorrow() {
        SearchQuery query = new SearchQuery("s.tom");

        assertEquals("s.tomorrow", query.currentGroup.toString());

        assertTrue(query.currentGroup.hasScheduled());
        assertEquals(1, query.currentGroup.getScheduled().getValue());
        assertEquals(OrgInterval.Unit.DAY, query.currentGroup.getScheduled().getUnit());
    }

    @Test
    public void testScheduledDays() {
        SearchQuery query = new SearchQuery("s.5d");

        assertEquals("s.5d", query.currentGroup.toString());

        assertTrue(query.currentGroup.hasScheduled());
        assertEquals(5, query.currentGroup.getScheduled().getValue());
        assertEquals(OrgInterval.Unit.DAY, query.currentGroup.getScheduled().getUnit());
    }

    @Test
    public void testDeadline() {
        SearchQuery query = new SearchQuery("s.1w d.2d");

        assertEquals("s.1w d.2d", query.currentGroup.toString());

        assertTrue(query.currentGroup.hasDeadline());
        assertEquals(2, query.currentGroup.getDeadline().getValue());
        assertEquals(OrgInterval.Unit.DAY, query.currentGroup.getDeadline().getUnit());
    }

    @Test
    public void testScheduledNone() {
        SearchQuery query = new SearchQuery("s.none");
        assertEquals("s.none", query.currentGroup.toString());
        assertTrue(query.currentGroup.hasScheduled());
        assertTrue(query.currentGroup.getScheduled().none());
    }

    @Test
    public void testScheduledInvalid() {
        SearchQuery query = new SearchQuery("s.5z");
        assertEquals("", query.currentGroup.toString());
        assertFalse(query.currentGroup.hasScheduled());
    }

    @Test
    public void testMultipleNotState() {
        SearchQuery query = new SearchQuery(".i.done .i.cancel i.today");
        assertEquals("i.today .i.cancel .i.done", query.currentGroup.toString());
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
        assertEquals("Book'Name", new SearchQuery("b.Book'Name").currentGroup.getBookName());
    }
}
