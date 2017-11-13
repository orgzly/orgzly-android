package com.orgzly.android.provider.clients;

import com.orgzly.android.Book;
import com.orgzly.android.OrgzlyTest;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class BooksClientTest extends OrgzlyTest {
    @Test
    public void testInsertBookWithExistingName() throws Exception {
        Book book = new Book("booky");

        assertNotNull(book);

        BooksClient.insert(context, book);

        try {
            BooksClient.insert(context, book);
            fail("Second insert of the book should fail");

        } catch (IOException e) {
            assertEquals("Can't insert notebook with the same name: booky", e.getMessage());
        }
    }
}
