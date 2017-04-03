package com.orgzly.android;

/**
 * Note's position in the notebook.
 */
public class NotePosition {
    /** Book ID. */
    private long bookId;

    /** Nested set model's left and right values. */
    private long lft;
    private long rgt;

    /** Level (depth) of a note. */
    private int level;

    /** Number of descendants. */
    private int descendantsCount;

    /** Note ID which hides this note by being folded. */
    private long foldedUnderId;

    private long parentId;

    /** Is the note folded (collapsed) or unfolded (expanded). */
    private boolean isFolded;

    public long getFoldedUnderId() {
        return foldedUnderId;
    }

    public void setFoldedUnderId(long foldedUnderId) {
        this.foldedUnderId = foldedUnderId;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public boolean isFolded() {
        return isFolded;
    }

    public void setIsFolded(boolean v) {
        isFolded = v;
    }

    public int getDescendantsCount() {
        return descendantsCount;
    }

    public void setDescendantsCount(int descendantsCount) {
        this.descendantsCount = descendantsCount;
    }

    public boolean hasDescendants() {
        return getDescendantsCount() > 0;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setBookId(long id) {
        bookId = id;
    }

    public long getBookId() {
        return bookId;
    }

    public long getLft() {
        return lft;
    }

    public void setLft(long lft) {
        this.lft = lft;
    }

    public long getRgt() {
        return rgt;
    }

    public void setRgt(long rgt) {
        this.rgt = rgt;
    }

    public boolean doesContain(NotePosition note) {
        return this.lft < note.lft && note.rgt < this.rgt;
    }

    public String toString() {
        return String.format("[%d-%d Lvl:%d Desc:%d Under:%d]", lft, rgt, level, descendantsCount, foldedUnderId);
    }
}
