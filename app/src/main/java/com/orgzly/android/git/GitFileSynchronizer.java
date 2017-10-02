package com.orgzly.android.git;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.orgzly.android.App;
import com.orgzly.android.repos.GitRepo;
import com.orgzly.android.util.MiscUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;

public class GitFileSynchronizer {
    private Git git;
    private GitPreferences preferences;

    public GitFileSynchronizer(Git g, GitPreferences prefs) {
        git = g;
        preferences = prefs;
    }

    private GitTransportSetter transportSetter() {
        return GitRepo.getTransportSetter(preferences);
    }

    public void safelyRetrieveLatestVersionOfFile(
            String repositoryPath, File destination, RevCommit revision) throws IOException {
        RevWalk revWalk = new RevWalk(git.getRepository());
        RevCommit head = currentHead();
        RevCommit rHead = revWalk.parseCommit(head.toObjectId());
        RevCommit rRevision = revWalk.parseCommit(revision.toObjectId());
        if (!revWalk.isMergedInto(rRevision, rHead)) {
            throw new IOException(
                    String.format(
                            "The provided revision %s is not merged in to the current HEAD, %s.",
                            revision, head));
        }
        retrieveLatestVersionOfFile(repositoryPath, destination);
    }

    public void retrieveLatestVersionOfFile(
            String repositoryPath, File destination) throws IOException {
        MiscUtils.copyFile(repoDirectoryFile(repositoryPath), destination);
    }

    private void fetch() throws GitAPIException {
        transportSetter().setTransport(git.fetch().setRemote(preferences.remoteName())).call();
    }

    public void checkoutSelected() throws GitAPIException {
        git.checkout().setName(preferences.branchName()).call();
    }

