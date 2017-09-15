package com.orgzly.android.repos;

import android.net.Uri;
import com.orgzly.android.App;
import com.orgzly.android.provider.clients.CurrentRooksClient;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitRepo implements Repo {
    private final Uri gitUri;
    private Git git;
    private GitFileSynchronizer synchronizer;

    static boolean isRepo(FileRepositoryBuilder frb, File f) {
        frb.addCeilingDirectory(f).findGitDir(f);
        return frb.getGitDir() != null;
    }

    static Uri buildDirectoryUri(Uri gitUri) {
        String filepath = gitUri.getSchemeSpecificPart().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        return new Uri.Builder().scheme("file").path(filepath).build();
    }

    public static Git ensureRepositoryExists(Uri repoUri, File directoryFile) throws IOException {
        FileRepositoryBuilder frb = new FileRepositoryBuilder();
        if (!directoryFile.exists()) {
            try {
                return Git.cloneRepository().setURI(repoUri.getPath()).setDirectory(directoryFile).call();
            } catch (GitAPIException e) {
                throw new IOException("Failed to clone repository " + repoUri.toString());
            }
        } else if (!isRepo(frb, directoryFile)) {
            throw new IOException(
                    String.format("Directory %s is not a git repository.", directoryFile.getAbsolutePath()));
        }
        return new Git(frb.build());
    }

    public GitRepo(Git g, Uri rUri) {
        git = git;
        gitUri = rUri;
        synchronizer = new GitFileSynchronizer(git);
    }

    public boolean requiresConnection() {
        return false;
    }

    public VersionedRook storeBook(File file, String fileName) throws IOException{
        VersionedRook current = CurrentRooksClient.get(
                App.getAppContext(), Uri.withAppendedPath(gitUri, fileName).toString());
        RevCommit commit = getCommitFromRevisionString(current.getRevision());
        synchronizer.updateAndCommitFileFromRevisionAndMerge(
                file, fileName, synchronizer.getFileRevision(fileName, commit), commit, false);
        return currentVersionedRook(current);
    }

    private RevWalk walk() {
        return new RevWalk(git.getRepository());
    }

    RevCommit getCommitFromRevisionString(String revisionString) throws IOException {
        return walk().parseCommit(ObjectId.fromString(revisionString));
    }

    public VersionedRook retrieveBook(Uri sourceUri, File destinationFile) throws IOException {
        VersionedRook current = CurrentRooksClient.get(
                App.getAppContext(), gitUri.toString(), sourceUri.toString());

        synchronizer.safelyRetrieveLatestVersionOfFile(
                sourceUri.getPath(), destinationFile, getCommitFromRevisionString(current.getRevision()));

        return currentVersionedRook(current);
    }

    private VersionedRook currentVersionedRook(VersionedRook last) throws IOException {
        RevCommit newCommit = synchronizer.currentHead();
        return new VersionedRook(last, newCommit.toString(), newCommit.getCommitTime());
    }

    private VersionedRook currentVersionedRook(Uri uri) throws IOException {
        RevCommit newCommit = synchronizer.currentHead();
        return new VersionedRook(gitUri, uri, newCommit.toString(), newCommit.getCommitTime());
    }

    public List<VersionedRook> getBooks() throws IOException {
        List<VersionedRook> result = new ArrayList<>();
        TreeWalk walk = new TreeWalk(git.getRepository());
        walk.reset();
        walk.setRecursive(true);

        while (walk.next()) {
            final FileMode mode = walk.getFileMode(0);
            if (mode == FileMode.TREE)
                continue;
            result.add(
                    currentVersionedRook(Uri.withAppendedPath(Uri.EMPTY, walk.getPathString())));
        }
        return result;
    }


    public Uri getUri() {
        return gitUri;
    }

    public void delete(Uri deleteUri) throws IOException {
        // XXX: finish me
        throw new IOException("Don't do that");
    }

    public VersionedRook renameBook(Uri from, String name) throws IOException {
        return null;
    }

}
