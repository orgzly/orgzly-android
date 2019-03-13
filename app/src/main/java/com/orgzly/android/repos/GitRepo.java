package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.orgzly.android.BookName;
import com.orgzly.android.git.GitFileSynchronizer;
import com.orgzly.android.git.GitPreferences;
import com.orgzly.android.git.GitPreferencesFromRepoPrefs;
import com.orgzly.android.git.GitSSHKeyTransportSetter;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitRepo implements SyncRepo, TwoWaySyncRepo {
    public final static String SCHEME = "git";

    public static GitTransportSetter getTransportSetter(GitPreferences preferences) {
        return new GitSSHKeyTransportSetter(Uri.parse(preferences.sshKeyPathString()).getPath());
    }

    public static GitRepo buildFromIdAndUri(Context context, Long rid, Uri uri)
            throws IOException {
        GitPreferencesFromRepoPrefs prefs = new GitPreferencesFromRepoPrefs(
                new RepoPreferences(context, rid, uri));
        return build(prefs, false);
    }

    private static GitRepo build(GitPreferences prefs, boolean clone) throws IOException {
        Git git = ensureRepositoryExists(prefs, clone, null);

        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", prefs.remoteName(), "url", prefs.remoteUri().toString());
        config.save();

        return new GitRepo(git, prefs);
    }

    static boolean isRepo(FileRepositoryBuilder frb, File f) {
        frb.addCeilingDirectory(f).findGitDir(f);
        return frb.getGitDir() != null && frb.getGitDir().exists();
    }

    public static Git ensureRepositoryExists(
            GitPreferences prefs, boolean clone, ProgressMonitor pm) throws IOException {
        return ensureRepositoryExists(
                prefs.remoteUri(), new File(prefs.repositoryFilepath()),
                getTransportSetter(prefs), clone, pm);
    }

    public static Git ensureRepositoryExists(
            Uri repoUri, File directoryFile, GitTransportSetter transportSetter,
            boolean clone, ProgressMonitor pm)
            throws IOException {
        FileRepositoryBuilder frb = new FileRepositoryBuilder();
        if (!directoryFile.exists() || directoryFile.list().length == 0) { // An existing, empty directory is OK
            if (clone) {
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
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                    throw new IOException(
                            String.format("Failed to clone repository %s, %s", repoUri.toString(),
                                    e.getCause()));
                }
            } else {
                throw new IOException(
                        String.format("The file %s does not exist", directoryFile.toString()));
            }
        } else if (!isRepo(frb, directoryFile)) {
            throw new IOException(
                    String.format("Directory %s is not a git repository.",
                            directoryFile.getAbsolutePath()));
        }
        return new Git(frb.build());
    }

    private Git git;
    private GitFileSynchronizer synchronizer;
    private GitPreferences preferences;

    public GitRepo(Git g, GitPreferences prefs) {
        git = g;
        preferences = prefs;
        synchronizer = new GitFileSynchronizer(git, prefs);
    }

    public boolean requiresConnection() {
        return false;
    }

    public VersionedRook storeBook(File file, String fileName) throws IOException {
        synchronizer.addAndCommitNewFile(file, fileName);
        Uri uri = Uri.parse("/" + fileName);
        return currentVersionedRook(uri);
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
        synchronizer.mergeWithRemote();
        synchronizer.tryPushIfUpdated(currentCommit);
        synchronizer.safelyRetrieveLatestVersionOfFile(
                sourceUri.getPath(), destination, currentCommit);

        return currentVersionedRook(sourceUri);
    }

    private VersionedRook currentVersionedRook(Uri uri) {
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
        return new VersionedRook(getUri(), uri, commit.name(), mtime);
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
        synchronizer.deleteFileAndCommit(deleteUri);
    }

    public VersionedRook renameBook(Uri from, String name) throws IOException {
        return null;
    }

    @Override
    public TwoWaySyncResult syncBook(
            Uri uri, VersionedRook current, File fromDB) throws IOException {
        File writeBack = null;
        String fileName = uri.getPath();
        if (fileName.startsWith("/"))
            fileName = fileName.replaceFirst("/", "");
        boolean syncBackNeeded = false;
        if (current != null) {
            RevCommit rookCommit = getCommitFromRevisionString(current.getRevision());
            Log.i("Git", String.format("File name %s, rookCommit: %s", fileName, rookCommit));
            synchronizer.updateAndCommitFileFromRevisionAndMerge(
                    fromDB, fileName,
                    synchronizer.getFileRevision(fileName, rookCommit),
                    rookCommit);

            synchronizer.tryPushIfUpdated(rookCommit);

            syncBackNeeded = !synchronizer.fileMatchesInRevisions(
                    fileName, rookCommit, synchronizer.currentHead());
        } else {
            // TODO: Prompt user for confirmation?
            Log.w("Git", "Unable to find previous commit, loading from repository.");
            syncBackNeeded = true;
        }
        Log.i("Git", String.format("Sync back needed was %s", syncBackNeeded));
        writeBack = synchronizer.repoDirectoryFile(fileName);
        return new TwoWaySyncResult(
                currentVersionedRook(Uri.EMPTY.buildUpon().appendPath(fileName).build()),
                writeBack);
    }
}
