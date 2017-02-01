package com.orgzly.android.repos;

import com.orgzly.android.OrgzlyTest;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class RepoFactoryTest extends OrgzlyTest {

    @Test
    public void testCreateRepoFromUrl1() {
        Repo repo = RepoFactory.getFromUri(context, "dropbox:");
        assertTrue(repo instanceof DropboxRepo);
        assertEquals("dropbox:", repo.getUri().toString());
    }

    @Test
    public void testCreateRepoFromUrl2() {
        Repo repo = RepoFactory.getFromUri(context, "dropbox:/path");
        assertTrue(repo instanceof DropboxRepo);
        assertEquals("dropbox:/path", repo.getUri().toString());
    }

    @Test
    public void testCreateRepoFromUrl10() {
        Repo repo = RepoFactory.getFromUri(context, "mock://authority/path");
        assertTrue(repo instanceof MockRepo);
        assertEquals("mock://authority/path", repo.getUri().toString());
    }

    @Test
    public void testCreateRepoFromUrl100() {
        try {
            RepoFactory.getFromUri(context, "I am not a valid URL");
            fail("URL should be invalid");

        } catch (IllegalArgumentException e) {
        }
    }
}
