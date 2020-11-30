package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.orgzly.android.BookName;
import com.orgzly.android.db.entity.Repo;
import com.orgzly.android.git.GitFileSynchronizer;
import com.orgzly.android.git.GitPreferences;
import com.orgzly.android.git.GitPreferencesFromRepoPrefs;
import com.orgzly.android.git.GitTransportSetter;
import com.orgzly.android.prefs.RepoPreferences;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitRepo implements SyncRepo, TwoWaySyncRepo {
    private final long repoId;

    /**
     * Used as cause when we try to clone into a non-empty directory
     */
    public static class DirectoryNotEmpty extends Exception {
        public File dir;

        DirectoryNotEmpty(File dir) {
            this.dir = dir;
        }
    }

    public static GitRepo getInstance(RepoWithProps props, Context context) throws IOException {
        // TODO: This doesn't seem to be implemented in the same way as WebdavRepo.kt, do
        //  we want to store configuration data the same way they do?
        Repo repo = props.getRepo();
        Uri repoUri = Uri.parse(repo.getUrl());
        RepoPreferences repoPreferences = new RepoPreferences(context, repo.getId(), repoUri);
        GitPreferencesFromRepoPrefs prefs = new GitPreferencesFromRepoPrefs(repoPreferences);

        // TODO: Build from info

        return build(props.getRepo().getId(), prefs, false);
    }

    private static GitRepo build(long id, GitPreferences prefs, boolean clone) throws IOException {
        Git git = ensureRepositoryExists(prefs, clone, null);

        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", prefs.remoteName(), "url", prefs.remoteUri().toString());
        config.save();

        return new GitRepo(id, git, prefs);
    }

    static boolean isRepo(FileRepositoryBuilder frb, File f) {
        frb.addCeilingDirectory(f).findGitDir(f);
        return frb.getGitDir() != null && frb.getGitDir().exists();
    }

    public static Git ensureRepositoryExists(
            GitPreferences prefs, boolean clone, ProgressMonitor pm) throws IOException {
        return ensureRepositoryExists(
                prefs.remoteUri(), new File(prefs.repositoryFilepath()),
                prefs.createTransportSetter(), clone, pm);
    }

    public static Git ensureRepositoryExists(
            Uri repoUri, File directoryFile, GitTransportSetter transportSetter,
            boolean clone, ProgressMonitor pm)
            throws IOException {
        if (clone) {
            return cloneRepo(repoUri, directoryFile, transportSetter, pm);
        } else {
            return verifyExistingRepo(directoryFile);
        }
    }

    /**
     * Check that the given path contains a valid git repository
     * @param directoryFile the path to check
     * @return A Git repo instance
     * @throws IOException Thrown when either the directory doesnt exist or is not a git repository
     */
    private static Git verifyExistingRepo(File directoryFile) throws IOException {
        if (!directoryFile.exists()) {
            throw new IOException(String.format("The directory %s does not exist", directoryFile.toString()), new FileNotFoundException());
        }

        FileRepositoryBuilder frb = new FileRepositoryBuilder();
        if (!isRepo(frb, directoryFile)) {
            throw new IOException(
                    String.format("Directory %s is not a git repository.",
                            directoryFile.getAbsolutePath()));
        }
        return new Git(frb.build());
    }

    /**
     * Attempts to clone a git repository
     * @param repoUri Remote location of git repository
     * @param directoryFile Location to clone to
     * @param transportSetter Transport information
     * @param pm Progress reporting helper
     * @return A Git repo instance
     * @throws IOException Thrown when directoryFile doesn't exist or isn't empty. Also thrown
     * when the clone fails
     */
    private static Git cloneRepo(Uri repoUri, File directoryFile, GitTransportSetter transportSetter,
                      ProgressMonitor pm) throws IOException {
        if (!directoryFile.exists()) {
            throw new IOException(String.format("The directory %s does not exist", directoryFile.toString()), new FileNotFoundException());
        }

        // Using list() can be resource intensive if there's many files, but since we just call it
        // at the time of cloning once we should be fine for now
        if (directoryFile.list().length != 0) {
            throw new IOException(String.format("The directory must be empty"), new DirectoryNotEmpty(directoryFile));
        }

        try {
            CloneCommand cloneCommand = Git.cloneRepository().
                    setURI(repoUri.toString()).
                    setProgressMonitor(pm).
                    setDirectory(directoryFile);
            transportSetter.setTransport(cloneCommand);
            return cloneCommand.call();
        } catch (GitAPIException | JGitInternalException e) {
            try {
                FileUtils.delete(directoryFile, FileUtils.RECURSIVE);
                // This is done to show sensible error messages when trying to create a new git sync
                directoryFile.mkdirs();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            throw new IOException(
                    String.format("Failed to clone repository %s, %s", repoUri.toString(),
                            e.getCause().getMessage()), e.getCause());
        }
    }

    private Git git;
    private GitFileSynchronizer synchronizer;
    private GitPreferences preferences;

    public GitRepo(long id, Git g, GitPreferences prefs) {
        repoId = id;
        git = g;
        preferences = prefs;
        synchronizer = new GitFileSynchronizer(git, prefs);
    }

    public boolean isConnectionRequired() {
        return true;
    }

    @Override
    public boolean isAutoSyncSupported() {
        return true;
    }

    public VersionedRook storeBook(File file, String fileName) throws IOException {
        // If the file already exists it is because we're trying to force save a file
        File destination = synchronizer.repoDirectoryFile(fileName);
        if (destination.exists()) {
            synchronizer.updateAndCommitExistingFile(file, fileName);
        } else {
            synchronizer.addAndCommitNewFile(file, fileName);
        }
        synchronizer.tryPush();
        return currentVersionedRook(Uri.EMPTY.buildUpon().appendPath(fileName).build());
    }

    @Override
    public VersionedRook storeFile(File file, String pathInRepo, String fileName) throws IOException {
        throw new UnsupportedOperationException();
    }

    private RevWalk walk() {
        return new RevWalk(git.getRepository());
    }

    RevCommit getCommitFromRevisionString(String revisionString) throws IOException {
        return walk().parseCommit(ObjectId.fromString(revisionString));
    }

    @Override
    public VersionedRook retrieveBook(String fileName, File destination) throws IOException {

        // public VersionedRook retrieveBook(Uri sourceUri, File destinationFile)
        // FIXME: Interface changed, this will not work
        Uri sourceUri = Uri.parse(fileName);

        // FIXME: Removed current_versioned_rooks table, just get the list from remote
        // VersionedRook current = CurrentRooksClient.get(App.getAppContext(), getUri().toString(), sourceUri.toString());
        VersionedRook current = null;

        RevCommit currentCommit = null;
        if (current != null) {
            currentCommit = getCommitFromRevisionString(current.getRevision());
        }

        // TODO: consider
        // synchronizer.checkoutSelected();
        try {
            currentCommit = synchronizer.getLatestCommitOfFile(Uri.parse(fileName));
        } catch (GitAPIException ex) {
            throw new IOException("Error while retrieving latest commit of " + fileName, ex);
        }
        // TODO: What if we  can't merge here? Can that not happen?
        synchronizer.mergeWithRemote();
        synchronizer.tryPushIfUpdated(currentCommit);
        synchronizer.safelyRetrieveLatestVersionOfFile(
                sourceUri.getPath(), destination, currentCommit);

        return currentVersionedRook(sourceUri);
    }

    private VersionedRook currentVersionedRook(Uri uri) throws IOException {
        RevCommit commit = null;
        if (uri.toString().contains("%")) {
            uri = Uri.parse(Uri.decode(uri.toString()));
        }
        try {
            commit = synchronizer.getLatestCommitOfFile(uri);
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        long mtime = (long)commit.getCommitTime()*1000;
        return new VersionedRook(repoId, RepoType.GIT, getUri(), uri, commit.name(), mtime);
    }

    private IgnoreNode getIgnores() throws IOException {
        IgnoreNode ignores = new IgnoreNode();
        File ignoreFile = synchronizer.repoDirectoryFile(".orgzlyignore");
        if (ignoreFile.exists()) {
            FileInputStream in = new FileInputStream(ignoreFile);
            try {
                ignores.parse(in);
            } finally {
                in.close();
            }
        }
        return ignores;
    }

    public List<VersionedRook> getBooks() throws IOException {
        synchronizer.setBranchAndGetLatest();
        List<VersionedRook> result = new ArrayList<>();
        if (synchronizer.currentHead() == null) {
            return result;
        }

        TreeWalk walk = new TreeWalk(git.getRepository());
        walk.reset();
        walk.setRecursive(true);
        walk.addTree(synchronizer.currentHead().getTree());
        final IgnoreNode ignores = getIgnores();
        walk.setFilter(new TreeFilter() {
            @Override
            public boolean include(TreeWalk walker) throws MissingObjectException, IncorrectObjectTypeException, IOException {
                final FileMode mode = walker.getFileMode(0);
                final String filePath = walker.getPathString();
                final boolean isDirectory = mode == FileMode.TREE;
                return !(ignores.isIgnored(filePath, isDirectory) == IgnoreNode.MatchResult.IGNORED);
            }

            @Override
            public boolean shouldBeRecursive() {
                return true;
            }

            @Override
            public TreeFilter clone() {
                return this;
            }
        });
        while (walk.next()) {
            final FileMode mode = walk.getFileMode(0);
            final boolean isDirectory = mode == FileMode.TREE;
            final String filePath = walk.getPathString();
            if (isDirectory)
                continue;
            if (BookName.isSupportedFormatFileName(filePath))
                result.add(
                        currentVersionedRook(
                                Uri.withAppendedPath(Uri.EMPTY, walk.getPathString())));
        }
        return result;
    }

    public Uri getUri() {
        Log.i("Git", String.format("%s", preferences.remoteUri()));
        return preferences.remoteUri();
    }

    public void delete(Uri deleteUri) throws IOException {
        // FIXME: finish me
        throw new IOException("Don't do that");
    }

    public VersionedRook renameBook(Uri from, String name) throws IOException {
        return null;
    }

    @Override
    public TwoWaySyncResult syncBook(
            Uri uri, VersionedRook current, File fromDB) throws IOException {
        File writeBack = null;
        boolean onMainBranch = true;
        String fileName = uri.getPath();
        if (fileName.startsWith("/"))
            fileName = fileName.replaceFirst("/", "");
        boolean syncBackNeeded;
        if (current != null) {
            RevCommit rookCommit = getCommitFromRevisionString(current.getRevision());
            Log.i("Git", String.format("File name %s, rookCommit: %s", fileName, rookCommit));
            boolean merged = synchronizer.updateAndCommitFileFromRevisionAndMerge(
                    fromDB, fileName,
                    synchronizer.getFileRevision(fileName, rookCommit),
                    rookCommit);

            // We have attempted a merge. Are we back on the main branch, or still on a temp branch?
            onMainBranch = git.getRepository().getBranch().equals(preferences.branchName());

            if (merged && !onMainBranch) {
                onMainBranch = synchronizer.attemptReturnToMainBranch();
            }

            syncBackNeeded = !synchronizer.fileMatchesInRevisions(
                    fileName, rookCommit, synchronizer.currentHead());
        } else {
            // TODO: Prompt user for confirmation?
            Log.w("Git", "Unable to find previous commit, loading from repository.");
            syncBackNeeded = true;
        }
        Log.i("Git", String.format("Sync back needed was %s", syncBackNeeded));
        if (syncBackNeeded) {
            writeBack = synchronizer.repoDirectoryFile(fileName);
        }
        return new TwoWaySyncResult(
                currentVersionedRook(Uri.EMPTY.buildUpon().appendPath(fileName).build()), onMainBranch,
                writeBack);
    }

    public void tryPushIfHeadDiffersFromRemote() {
        synchronizer.tryPushIfHeadDiffersFromRemote();
    }
}
