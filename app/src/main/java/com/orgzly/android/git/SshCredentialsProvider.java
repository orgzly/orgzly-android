package com.orgzly.android.git;

import static com.orgzly.android.AppIntent.ACTION_ACCEPT_AND_STORE_REMOTE_HOST_KEY;
import static com.orgzly.android.AppIntent.ACTION_ACCEPT_REMOTE_HOST_KEY;
import static com.orgzly.android.AppIntent.ACTION_REJECT_REMOTE_HOST_KEY;
import static com.orgzly.android.ui.notifications.Notifications.SYNC_SSH_REMOTE_HOST_KEY;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.orgzly.android.App;
import com.orgzly.android.ui.notifications.Notifications;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import java.util.ArrayList;
import java.util.List;

public class SshCredentialsProvider extends CredentialsProvider {

    private static final Object monitor = new Object();

    public static final String DENY = "Reject";
    public static final String ALLOW = "Accept";
    public static final String ALLOW_AND_STORE = "Accept and store";

    @Override
    public boolean isInteractive() {
        return true;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        for (CredentialItem i : items) {
            if (i instanceof CredentialItem.YesNoType) {
                continue;
            }
            if (i instanceof CredentialItem.InformationalMessage) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        List<CredentialItem.YesNoType> questions = new ArrayList<>();
        for (CredentialItem item : items) {
            if (item instanceof CredentialItem.InformationalMessage) {
                continue;
            }
            if (item instanceof CredentialItem.YesNoType) {
                questions.add((CredentialItem.YesNoType) item);
                continue;
            }
            throw new UnsupportedCredentialItem(uri, item.getClass().getName()
                    + ":" + item.getPromptText()); //$NON-NLS-1$
        }

        if (questions.isEmpty()) {
            return true;
        } else {
            // We need to prompt the user via a notification;
            // set up a broadcast receiver for this purpose.
            Context context = App.getAppContext();
            final Boolean[] userHasResponded = {false};
            final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Remove the notification
                    NotificationManager notificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(SYNC_SSH_REMOTE_HOST_KEY);
                    // Save the user response
                    switch (intent.getAction()) {
                        case ACTION_REJECT_REMOTE_HOST_KEY:
                            questions.get(0).setValue(false);
                            break;
                        case ACTION_ACCEPT_REMOTE_HOST_KEY:
                            questions.get(0).setValue(true);
                            break;
                        case ACTION_ACCEPT_AND_STORE_REMOTE_HOST_KEY:
                            questions.get(0).setValue(true);
                            if (questions.size() == 2) {
                                questions.get(1).setValue(true);
                            }
                    }
                    userHasResponded[0] = true;
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };
            // Create intent filter and register receiver
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_REJECT_REMOTE_HOST_KEY);
            intentFilter.addAction(ACTION_ACCEPT_REMOTE_HOST_KEY);
            intentFilter.addAction(ACTION_ACCEPT_AND_STORE_REMOTE_HOST_KEY);
            context.registerReceiver(broadcastReceiver, intentFilter);

            // Send the notification and wait up to 30 seconds for the user to respond
            Notifications.showSshRemoteHostKeyPrompt(context, uri, items);
            synchronized (monitor) {
                if (!userHasResponded[0]) {
                    try {
                        monitor.wait(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        if (!userHasResponded[0]) {
                            return false;
                        }
                    }
                }
            }
            // Remove the broadcast receiver and its intent filters
            context.unregisterReceiver(broadcastReceiver);
            // Update the original list objects
            int questionCounter = 0;
            for (CredentialItem item : items) {
                if (item instanceof CredentialItem.YesNoType) {
                    ((CredentialItem.YesNoType) item).setValue(questions.get(questionCounter).getValue());
                    questionCounter++;
                }
            }
            return questions.get(0).getValue();
        }
    }
}