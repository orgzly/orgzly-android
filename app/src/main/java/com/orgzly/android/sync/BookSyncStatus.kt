package com.orgzly.android.sync

// TODO: Write tests for *all* cases.
enum class BookSyncStatus {
    NO_CHANGE,

    BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST,
    DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS,
    NO_BOOK_MULTIPLE_ROOKS, // TODO: This should never be the case, as we already add dummy (dummy = there was no book)
    ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS,
    BOOK_WITH_LINK_AND_ROOK_EXISTS_BUT_LINK_POINTING_TO_DIFFERENT_ROOK,
    BOOK_ENCRYPTED_WITH_LINK_AND_ONLY_UNENCRYPTED_ROOK_EXISTS,
    BOOK_UNENCRYPTED_WITH_LINK_AND_ONLY_ENCRYPTED_ROOK_EXISTS,
    ONLY_DUMMY,
    ROOK_AND_VROOK_HAVE_DIFFERENT_REPOS,

    /* Conflict. */
    CONFLICT_BOTH_BOOK_AND_ROOK_MODIFIED,
    CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE,
    CONFLICT_LAST_SYNCED_ROOK_AND_LATEST_ROOK_ARE_DIFFERENT,
    CONFLICT_ENCRYPTION_TOGGLED_AND_TARGET_ROOK_EXISTS,
    CONFLICT_BOTH_ENCRYPTED_AND_UNENCRYPTED_ROOK_EXIST,

    /* Book can be loaded. */
    NO_BOOK_ONE_ROOK, // TODO: Can this happen? We always load dummy.
    NO_BOOK_ONE_ROOK_ENCRYPTED, // TODO: Can this happen? We always load dummy.
    DUMMY_WITHOUT_LINK_AND_ONE_ROOK,
    DUMMY_WITHOUT_LINK_AND_ONE_ROOK_ENCRYPTED,
    BOOK_WITH_LINK_AND_ROOK_MODIFIED,
    BOOK_WITH_LINK_AND_ROOK_MODIFIED_ENCRYPTED,
    DUMMY_WITH_LINK,
    DUMMY_WITH_LINK_ENCRYPTED,

    /* Book can be saved. */
    ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO,
    BOOK_WITH_LINK_LOCAL_MODIFIED,
    ONLY_BOOK_WITH_LINK,
    BOOK_WITH_LINK_ENCRYPTION_TOGGLED_INITIAL_PUSH;

    // TODO: Extract string resources
    @JvmOverloads
    fun msg(arg: Any = ""): String {
        when (this) {
            NO_CHANGE ->
                return "No change"

            BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST ->
                return "Notebook has no link and one or more remote notebooks with the same name exist"

            DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS ->
                return "Notebook has no link and multiple remote notebooks with the same name exist"

            NO_BOOK_MULTIPLE_ROOKS ->
                return "No notebook and multiple remote notebooks with the same name exist"

            ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS ->
                return "Notebook has no link and multiple repositories exist"

            BOOK_WITH_LINK_AND_ROOK_EXISTS_BUT_LINK_POINTING_TO_DIFFERENT_ROOK ->
                return "Notebook has link and remote notebook with the same name exists, but link is pointing to a different remote nuotebook which does not exist"

            BOOK_ENCRYPTED_WITH_LINK_AND_ONLY_UNENCRYPTED_ROOK_EXISTS ->
                return "Notebook has link and remote notebook with the same name exists, but local book is marked as encrypted and remote book is unencrypted"

            BOOK_UNENCRYPTED_WITH_LINK_AND_ONLY_ENCRYPTED_ROOK_EXISTS ->
                return "Notebook has link and remote notebook with the same name exists, but local book is marked as unencrypted and remote book is encrypted"

            ONLY_DUMMY ->
                return "Only local dummy exists"

            ROOK_AND_VROOK_HAVE_DIFFERENT_REPOS ->
                return "Linked and synced notebooks have different repositories"

            CONFLICT_BOTH_BOOK_AND_ROOK_MODIFIED ->
                return "Both local and remote notebook have been modified"

            CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE ->
                return "Link and remote notebook exist but notebook hasn't been synced before"

            CONFLICT_LAST_SYNCED_ROOK_AND_LATEST_ROOK_ARE_DIFFERENT ->
                return "Last synced notebook and latest remote notebook differ"

            CONFLICT_ENCRYPTION_TOGGLED_AND_TARGET_ROOK_EXISTS ->
                return "Notebook encryption was toggled but a remote book already exists in the place of the target remote book that would be newly created"

            CONFLICT_BOTH_ENCRYPTED_AND_UNENCRYPTED_ROOK_EXIST ->
                return "Both encrypted and unencrypted versions of the remote book exist"

            NO_BOOK_ONE_ROOK, NO_BOOK_ONE_ROOK_ENCRYPTED, DUMMY_WITHOUT_LINK_AND_ONE_ROOK, DUMMY_WITHOUT_LINK_AND_ONE_ROOK_ENCRYPTED, BOOK_WITH_LINK_AND_ROOK_MODIFIED, BOOK_WITH_LINK_AND_ROOK_MODIFIED_ENCRYPTED, DUMMY_WITH_LINK, DUMMY_WITH_LINK_ENCRYPTED ->
                return "Loaded from $arg"

            ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO, BOOK_WITH_LINK_LOCAL_MODIFIED, ONLY_BOOK_WITH_LINK, BOOK_WITH_LINK_ENCRYPTION_TOGGLED_INITIAL_PUSH ->
                return "Saved to $arg"

            else ->
                throw IllegalArgumentException("Unknown sync status " + this)
        }
    }
}
