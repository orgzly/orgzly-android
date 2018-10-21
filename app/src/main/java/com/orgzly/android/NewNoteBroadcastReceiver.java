package com.orgzly.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;

import com.orgzly.android.ui.NotePlace;

import java.io.IOException;

public class NewNoteBroadcastReceiver extends BroadcastReceiver {
    public static final String NOTE_TITLE = "NOTE_TITLE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = getNoteTitle(intent);

        if (title != null) {
            try {
                Shelf shelf = new Shelf(context);

                Book book = BookUtils.getTargetBook(context);

                Note note = Shelf.buildNewNote(context, book.getId(), title, "");

                shelf.createNote(note, null);

                Notifications.createNewNoteNotification(context);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String getNoteTitle(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        return remoteInput == null ? null : remoteInput.getString(NOTE_TITLE);
    }
}
