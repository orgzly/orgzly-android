package com.orgzly.android.sync;

import android.content.Context;

import com.orgzly.android.BookFormat;
import com.orgzly.android.BookName;
import com.orgzly.android.db.entity.BookView;
import com.orgzly.android.db.entity.Repo;
import com.orgzly.android.repos.VersionedRook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for local and all remote books that share the same name.
 */
public class BookNamesake {
    private String name;

    /** Local book. */
    private BookView book;

    /** Remote versioned books. */
    private List<VersionedRook> versionedRooks = new ArrayList<>();

    /** Remote versioned books. */
    private List<VersionedRook> versionedRooksEncrypted = new ArrayList<>();
    /** Remote versioned books. */
    private List<VersionedRook> versionedRooksUnencrypted = new ArrayList<>();

    /** Current remote book that the local one is linking to. */
    private VersionedRook latestLinkedRook;

    private BookSyncStatus status;

    public BookNamesake(String name) {
        this.name = name;
    }

    /**
     * Create links between each local book and each remote book with the same name.
     */
    public static Map<String, BookNamesake> getAll(Context context, List<BookView> books, List<VersionedRook> versionedRooks) {
        Map<String, BookNamesake> namesakes = new HashMap<>();

        /* Create links from all local books first. */
        for (BookView book: books) {
            BookNamesake pair = new BookNamesake(book.getBook().getName());
            namesakes.put(book.getBook().getName(), pair);
            pair.setBook(book);
        }

        /* Set repo books. */
        for (VersionedRook book: versionedRooks) {
            String fileName = BookName.getFileName(context, book.getUri());
            String name = BookName.fromFileName(fileName).getName();

            BookNamesake pair = namesakes.get(name);
            if (pair == null) {
                /* Local file doesn't exists, create new pair. */
                pair = new BookNamesake(name);
                namesakes.put(name, pair);
            }

            boolean encrypted = BookName.fromFileName(BookName.getFileName(context, book.getUri())).getEncrypted();

            /* Add remote book. */
            pair.addRook(book, encrypted);
        }

        return namesakes;
    }

    public String getName() {
        return name;
    }

    public BookView getBook() {
        return book;
    }

    public void setBook(BookView book) {
        this.book = book;
    }

    // TODO: We are always using only the first element of this list
    public List<VersionedRook> getRooks() {
        return versionedRooks;
    }

    public void addRook(VersionedRook vrook, boolean encrypted) {
        this.versionedRooks.add(vrook);
        if (encrypted) {
            this.versionedRooksEncrypted.add(vrook);
        } else {
            this.versionedRooksUnencrypted.add(vrook);
        }
    }

    public BookSyncStatus getStatus() {
        return status;
    }

    public VersionedRook getLatestLinkedRook() {
        return latestLinkedRook;
    }

    public void setLatestLinkedRook(VersionedRook linkedVersionedRook) {
        this.latestLinkedRook = linkedVersionedRook;
    }

    public String toString() {
        return "[" + this.getClass().getSimpleName() + " " + name +
                " | " + status +
                " | Local:" + (book != null ? book : "N/A") +
                " | Remotes:" + versionedRooks.size() + "]";
    }


