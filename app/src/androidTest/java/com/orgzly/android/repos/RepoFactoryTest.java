package com.orgzly.android.repos;

import com.orgzly.BuildConfig;
import com.orgzly.android.OrgzlyTest;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RepoFactoryTest extends OrgzlyTest {

    @Test
    public void testCreateRepoFromUrl1() {
        Assume.assumeTrue(BuildConfig.IS_DROPBOX_ENABLED);

        SyncRepo repo = testUtils.repoInstance(RepoType.DROPBOX, "dropbox:");
        assertTrue(repo instanceof DropboxRepo);
        assertEquals("dropbox:", repo.getUri().toString());
    }

    @Test
    public void testCreateRepoFromUrl2() {
        Assume.assumeTrue(BuildConfig.IS_DROPBOX_ENABLED);

        SyncRepo repo = testUtils.repoInstance(RepoType.DROPBOX, "dropbox:/path");
        assertTrue(repo instanceof DropboxRepo);
        assertEquals("dropbox:/path", repo.getUri().toString());
    }

    @Test
    public void testCreateRepoFromUrl10() {
        SyncRepo repo = testUtils.repoInstance(RepoType.MOCK, "mock://authority/path");
        assertTrue(repo instanceof MockRepo);
        assertEquals("mock://authority/path", repo.getUri().toString());
    }

    @Test
    public void testInvalidDirectoryUrl() {
        try {
            testUtils.repoInstance(RepoType.DIRECTORY, "I am not a valid URL");
            fail("Invalid repo URL should throw exception");

        } catch (IllegalArgumentException e) {
            assertEquals("Missing file scheme in I am not a valid URL", e.getMessage());
        }
    }
}
