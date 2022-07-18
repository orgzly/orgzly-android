package com.orgzly.android.git;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.orgzly.android.App;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.internal.transport.sshd.OpenSshServerKeyDatabase;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class GitSSHKeyTransportSetter implements GitTransportSetter {
    private final TransportConfigCallback configCallback;

    public GitSSHKeyTransportSetter(String pathToSSHKey) {

        SshSessionFactory factory = new SshdSessionFactory(null, null) {

            @Override
            public File getHomeDirectory() {
                return App.getAppContext().getFilesDir();
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            protected List<Path> getDefaultIdentities(File sshDir) {
                return Collections.singletonList(Paths.get(Uri.decode(pathToSSHKey)));
            }

            @Override
            protected String getDefaultPreferredAuthentications() {
                return "publickey";
            }

            @Override
            protected ServerKeyDatabase createServerKeyDatabase(@NonNull File homeDir,
                                                                @NonNull File sshDir) {
                // We override this method because we want to set "askAboutNewFile" to False.
                return new OpenSshServerKeyDatabase(false,
                        getDefaultKnownHostsFiles(sshDir));
            }
        };

        SshSessionFactory.setInstance(factory);

        // org.apache.sshd.common.config.keys.IdentityUtils freaks out if user.home is not set
        System.setProperty("user.home", App.getAppContext().getFilesDir().toString());

        configCallback = transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(factory);

        };
    }

    public TransportCommand setTransport(TransportCommand tc) {
        tc.setTransportConfigCallback(configCallback);
        tc.setCredentialsProvider(new SshCredentialsProvider());
        return tc;
    }
}