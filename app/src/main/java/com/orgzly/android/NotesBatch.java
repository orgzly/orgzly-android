package com.orgzly.android;

import java.util.Set;

/**
 * Group of notes, used when cutting or deleting.
 */
public class NotesBatch {
    private long id;
    private int count;
    private Set<Long> noteIds;

    public NotesBatch(long id, Set<Long> noteIds) {
        this.id = id;
        this.count = noteIds.size();
        this.noteIds = noteIds;
    }

    public int getCount() {
        return count;
    }

    public long getId() {
        return id;
    }

    public Set<Long> getNoteIds() {
        return noteIds;
    }

    public String toString() {
        return getClass().getSimpleName() + "[" + id + " with " + count + " notes]";
    }
}

