package com.orgzly.android.git;

import org.eclipse.jgit.api.TransportCommand;

public class HTTPTransportSetter implements GitTransportSetter {
    public HTTPTransportSetter() {}

    public TransportCommand setTransport(TransportCommand tc) {
        return tc;
    }
}
