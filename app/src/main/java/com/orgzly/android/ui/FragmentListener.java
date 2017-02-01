package com.orgzly.android.ui;

/**
 * Common interface for most fragments' listeners.
 */
public interface FragmentListener {
    void announceChanges(
            String fragmentTag,
            CharSequence title,
            CharSequence subTitle,
            int selectionCount);
}
