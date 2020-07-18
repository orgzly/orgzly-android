package com.orgzly.android.ui;

import org.jetbrains.annotations.NotNull;

/**
 * Selected target note.
 * Used when pasting or creating new notes.
 */
public class NotePlace {
    private long bookId;
    private long noteId;
    private Place place;

    public NotePlace(long bookId) {
        this.bookId = bookId;
        place = Place.UNSPECIFIED;
    }

    public NotePlace(long bookId, long noteId, Place place) {
        this.bookId = bookId;
        this.noteId = noteId;
        this.place = place;
    }

    public long getBookId() {
        return bookId;
    }

    public long getNoteId() {
        return noteId;
    }

    @NotNull
    public Place getPlace() {
        return place;
    }

    public void setPlace(Place _place) { place = _place; }

    public String toString() {
        return "Book#" + bookId + " Note#" + noteId + " Place#" + place;
    }
}