    public boolean mergeWithRemote() throws IOException {
        ensureReposIsClean();
        try {
            fetch();
            RevCommit mergeTarget = getCommit(
                    String.format("%s/%s", preferences.remoteName(),
                            git.getRepository().getBranch()));
            return doMerge(mergeTarget);
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean mergeAndPushToRemote() throws IOException {
        boolean success = mergeWithRemote();
        if (success) try {
            transportSetter().setTransport(git.push().setRemote(preferences.remoteName())).call();
        } catch (GitAPIException e) {}
        return success;
    }

    public void updateAndCommitFileFromRevision(
            File sourceFile, String repositoryPath,
            ObjectId fileRevision, RevCommit revision) throws IOException {
        ensureReposIsClean();
        if (updateAndCommitFileFromRevision(sourceFile, repositoryPath, fileRevision))
            return;

    }

    public boolean updateAndCommitFileFromRevisionAndMerge(
            File sourceFile, String repositoryPath,
            ObjectId fileRevision, RevCommit revision)
            throws IOException {
        ensureReposIsClean();
        if (updateAndCommitFileFromRevision(sourceFile, repositoryPath, fileRevision)) return true;

        Log.i("Temp", "Attempting setback merge");
        String originalBranch = git.getRepository().getFullBranch();
        String mergeBranch = String.format("merge%s%s", repositoryPath, fileRevision.getName());
        try {
            git.branchDelete().setBranchNames(mergeBranch).call();
        } catch (GitAPIException e) {}
        Boolean mergeSucceeded = true;
        Boolean doCleanup = true;
        try {
            RevCommit mergeTarget = currentHead();
            git.checkout().setCreateBranch(true).setForce(true).
                    setStartPoint(revision).setName(mergeBranch).call();
            if (!currentHead().equals(revision))
                throw new IOException("Unable to set revision to " + revision.toString());
            if (!updateAndCommitFileFromRevision(sourceFile, repositoryPath, fileRevision))
                throw new IOException(
                        String.format(
                                "The provided file revision %s for %s is " +
                                        "not the same as the one found in the provided commit %s.",
                                fileRevision.toString(), repositoryPath, revision.toString()));
            mergeSucceeded = doMerge(mergeTarget);
            if (mergeSucceeded) {
                RevCommit merged = currentHead();
                git.checkout().setName(originalBranch).call();
                MergeResult result = git.merge().include(merged).call();
                if (!currentHead().equals(merged))
                    throw new IOException("Unexpected failure to merge branch");
            }
        } catch (GitAPIException e) {
            doCleanup = true;
            e.printStackTrace();
            throw new IOException(
                    String.format("Failed to handle merge correctly: %s", e.getMessage()));
        } finally {
            if (mergeSucceeded || doCleanup) try {
                git.checkout().setName(originalBranch).call();
                git.branchDelete().setBranchNames(mergeBranch);
            } catch (GitAPIException e) {
            }
        }
        return mergeSucceeded;
    }

    private boolean doMerge(RevCommit mergeTarget) throws IOException, GitAPIException {
        MergeResult result = git.merge().include(mergeTarget).call();
        if (result.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)) {
            gitResetMerge();
            return false;
        }
        return true;
    }

    public void tryPushIfUpdated(RevCommit commit) throws IOException {
        if (!commit.equals(currentHead())) {
            tryPush();
        }
    }

    public void tryPush() {
        final TransportCommand pushCommand = transportSetter().setTransport(
                git.push().setRemote(preferences.remoteName()));
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    pushCommand.call();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    private void gitResetMerge() throws IOException, GitAPIException {
        git.getRepository().writeMergeCommitMsg(null);
        git.getRepository().writeMergeHeads(null);
        git.reset().setMode(ResetCommand.ResetType.HARD).call();
    }

    public boolean updateAndCommitFileFromRevision(
            File sourceFile, String repositoryPath, ObjectId revision) throws IOException {
        ensureReposIsClean();
        ObjectId repositoryRevision = getFileRevision(repositoryPath, currentHead());
        if (repositoryRevision.equals(revision)) {
            updateAndCommitFile(sourceFile, repositoryPath);
            return true;
        }
        return false;
    }

    public void setBranchAndGetLatest() throws IOException {
        ensureReposIsClean();
        try {
            fetch();
            // TODO: XXX maybe:
            // checkoutSelected();
            RevCommit current = currentHead();
            RevCommit mergeTarget = getCommit(
                    String.format("%s/%s", preferences.remoteName(), preferences.branchName()));
            if (!doMerge(mergeTarget))
                throw new IOException(
                        String.format("Failed to merge %s and %s",
                                current.getName(), mergeTarget.getName()));
        } catch (GitAPIException e) {
            e.printStackTrace();
            throw new IOException("Failed to update from remote");
        }
    }

    private RevCommit updateAndCommitFile(
            File sourceFile, String repositoryPath) throws IOException {
        File destinationFile = repoDirectoryFile(repositoryPath);
        MiscUtils.copyFile(sourceFile, destinationFile);
        try {
            git.add().addFilepattern(repositoryPath).call();
            if (!gitRepoIsClean())
                commit(String.format("Orgzly update: %s", repositoryPath));
        } catch (GitAPIException e) {
            throw new IOException("Failed to commit changes.");
        }
        return currentHead();
    }

    private void commit(String message) throws GitAPIException {
        Context context = App.getAppContext();
        git.commit().setCommitter(
                preferences.getAuthor(),
                preferences.getEmail()).
                setMessage(message).call();
    }

    public RevCommit currentHead() throws IOException {
        return getCommit(Constants.HEAD);
    }

    public RevCommit getCommit(String identifier) throws IOException {
        Log.i("test", git.getRepository().getWorkTree().toString());
        Log.i("test", identifier);
        Ref target = git.getRepository().getRef(identifier);
        return new RevWalk(git.getRepository()).parseCommit(target.getObjectId());
    }

    public String repoPath() {
        return git.getRepository().getWorkTree().getAbsolutePath();
    }

    private boolean gitRepoIsClean() {
        try {
            Status status = git.status().call();
            return !status.hasUncommittedChanges();
        } catch (GitAPIException e) {
            return false;
        }
    }

    private void ensureReposIsClean() throws IOException {
        if (!gitRepoIsClean())
            throw new IOException("Refusing to update because there are uncommitted changes.");
    }

    private File repoDirectoryFile(String filePath) {
        return new File(repoPath(), filePath);
    }

    public ObjectId getFileRevision(String pathString, RevCommit commit) throws IOException {
        ObjectId objectId = TreeWalk.forPath(
                git.getRepository(), pathString, commit.getTree()).getObjectId(0);
        return objectId;
    }
}
