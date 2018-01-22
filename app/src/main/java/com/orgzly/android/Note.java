package com.orgzly.android;

import com.orgzly.org.OrgHead;
import com.orgzly.org.OrgStringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Note with {@link OrgHead} and {@link NotePosition} in the notebook.
 */
public class Note {
    private long id;

    private long createdAt;

    private OrgHead head;

    private NotePosition position;

    /** Number of lines in content. */
    private int mContentLines;

    private List<String> inheritedTags;


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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
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

    /**
     * Inherited tags.
     *
     * @return list of tags
     */
    public List<String> getInheritedTags() {
        if (inheritedTags == null) {
            return new ArrayList<>();
        } else {
            return inheritedTags;
        }
    }

    public boolean hasInheritedTags() {
        return inheritedTags != null && !inheritedTags.isEmpty();
    }

    public void setInheritedTags(String[] tags) {
        if (tags == null) {
            throw new IllegalArgumentException("Tags passed to setTags cannot be null");
        }

        this.inheritedTags = new ArrayList<>();

        /* Only add non-null and non-empty strings. */
        for (String tag: tags) {
            if (!OrgStringUtils.isEmpty(tag)) {
                this.inheritedTags.add(tag);
            }
        }
    }

    public String toString() {
        return String.format(
                "[%d-%d]  L:%d  Desc:%d  Folded:%s  FoldedUnder:%d  Id: %d",
                position.getLft(),
                position.getRgt(),
                position.getLevel(),
                position.getDescendantsCount(),
                position.isFolded(),
                position.getFoldedUnderId(),
                id);
    }
}
