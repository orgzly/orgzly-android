package com.orgzly.android.sync;

// TODO: Write tests for *all* cases.
public enum BookSyncStatus {
    NO_CHANGE,

    BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST,
    DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS,
    NO_BOOK_MULTIPLE_ROOKS, // TODO: This should never be the case, as we already add dummy (dummy = there was no book)
    ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS,
    BOOK_WITH_LINK_AND_ROOK_EXISTS_BUT_LINK_POINTING_TO_DIFFERENT_ROOK,
    ONLY_DUMMY,
    ROOK_AND_VROOK_HAVE_DIFFERENT_REPOS,

    /* Conflict. */
    CONFLICT_BOTH_BOOK_AND_ROOK_MODIFIED,
    CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE,
    CONFLICT_LAST_SYNCED_ROOK_AND_LATEST_ROOK_ARE_DIFFERENT,

    /* Book can be loaded. */
    NO_BOOK_ONE_ROOK, // TODO: Can this happen? We always load dummy.
    DUMMY_WITHOUT_LINK_AND_ONE_ROOK,
    BOOK_WITH_LINK_AND_ROOK_MODIFIED,
    DUMMY_WITH_LINK,

    /* Book can be saved. */
    ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO,
    BOOK_WITH_LINK_LOCAL_MODIFIED,
    ONLY_BOOK_WITH_LINK;

    public String msg() {
        return msg("");
    }

    // TODO: Extract string resources
    public String msg(Object arg) {
        switch (this) {
            case NO_CHANGE:
                return "No change";

            case BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST:
                return "Notebook has no link and one or more remote notebooks with the same name exist";

            case DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS:
                return "Notebook has no link and multiple remote notebooks with the same name exist";

            case NO_BOOK_MULTIPLE_ROOKS:
                return "No notebook and multiple remote notebooks with the same name exist";

            case ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS:
                return "Notebook has no link and multiple repositories exist";

            case BOOK_WITH_LINK_AND_ROOK_EXISTS_BUT_LINK_POINTING_TO_DIFFERENT_ROOK:
                return "Notebook has link and remote notebook with the same name exists, but link is pointing to a different remote notebook which does not exist";

            case ONLY_DUMMY:
                return "Only local dummy exists";

            case ROOK_AND_VROOK_HAVE_DIFFERENT_REPOS:
                return "Linked and synced notebooks have different repositories";

            case CONFLICT_BOTH_BOOK_AND_ROOK_MODIFIED:
                return "Both local and remote notebook have been modified";

            case CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE:
                return "Link and remote notebook exist but notebook hasn't been synced before";

            case CONFLICT_LAST_SYNCED_ROOK_AND_LATEST_ROOK_ARE_DIFFERENT:
                return "Last synced notebook and latest remote notebook differ";

            case NO_BOOK_ONE_ROOK:
            case DUMMY_WITHOUT_LINK_AND_ONE_ROOK:
            case BOOK_WITH_LINK_AND_ROOK_MODIFIED:
            case DUMMY_WITH_LINK:
                return "Loaded from " + arg;

            case ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO:
            case BOOK_WITH_LINK_LOCAL_MODIFIED:
            case ONLY_BOOK_WITH_LINK:
                return "Saved to " + arg;

            default:
                throw new IllegalArgumentException("Unknown sync status " + this);
        }
    }
}
