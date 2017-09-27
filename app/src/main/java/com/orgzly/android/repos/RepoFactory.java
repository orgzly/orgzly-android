package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import com.orgzly.R;
import com.orgzly.android.git.GitPreferences;
import com.orgzly.android.git.GitSSHKeyTransportSetter;
import com.orgzly.android.git.GitTransportSetter;
import com.orgzly.android.prefs.RepoPreferences;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import com.orgzly.BuildConfig;

public class RepoFactory {
    public static Repo getFromUri(Context context, Uri uri) {
        return getFromUri(context, uri.toString());
    }

    // TODO: Better throw exception, not return null?
    public static Repo getFromUri(Context context, String uriString) {
        Uri uri = Uri.parse(uriString);

        if (uri != null && uri.getScheme() != null) { // Make sure uri is valid and has a scheme
            try {
                switch (uri.getScheme()) {
                    case ContentRepo.SCHEME:
                        return new ContentRepo(context, uri);

                    case DropboxRepo.SCHEME:
                        if (! BuildConfig.IS_DROPBOX_ENABLED) {
                            return null;
                        }

                        /* There should be no authority. */
                        if (uri.getAuthority() != null) {
                            return null;
                        }
                        return new DropboxRepo(context, uri);
                    case GitRepo.SCHEME:
                        return buildGitRepo(context, uri);
                    case DirectoryRepo.SCHEME:
                        return new DirectoryRepo(uriString, false);

                    case MockRepo.SCHEME:
                        return new MockRepo(context, uriString);
                    default:
                        return buildGitRepo(context, uri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    static GitRepo buildGitRepo(Context context, Uri uri) throws IOException, URISyntaxException, GitAPIException {
        GitPreferences prefs = new GitPreferences(RepoPreferences.fromUri(context, uri));

        Git git = ensureRepositoryExists(uri, new File(prefs.repositoryFilepath()));

        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", prefs.remoteName(), "url", uri.toString());
        config.save();

        return new GitRepo(uri, git, prefs);
    }

    static boolean isRepo(FileRepositoryBuilder frb, File f) {
        frb.addCeilingDirectory(f).findGitDir(f);
        return frb.getGitDir() != null;
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
}
