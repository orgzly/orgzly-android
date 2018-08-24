package com.orgzly.android.repos;

import com.orgzly.android.OrgzlyTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RepoFactoryTest extends OrgzlyTest {

    @Test
    public void testCreateRepoFromUrl1() {
        SyncRepo repo = repoFactory.getFromUri(context, "dropbox:");
        assertTrue(repo instanceof DropboxRepo);
        assertEquals("dropbox:", repo.getUri().toString());
    }

    @Test
    public void testCreateRepoFromUrl2() {
        SyncRepo repo = repoFactory.getFromUri(context, "dropbox:/path");
        assertTrue(repo instanceof DropboxRepo);
        assertEquals("dropbox:/path", repo.getUri().toString());
    }

    @Test
    public void testCreateRepoFromUrl10() {
        SyncRepo repo = repoFactory.getFromUri(context, "mock://authority/path");
        assertTrue(repo instanceof MockRepo);
        assertEquals("mock://authority/path", repo.getUri().toString());
    }

    @Test
    public void testCreateRepoFromUrl100() {
        assertNull(repoFactory.getFromUri(context, "I am not a valid Uri"));
    }
}
