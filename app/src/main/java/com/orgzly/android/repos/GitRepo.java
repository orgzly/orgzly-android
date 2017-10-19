package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.orgzly.android.App;
import com.orgzly.android.BookName;
import com.orgzly.android.git.GitFileSynchronizer;
import com.orgzly.android.git.GitPreferences;
import com.orgzly.android.git.GitPreferencesFromRepoPrefs;
import com.orgzly.android.git.GitSSHKeyTransportSetter;
import com.orgzly.android.git.GitTransportSetter;
import com.orgzly.android.prefs.RepoPreferences;
import com.orgzly.android.provider.clients.CurrentRooksClient;

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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GitRepo implements Repo, Repo.TwoWaySync {
    public final static String SCHEME = "git";

    public static GitTransportSetter getTransportSetter(GitPreferences preferences) {
        return new GitSSHKeyTransportSetter(Uri.parse(preferences.sshKeyPathString()).getPath());
    }

    public static GitRepo buildFromUri(Context context, Uri uri)
            throws IOException, URISyntaxException {
        GitPreferencesFromRepoPrefs prefs = new GitPreferencesFromRepoPrefs(
                RepoPreferences.fromUri(context, uri));
        return build(prefs, false);
    }

    public static GitRepo build(GitPreferences prefs, boolean clone) throws IOException {
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
        if (!directoryFile.exists()) {
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
        VersionedRook current = CurrentRooksClient.get(
                // TODO: get rid of "/" prefix needed here
                App.getAppContext(), getUri().toString(), "/" + fileName);
        RevCommit commit = getCommitFromRevisionString(current.getRevision());
        synchronizer.updateAndCommitFileFromRevision(
                file, fileName, synchronizer.getFileRevision(fileName, commit));
        synchronizer.tryPushIfUpdated(commit);
        return currentVersionedRook(Uri.EMPTY.buildUpon().appendPath(fileName).build());
    }

    private RevWalk walk() {
        return new RevWalk(git.getRepository());
    }

    RevCommit getCommitFromRevisionString(String revisionString) throws IOException {
        return walk().parseCommit(ObjectId.fromString(revisionString));
    }

    public VersionedRook retrieveBook(Uri sourceUri, File destinationFile) throws IOException {
        VersionedRook current = CurrentRooksClient.get(
                App.getAppContext(), getUri().toString(), sourceUri.toString());
        RevCommit currentCommit = null;
        if (current != null) {
            currentCommit = getCommitFromRevisionString(current.getRevision());
        }

        // TODO: consider
        // synchronizer.checkoutSelected();
        synchronizer.mergeWithRemote();
        synchronizer.tryPushIfUpdated(currentCommit);
        synchronizer.safelyRetrieveLatestVersionOfFile(
                sourceUri.getPath(), destinationFile, currentCommit);

        return currentVersionedRook(sourceUri);
    }

    private VersionedRook currentVersionedRook(Uri uri) throws IOException {
        RevCommit newCommit = synchronizer.currentHead();
        return new VersionedRook(getUri(), uri, newCommit.name(), newCommit.getCommitTime()*1000);
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
        // XXX: finish me
        throw new IOException("Don't do that");
    }

    public VersionedRook renameBook(Uri from, String name) throws IOException {
        return null;
    }

    @Override
    public VersionedRook syncBook(
            Uri uri, VersionedRook current, File fromDB, File destinationFile) throws IOException {
        String fileName = uri.getPath();
        if (fileName.startsWith("/"))
            fileName = fileName.replaceFirst("/", "");
        if (current != null) {
            RevCommit rookCommit = getCommitFromRevisionString(current.getRevision());
            RevCommit originalSyncHEAD = synchronizer.currentHead();
            Log.i("Git", String.format("File name %s, rookCommit: %s", fileName, rookCommit));
            synchronizer.updateAndCommitFileFromRevisionAndMerge(
                    fromDB, fileName,
                    synchronizer.getFileRevision(fileName, rookCommit),
                    rookCommit);

            if (!synchronizer.currentHead().equals(originalSyncHEAD)) {
                synchronizer.tryPushIfUpdated(rookCommit);
                synchronizer.safelyRetrieveLatestVersionOfFile(
                        fileName, destinationFile, rookCommit);
            }
        } else {
            // XXX/TODO: Prompt user for confirmation?
            Log.w("Git", "Unable to find previous commit, loading from repository.");
            synchronizer.retrieveLatestVersionOfFile(fileName, destinationFile);
        }
        return currentVersionedRook(Uri.EMPTY.buildUpon().appendPath(fileName).build());
    }
}
