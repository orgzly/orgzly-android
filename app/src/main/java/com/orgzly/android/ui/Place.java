package com.orgzly.android.ui;

/**
 * Various actions can be performed relative to the specific item.
 * E.g., new note can be created *under* another, pasted *below*, etc.
 */
public enum Place {
    ABOVE,
    UNDER,
    UNDER_AS_FIRST,
    BELOW,
    UNSPECIFIED
}
