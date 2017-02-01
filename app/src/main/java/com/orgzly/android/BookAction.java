package com.orgzly.android;

/**
 * Last action performed on a notebook.
 *
 * Action can be a result of renaming, syncing (loading, saving, ...), importing, etc.
 */
public class BookAction {
    private final Type type;
    private final String message;
    private final long timestamp;

    public BookAction(Type type, String message) {
        this(type, message, System.currentTimeMillis());
    }

    public BookAction(Type type, String message, long timestamp) {
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toString() {
        return "[" + this.getClass().getSimpleName() + " " + type +
                " | " + message + " | " + timestamp + "]";
    }

    public enum Type {
        INFO,
        ERROR,
        PROGRESS
    }
}
