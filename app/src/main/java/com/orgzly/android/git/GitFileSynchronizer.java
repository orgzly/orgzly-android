package com.orgzly.android.git;

import android.content.Context;
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
        if (!(revision.equals(head) || revWalk.isMergedInto(revision, head))) {
            throw new IOException("The provided revision is not merged in to the current HEAD.");
        }
        retrieveLatestVersionOfFile(repositoryPath, destination);
    }

    public void retrieveLatestVersionOfFile(
            String repositoryPath, File destination) throws IOException {
        MiscUtils.copyFile(repoDirectoryFile(repositoryPath), destination);
    }

    public boolean mergeWithRemote(boolean leaveConflicts) throws IOException {
        ensureReposIsClean();
        try {
            transportSetter().setTransport(git.fetch().setRemote(preferences.remoteName())).call();
            RevCommit mergeTarget = getCommit(
                    String.format("%s/%s", preferences.remoteName(),
                            git.getRepository().getBranch()));
            return doMerge(mergeTarget, leaveConflicts);
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean mergeAndPushToRemote() throws IOException {
        boolean success = mergeWithRemote(false);
        if (success) try {
            transportSetter().setTransport(git.push().setRemote(preferences.remoteName())).call();
        } catch (GitAPIException e) {}
        return success;
    }

    public boolean updateAndCommitFileFromRevisionAndMerge(
            File sourceFile, String repositoryPath,
            ObjectId fileRevision, RevCommit revision, boolean leaveConflicts)
            throws IOException {
        ensureReposIsClean();
        if (updateAndCommitFileFromRevision(sourceFile, repositoryPath, fileRevision)) return true;

        String originalBranch = git.getRepository().getFullBranch();
        String mergeBranch = String.format("merge%s%s", repositoryPath, fileRevision.getName());
        Boolean mergeSucceeded = true;
        Boolean doCleanup = true;
        try {
            RevCommit mergeTarget = currentHead();
            git.checkout().setCreateBranch(true).setForce(true).
                    setStartPoint(revision).setName(mergeBranch).call();
            if (!updateAndCommitFileFromRevision(sourceFile, repositoryPath, fileRevision))
                throw new IOException(
                        String.format(
                                "The provided file revision %s for %s is not the same as the one found in the provided commit %s.",
                                fileRevision.toString(), repositoryPath, revision.toString()));
            mergeSucceeded = doMerge(mergeTarget, leaveConflicts);
            if (mergeSucceeded) {
                RevCommit merged = currentHead();
                git.checkout().setName(originalBranch).call();
                MergeResult result = git.merge().setFastForward(
                        MergeCommand.FastForwardMode.FF_ONLY).include(merged).call();
                if (!result.getMergeStatus().equals(MergeResult.MergeStatus.MERGED))
                    throw new IOException("Unexpected failure to fast forward.");
            }
        } catch (GitAPIException e) {
            doCleanup = true;
            e.printStackTrace();
            throw new IOException(String.format("Failed to handle merge correctly: %s", e.getMessage()));
        } finally {
            if (mergeSucceeded || doCleanup) try {
                git.checkout().setName(originalBranch).call();
                git.branchDelete().setBranchNames(mergeBranch);
            } catch (GitAPIException e) {
            }
        }
        return mergeSucceeded;
    }

    private boolean doMerge(RevCommit mergeTarget, boolean leaveConflicts) throws IOException {
        try {
            MergeResult result = git.merge().include(mergeTarget).call();
            if (result.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)) {
                if (!leaveConflicts) gitResetMerge();
                return false;
            }
        } catch (GitAPIException e) {
            throw new IOException("Failed to handle merge correctly");
        }
        return true;
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
        Log.i("temp", String.format(
                "Repository revision for %s is %s, current is %s. Equality is %s",
                repositoryPath, repositoryRevision.name(), revision.name(), repositoryRevision == revision));
        if (repositoryRevision.equals(revision)) {
            updateAndCommitFile(sourceFile, repositoryPath);
            return true;
        }
        return false;
    }

    private RevCommit updateAndCommitFile(
            File sourceFile, String repositoryPath) throws IOException {
        File destinationFile = repoDirectoryFile(repositoryPath);
        MiscUtils.copyFile(sourceFile, destinationFile);
        try {
            git.add().addFilepattern(repositoryPath).call();
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
        Ref head = git.getRepository().getRef(identifier);
        return new RevWalk(git.getRepository()).parseCommit(head.getObjectId());
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
            throw new IOException("Refusing to update because there are uncomitted changes.");
    }

    private File repoDirectoryFile(String filePath) {
        return new File(repoPath(), filePath);
    }

    public ObjectId getFileRevision(String pathString, RevCommit commit) throws IOException {
        Log.i("path", pathString);
        ObjectId objectId = TreeWalk.forPath(git.getRepository(), pathString, commit.getTree()).getObjectId(0);
        Log.i("temp", String.format("ID for %s is %s", pathString, objectId.toString()));
        return objectId;
    }
}
