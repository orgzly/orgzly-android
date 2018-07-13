package com.orgzly.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;

import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.org.OrgHead;

import java.io.IOException;

public class NewNoteBroadcastReceiver extends BroadcastReceiver {
    public static final String NOTE_TITLE = "NOTE_TITLE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = getNoteTitle(intent);
        if (title != null) {
            try {
                Book targetBook = BookUtils.getTargetBook(context);
                NotesClient.create(context, makeNote(AppPreferences.newNoteState(context), title, targetBook));
                Notifications.createNewNoteNotification(context);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private Note makeNote(String state, String title, Book targetBook) {
        OrgHead head = new OrgHead();
        head.setTitle(title);
        head.setState(state);
        Note note = new Note();
        note.setCreatedAt(System.currentTimeMillis());
        note.getPosition().setBookId(targetBook.getId());
        note.setHead(head);
        return note;
    }

    private String getNoteTitle(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        return remoteInput == null ? null : remoteInput.getString(NOTE_TITLE);
    }
}
