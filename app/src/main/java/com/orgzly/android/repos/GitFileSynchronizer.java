package com.orgzly.android.repos;

import android.content.Context;

import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.util.MiscUtils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
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
    private Context context;

    public GitFileSynchronizer(Git g, Context c) {
        git = g;
        context = c;
    }

    public void safelyRetrieveLatestVersionOfFile(
            String repositoryPath, File destination, RevCommit revision) throws IOException {
        RevWalk revWalk = new RevWalk(git.getRepository());
        if (!revWalk.isMergedInto(revision, currentHead())) {
            throw new IOException("The provided revision is not merged in to the current HEAD.");
        }
        retrieveLatestVersionOfFile(repositoryPath, destination);
    }

    private void retrieveLatestVersionOfFile(
            String repositoryPath, File destination) throws IOException {
        MiscUtils.copyFile(repoDirectoryFile(repositoryPath), destination);
    }

    public boolean mergeWithRemote(String remoteName, boolean leaveConflicts) throws IOException {
        ensureReposIsClean();
        try {
            git.fetch().setRemote(remoteName).call();
            RevCommit mergeTarget = getCommit(
                    String.format("%s/%s", remoteName, git.getRepository().getFullBranch()));
            return doMerge(mergeTarget, leaveConflicts);
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateAndCommitFileFromRevisionAndMerge(
            File sourceFile, String repositoryPath,
            ObjectId fileRevision, RevCommit revision, boolean leaveConflicts)
            throws IOException {
        ensureReposIsClean();
        if (updateAndCommitFileFromRevision(sourceFile, repositoryPath, fileRevision)) return true;

        String originalBranch = git.getRepository().getFullBranch();
        String mergeBranch = String.format("merge%s%s", repositoryPath, fileRevision.toString());
        Boolean mergeSucceeded = true;
        Boolean doCleanup = true;
        try {
            RevCommit mergeTarget = currentHead();
            git.checkout().setCreateBranch(true).setStartPoint(revision).setName(mergeBranch).call();
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
            throw new IOException("Failed to handle merge correctly");
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
        if (getFileRevision(repositoryPath) == revision) {
            updateAndCommitFile(sourceFile, repositoryPath);
            return true;
        }
        return false;
    }

    private ObjectId updateAndCommitFile(
            File sourceFile, String repositoryPath) throws IOException {
        File destinationFile = repoDirectoryFile(repositoryPath);
        MiscUtils.copyFile(sourceFile, destinationFile);
        try {
            git.add().addFilepattern(repositoryPath).call();
            commit(String.format("Orgzly update: %s", repositoryPath));
        } catch (GitAPIException e) {
            throw new IOException("Failed to commit changes.");
        }
        return getFileRevision(repositoryPath);
    }

    private void commit(String message) throws GitAPIException {
        git.commit().
                setCommitter(
                        AppPreferences.gitAuthor(context),
                        AppPreferences.gitEmail(context)).
                setMessage(message).call();
    }

    private RevCommit currentHead() throws IOException {
        return getCommit("HEAD");
    }

    private RevCommit getCommit(String identifier) throws IOException {
        Ref head = git.getRepository().exactRef(identifier);
        return new RevWalk(git.getRepository()).parseCommit(head.getObjectId());
    }

    public String repoPath() {
        return git.getRepository().getDirectory().getAbsolutePath();
    }

    private boolean gitRepoIsClean() {
        try {
            Status status = git.status().call();
            return status.hasUncommittedChanges();
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

    public ObjectId getFileRevision(String pathString) throws IOException {
        TreeWalk walk = TreeWalk.forPath(
                git.getRepository(),
                pathString,
                git.getRepository().resolve(Constants.HEAD));
        return walk.getObjectId(0);
    }
}
