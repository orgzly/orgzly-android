package com.orgzly.android;

import com.orgzly.android.repos.Rook;
import com.orgzly.android.repos.VersionedRook;
import com.orgzly.android.sync.BookSyncStatus;
import com.orgzly.org.OrgFileSettings;

/**
 *
 */
public class Book {
    private long id;

    /**
     * Dummy book added while syncing, if there's no local book with the name and remotes exist.
     */
    private boolean isDummy;

    /**
     * Name of the book.
     * Currently unique. Does not include extension,
     * as format is only relevant when importing, exporting, syncing etc.
     */
    private String name;

    /** Last modified time. */
    private long modificationTime;

    /** Link to remote book. */
    private Rook rook;

    /** Remote book that this book has been synced to last. */
    private VersionedRook lastSyncedToRook;

    private String detectedEncoding;
    private String selectedEncoding;
    private String usedEncoding;

    /** Book preface - any text before the first heading. */
    private String preface;

    private BookSyncStatus syncStatus;

    private BookAction lastAction;

    private OrgFileSettings orgFileSettings = new OrgFileSettings();

    /**
     * Returns either notebook's #+TITLE or file name.
     */
    public static String getFragmentTitleForBook(Book book) {
        String str = null;

        if (book != null) {
            if (book.getOrgFileSettings().getTitle() != null) {
                str = book.getOrgFileSettings().getTitle();
            } else {
                str = book.getName();
            }
        }

        return str;
    }

    /**
     * Returns book's file name if #+TITLE is set, null otherwise.
     */
    public static String getFragmentSubtitleForBook(Book book) {
        String str = null;

        if (book != null) {
            if (book.getOrgFileSettings().getTitle() != null) {
                str = book.getName();
            }
        }

        return str;
    }

    public Book(String name) {
        this(name, "", System.currentTimeMillis(), false);
    }

    public Book(String name, String preface, long modificationTime, boolean isDummy) {
        this.name = name;
        this.preface = preface;
        this.modificationTime = modificationTime;
        this.isDummy = isDummy;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getPreface() {
        return preface;
    }

    public void setPreface(String preface) {
        this.preface = preface;
    }

    public String getName() {
        return name;
    }

    public boolean isModifiedAfterLastSync() {
        // Android does not report last file modification time in milliseconds in all cases
        // The mod times are truncated to seconds in some/many cases.
        // so consider it synced if within the last second
        return lastSyncedToRook != null && modificationTime - lastSyncedToRook.getMtime() > 1000;
    }

    public long getMtime() {
        return modificationTime;
    }

    public VersionedRook getLastSyncedToRook() {
        return lastSyncedToRook;
    }

    public void setLastSyncedToRook(VersionedRook vrook) {
        this.lastSyncedToRook = vrook;
    }

    public void setLink(Rook rook) {
        this.rook = rook;
    }

    public Rook getLink() {
        return rook;
    }

    public boolean isDummy() {
        return isDummy;
    }

    public BookSyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = null;

        if (syncStatus != null) {
            try {
                this.syncStatus = BookSyncStatus.valueOf(syncStatus);

            } catch (IllegalArgumentException e) {
                /* This status is not known.
                 * There was an issue when BOOK_WITH_LINK_BUT_NO_ROOK was renamed -- users
                 * who had it in DB would get a crash.
                 */
                e.printStackTrace();
            }
        }
    }

    public void setLastAction(BookAction lastAction) {
        this.lastAction = lastAction;
    }

    public BookAction getLastAction() {
        return lastAction;
    }

    public OrgFileSettings getOrgFileSettings() {
        return orgFileSettings;
    }

    public void setDetectedEncoding(String detectedEncoding) {
        this.detectedEncoding = detectedEncoding;
    }

    public void setSelectedEncoding(String selectedEncoding) {
        this.selectedEncoding = selectedEncoding;
    }

    public String getSelectedEncoding() {
        return selectedEncoding;
    }

    public String getDetectedEncoding() {
        return detectedEncoding;
    }

    public void setUsedEncoding(String usedEncoding) {
        this.usedEncoding = usedEncoding;
    }

    public String getUsedEncoding() {
        return usedEncoding;
    }

    public String toString() {
         /* Used by spinner in share activity. */
        return getFragmentTitleForBook(this);
    }

}
