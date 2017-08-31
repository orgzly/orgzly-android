package com.orgzly.android;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.orgzly.BuildConfig;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.sync.SyncService;

public class ActionService extends IntentService {
    public static final String TAG = ActionService.class.getName();

    public static final String EXTRA_NOTIFICATION_TAG = "notification_tag";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    public static final String EXTRA_NOTE_ID = "note_id";

    public ActionService() {
        super(TAG);

        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        Shelf shelf = new Shelf(this);

        dismissNotification(this, intent);

        if (intent != null && AppIntent.ACTION_NOTE_MARK_AS_DONE.equals(intent.getAction())) {
            long noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0);

            if (noteId > 0) {
                shelf.setStateToDone(noteId);
                if (AppPreferences.syncAfterNoteCreate(this)) {
                    Intent syncIntent = new Intent(this, SyncService.class);
                    intent.setAction(AppIntent.ACTION_SYNC_START);
                    this.startService(syncIntent);
                }
            } else {
                throw new IllegalArgumentException("Missing note ID");
            }
        }
    }

    /**
     * If notification ID was passed as an extra,
     * it means this action was performed from the notification.
     * Cancel the notification here.
     */
    private void dismissNotification(Context context, Intent intent) {
        if (intent != null) {
            int id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
            String tag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG);

            if (id > 0) {
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                notificationManager.cancel(tag, id);
            }
        }
    }
}