    /**
     * States to consider:
     *
     * - Book exists
     *   - Book is dummy
     *   - Book has a link
     *     - Linked remote book exists
     *   - Book has a last-synced-with remote book
     * - Remote book exists
     */
    /* TODO: Case: Remote book deleted? */
    public void updateStatus(Context context, int reposCount) {
        /* Sanity check. Group's name must come from somewhere - local or remote books. */
        if (book == null && versionedRooks.isEmpty()) {
            throw new IllegalStateException("BookNameGroup does not contain any books");
        }

        if (book == null) {
            /* Remote books only */

            if (versionedRooks.size() == 1) {
                if (versionedRooksUnencrypted.size() == 1) {
                    status = BookSyncStatus.NO_BOOK_ONE_ROOK;
                } else {
                    status = BookSyncStatus.NO_BOOK_ONE_ROOK_ENCRYPTED;
                }
            } else {
                status = BookSyncStatus.NO_BOOK_MULTIPLE_ROOKS;
            }

            return;

        } else if (versionedRooks.isEmpty()) {
            /* Local book only */

            if (book.getBook().isDummy()) { /* Only dummy exists. */
                status = BookSyncStatus.ONLY_DUMMY;

            } else {
                if (book.hasLink()) { /* Only local book with a link. */
                    status = BookSyncStatus.ONLY_BOOK_WITH_LINK;

                } else { /* Only local book without link. */
                    if (reposCount > 1) {
                        status = BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS;
                    } else {
                        status = BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO;
                    } // TODO: What about no repos?
                }
            }

            return;
        }

        /* Both local book and one or more remote books exist at this point ... */

        if (book.hasLink()) { // Book has link set.

            VersionedRook latestLinkedRookEncrypted = getLatestLinkedRookVersion(book, versionedRooksEncrypted);
            VersionedRook latestLinkedRookUnencrypted = getLatestLinkedRookVersion(book, versionedRooksUnencrypted);

//            if (latestLinkedRookEncrypted != null && latestLinkedRookUnencrypted != null) {
//                status = BookSyncStatus.CONFLICT_BOTH_ENCRYPTED_AND_UNENCRYPTED_ROOK_EXIST;
//                return;
//            }

            /* Fallback */
//            if (latestLinkedRook == null) { // todo don't go through all again
//                getLatestLinkedRookVersion(book, versionedRooks);
//            }

            if (latestLinkedRookEncrypted == null && latestLinkedRookUnencrypted == null) {
                /* Both local and remote book exist with the same name.
                 * Book has a link, however that link is not pointing to an existing remote book.
                 */
                status = BookSyncStatus.BOOK_WITH_LINK_AND_ROOK_EXISTS_BUT_LINK_POINTING_TO_DIFFERENT_ROOK;
                // TODO: So what's the problem? Just save it then? But can we just overwrite whatever is link pointing too?
                return;
            }
//            else if (book.hasEncryption() && latestLinkedRookEncrypted == null) {
//                status = BookSyncStatus.BOOK_ENCRYPTED_WITH_LINK_AND_ONLY_UNENCRYPTED_ROOK_EXISTS;
//                return;
//            } else if (!book.hasEncryption() && latestLinkedRookUnencrypted == null) {
//                status = BookSyncStatus.BOOK_UNENCRYPTED_WITH_LINK_AND_ONLY_ENCRYPTED_ROOK_EXISTS;
//                return;
//            }
            VersionedRook latestLinkedRook = getLatestLinkedRookVersion(book, versionedRooks);
//            if (book.hasEncryption()) {
//                latestLinkedRook = latestLinkedRookEncrypted;
//            } else {
//                latestLinkedRook = latestLinkedRookUnencrypted;
//            }
            setLatestLinkedRook(latestLinkedRook);


            if (book.getBook().isDummy()) {
                if (versionedRooksUnencrypted.size() == 1) {
                    status = BookSyncStatus.DUMMY_WITH_LINK;
                } else {
                    status = BookSyncStatus.DUMMY_WITH_LINK_ENCRYPTED;
                }
                return;
            }

            if (book.getSyncedTo() == null) {
                status = BookSyncStatus.CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE;
                return;
            }

            boolean bookEncryption = book.hasEncryption();
            boolean syncedEncryption = BookName.fromFileName(BookName.getFileName(context, book.getSyncedTo().getUri())).getEncrypted();
            if (bookEncryption != syncedEncryption) {
                /* First sync after encryption toggle on previously synced book. */

                String toBeSyncedFilename = BookName.fileName(name, BookFormat.ORG, bookEncryption);

                // todo if ((bookEncryption && latestLinkedRookEncrypted != null) || unenc && unenc) already_exists

                for (VersionedRook vrook : versionedRooks) {
                    if (BookName.getFileName(context, vrook.getUri()).equals(toBeSyncedFilename)){
                        status = BookSyncStatus.CONFLICT_ENCRYPTION_TOGGLED_AND_TARGET_ROOK_EXISTS;
                        return;
                    }
                }

                status = BookSyncStatus.BOOK_WITH_LINK_ENCRYPTION_TOGGLED_INITIAL_PUSH;
                return;
            }

            if (! book.getSyncedTo().getUri().equals(latestLinkedRook.getUri())) {
                status = BookSyncStatus.CONFLICT_LAST_SYNCED_ROOK_AND_LATEST_ROOK_ARE_DIFFERENT;
                return;
            }



            /* Same revision, there was no remote change. */
            if (book.getSyncedTo().getRevision().equals(latestLinkedRook.getRevision())) {
                /* Revision did not change. */

                if (book.isOutOfSync()) { // Local change
                    status = BookSyncStatus.BOOK_WITH_LINK_LOCAL_MODIFIED;
                } else {
                    status = BookSyncStatus.NO_CHANGE;
                }

            } else { /* Remote book has been modified. */
                if (book.isOutOfSync()) { // Local change
                    status = BookSyncStatus.CONFLICT_BOTH_BOOK_AND_ROOK_MODIFIED;
                } else {
                    if (versionedRooksUnencrypted.size() == 1) {
                        status = BookSyncStatus.BOOK_WITH_LINK_AND_ROOK_MODIFIED;
                    } else {
                        status = BookSyncStatus.BOOK_WITH_LINK_AND_ROOK_MODIFIED_ENCRYPTED;
                    }
                }
            }

        } else { /* Local book without link. */

            if (! book.getBook().isDummy()) {
                status = BookSyncStatus.BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST;

            } else {
                if (versionedRooks.size() == 1) {
                    // todo ?maybe two statuses encrypted/unencrypted
                    if (versionedRooksUnencrypted.size() == 1) {
                        status = BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK;
                    } else {
                        status = BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK_ENCRYPTED;
                    }
                } else {
                    status = BookSyncStatus.DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS;
                }
            }
        }
    }

    /** Find latest (current) remote book that local one links to. */
    private VersionedRook getLatestLinkedRookVersion(BookView bookView, List<VersionedRook> vrooks) {
        Repo linkRepo = bookView.getLinkRepo();

        VersionedRook latestRook = null;

        if (linkRepo != null) {
            long maxMTime = 0;
            for (VersionedRook vrook : vrooks) {
                if (linkRepo.getUrl().equals(vrook.getRepoUri().toString())){
                    if (vrook.getMtime() > maxMTime) {
                        maxMTime = vrook.getMtime();
                        latestRook = vrook;
                    }
                }
            }
        }

        return latestRook;
    }
}
