package com.orgzly.android.ui.fragments;

import android.net.Uri;

public interface FileBrowserOpener {
    // XXX: fragmentId is used here to deal with fragments being taken out of memory
    void browseDirectory(Uri uri, BrowserResultHandler resultHandler, boolean allowFileSelection);
    interface BrowserResultHandler {
        void handleBrowseResult(Uri uri);
    }
}
