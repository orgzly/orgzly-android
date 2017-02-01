package com.orgzly.android.ui;

/**
 * Selected target note.
 * Used when pasting or creating new notes.
 */
public class NotePlacement {
    private long bookId;
    private long noteId;
    private Placement placement;

    public NotePlacement(long bookId) {
        this.bookId = bookId;
        placement = Placement.UNDEFINED;
    }

    public NotePlacement(long bookId, long noteId, Placement placement) {
        this.bookId = bookId;
        this.noteId = noteId;
        this.placement = placement;
    }

    public long getBookId() {
        return bookId;
    }

    public long getNoteId() {
        return noteId;
    }

    public Placement getPlacement() {
        return placement;
    }

    public String toString() {
        return "Book#" + bookId + " Note#" + noteId + " Placement#" + placement;
    }
}

