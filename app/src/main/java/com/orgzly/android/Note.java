package com.orgzly.android;

import com.orgzly.org.OrgHead;

/**
 * Note with {@link OrgHead} and {@link NotePosition} in the notebook.
 */
public class Note {
    private long id;

    private OrgHead head;

    private NotePosition position;

    /** Number of lines in content. */
    private int mContentLines;

    /** Is the note folded (collapsed) or unfolded (expanded). */
    private boolean isFolded; // FIXME: Already in NotePosition


    public Note() {
        this.head = new OrgHead();
        this.position = new NotePosition();
    }

    public static Note newRootNote(long bookId) {
        Note note = new Note();

        NotePosition position = note.getPosition();

        position.setBookId(bookId);
        position.setLevel(0);
        position.setLft(1);
        position.setRgt(2);

        return note;
    }

    public NotePosition getPosition() {
        return position;
    }

    public void setPosition(NotePosition position) {
        this.position = position;
    }

    public OrgHead getHead() {
        return head;
    }

    public void setHead(OrgHead head) {
        this.head = head;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getContentLines() {
        return mContentLines;
    }

    public void setContentLines(int mContentLines) {
        this.mContentLines = mContentLines;
    }


    public boolean isFolded() {
        return isFolded;
    }

    public void setFolded(boolean folded) {
        isFolded = folded;
    }

    public String toString() {
        return String.format(
                "[%d-%d]  L:%d  Desc:%d  Folded:%s  FoldedUnder:%d  Id: %d",
                position.getLft(),
                position.getRgt(),
                position.getLevel(),
                position.getDescendantsCount(),
                this.isFolded(),
                position.getFoldedUnderId(),
                id);
    }
}
