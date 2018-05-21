package com.orgzly.android.git;

import org.eclipse.jgit.api.TransportCommand;

public interface GitTransportSetter {
    public TransportCommand setTransport(TransportCommand tc);
}
