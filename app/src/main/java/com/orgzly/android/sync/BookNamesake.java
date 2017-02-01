package com.orgzly.android.sync;

import android.content.Context;

import com.orgzly.android.Book;
import com.orgzly.android.BookName;
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
    private Book book;

    /** Remote versioned books. TODO: Is this even used? We don't want to support 1->many links, it would be too confusing. */
    private List<VersionedRook> versionedRooks = new ArrayList<>();

    /** Current remote book that the local one is linking to. */
    private VersionedRook latestLinkedRook;

    private BookSyncStatus status;

    public BookNamesake(String name) {
        this.name = name;
    }

    /**
     * Create links between each local book and each remote book with the same name.
     */
    public static Map<String, BookNamesake> getAll(Context context, List<Book> books, List<VersionedRook> versionedRooks) {
        Map<String, BookNamesake> namesakes = new HashMap<>();

        /* Create links from all local books first. */
        for (Book book: books) {
            BookNamesake pair = new BookNamesake(book.getName());
            namesakes.put(book.getName(), pair);
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

            /* Add remote book. */
            pair.addRook(book);
        }

        return namesakes;
    }

    public String getName() {
        return name;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    // TODO: We are always using only the first element of this list
    public List<VersionedRook> getRooks() {
        return versionedRooks;
    }

    public void addRook(VersionedRook vrook) {
        this.versionedRooks.add(vrook);
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
    public void updateStatus(int reposCount) {
        /* Sanity check. Group's name must come from somewhere - local or remote books. */
        if (book == null && versionedRooks.size() == 0) {
            throw new IllegalStateException("BookNameGroup does not contain any books");
        }

        if (book == null) {
            /* Remote books only */

            if (versionedRooks.size() == 1) {
                status = BookSyncStatus.NO_BOOK_ONE_ROOK;
            } else {
                status = BookSyncStatus.NO_BOOK_MULTIPLE_ROOKS;
            }

            return;

        } else if (versionedRooks.size() == 0) {
            /* Local book only */

            if (book.isDummy()) { /* Only dummy exists. */
                status = BookSyncStatus.ONLY_DUMMY;

            } else {
                if (book.getLink() != null) { /* Only local book with a link. */
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

        if (book.getLink() != null) { // Book has link set.

            VersionedRook latestLinkedRook = getLatestLinkedRookVersion(book, versionedRooks);

            if (latestLinkedRook == null) {
                /* Both local and remote book exist with the same name.
                 * Book has a link, however that link is not pointing to an existing remote book.
                 */
                status = BookSyncStatus.BOOK_WITH_LINK_AND_ROOK_EXISTS_BUT_LINK_POINTING_TO_DIFFERENT_ROOK;
                // TODO: So what's the problem? Just save it then? But can we just overwrite whatever is link pointing too?
                return;
            }
            setLatestLinkedRook(latestLinkedRook);


            if (book.isDummy()) {
                status = BookSyncStatus.DUMMY_WITH_LINK;
                return;
            }

            if (book.getLastSyncedToRook() == null) {
                status = BookSyncStatus.CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE;
                return;
            }

            if (! book.getLastSyncedToRook().getUri().equals(latestLinkedRook.getUri())) {
                status = BookSyncStatus.CONFLICT_LAST_SYNCED_ROOK_AND_LATEST_ROOK_ARE_DIFFERENT;
                return;
            }

            /* Same revision, there was no remote change. */
            // TODO: We get difference even if the file content is identical - if mtimes (revisions) are different - do compare content too in that case. Size first for speed.
            if (book.getLastSyncedToRook().getRevision().equals(latestLinkedRook.getRevision())) {
                /* Revision did not change. */

                if (book.isModifiedAfterLastSync()) { // Local change.
                    status = BookSyncStatus.BOOK_WITH_LINK_LOCAL_MODIFIED;
                } else {
                    status = BookSyncStatus.NO_CHANGE;
                }

            } else { /* Remote book has been modified. */
                if (book.isModifiedAfterLastSync()) {
                    /* Uh oh. Both local and remote modified. */
                    status = BookSyncStatus.CONFLICT_BOTH_BOOK_AND_ROOK_MODIFIED;
                } else {
                    status = BookSyncStatus.BOOK_WITH_LINK_AND_ROOK_MODIFIED;
                }
            }

        } else { /* Local book without link. */

            if (! book.isDummy()) {
                status = BookSyncStatus.BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST;

            } else {
                if (versionedRooks.size() == 1) {
                    status = BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK;
                } else {
                    status = BookSyncStatus.DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS;
                }
            }
        }
    }

    /** Find latest (current) remote book that local one links to. */
    private VersionedRook getLatestLinkedRookVersion(Book book, List<VersionedRook> vrooks) {
        for (VersionedRook vrook : vrooks) {
            if (book.getLink().getUri().equals(vrook.getUri())) {
                return vrook;
            }
        }

        return null;
    }
}
