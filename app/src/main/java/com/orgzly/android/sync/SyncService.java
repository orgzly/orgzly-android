package com.orgzly.android.sync;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.BookAction;
import com.orgzly.android.Notifications;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.repos.DirectoryRepo;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.repos.RepoUtils;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;

import java.util.Collection;
import java.util.Map;

public class SyncService extends Service {
    public static final String TAG = SyncService.class.getName();

    private SyncStatus status = new SyncStatus();

    private Shelf shelf;

    private SyncTask syncTask;

    private final IBinder binder = new LocalBinder();


    public static void start(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        startForeground(
                Notifications.SYNC_IN_PROGRESS,
                Notifications.createSyncInProgressNotification(getApplicationContext()));

        shelf = new Shelf(this);

        status.loadFromPreferences(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        boolean isTriggeredAutomatically = isTriggeredAutomatically(intent);

        if (intent != null && AppIntent.ACTION_SYNC_START.equals(intent.getAction())) {
            if (!isRunning()) {
                start(isTriggeredAutomatically);
            } else {
                stopSelf();
            }

        } else if (intent != null && AppIntent.ACTION_SYNC_STOP.equals(intent.getAction())) {
            if (isRunning()) {
                stop();
            } else {
                stopSelf();
            }

        } else {
            if (isRunning()) {
                stop();
            } else {
                start(isTriggeredAutomatically);
            }
        }

        return Service.START_REDELIVER_INTENT;
    }

    private boolean isTriggeredAutomatically(Intent intent) {
        return intent != null && intent.getBooleanExtra(AppIntent.EXTRA_IS_AUTOMATIC, false);
    }

    private boolean isRunning() {
        return syncTask != null;
    }

    private void start(boolean isTriggeredAutomatically) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        syncTask = new SyncTask();
        syncTask.execute(isTriggeredAutomatically);
    }

    private void stop() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        status.set(SyncStatus.Type.CANCELING, null, status.currentBook, status.totalBooks);
        announceActiveSyncStatus();

        syncTask.cancel(false);
    }

    private boolean reposRequireStoragePermission(Collection<Repo> repos) {
        for (Repo repo: repos) {
            if (DirectoryRepo.SCHEME.equals(repo.getUri().getScheme())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if there is internet connection available.
     */
    private boolean haveNetworkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null) {
            int type = networkInfo.getType();

            if (type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_MOBILE) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDestroy();
    }

    /**
     * Announce current sync status.
     */
    public void announceActiveSyncStatus() {
        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, status.type, status.message, status.currentBook, status.totalBooks);

        /* Broadcast the intent. */
        LocalBroadcastManager.getInstance(this).sendBroadcast(status.intent());

        status.saveToPreferences(this);
    }

    /**
     * Main sync task.
     */
    private class SyncTask extends AsyncTask<Boolean, Object, Void> {
        @Override
        protected Void doInBackground(Boolean... params) { /* Executing on a different thread. */
            boolean isTriggeredAutomatically = params[0];

            Context context = SyncService.this;

            Map<String, Repo> repos = shelf.getAllRepos();

            /* Do nothing if it's auto-sync and there are no repos or they require connection. */
            if (isTriggeredAutomatically) {
                if (repos.size() == 0 || RepoUtils.requireConnection(repos.values())) {
                    return null;
                }
            }

            Notifications.ensureSyncNotificationSetup(context);

            /* There are no repositories configured. */
            if (repos.size() == 0) {
                status.set(SyncStatus.Type.FAILED, getString(R.string.no_repos_configured), 0, 0);
                announceActiveSyncStatus();
                return null;
            }

            /* If one of the repositories requires internet connection, check for it. */
            if (RepoUtils.requireConnection(repos.values()) && !haveNetworkConnection()) {
                status.set(SyncStatus.Type.FAILED, getString(R.string.no_connection), 0, 0);
                announceActiveSyncStatus();
                return null;
            }

            /* Make sure we have permission to access local storage,
             * if there are repositories that would use it.
             */
            if (reposRequireStoragePermission(repos.values())) {
                if (!AppPermissions.isGranted(context, AppPermissions.Usage.SYNC_START)) {
                    status.set(SyncStatus.Type.NO_STORAGE_PERMISSION, null, 0, 0);
                    announceActiveSyncStatus();
                    return null;
                }
            }

            status.set(SyncStatus.Type.STARTING, null, 0, 0);
            announceActiveSyncStatus();

            /* Get the list of local and remote books from all repositories.
             * Group them by name.
             * Inserts dummy books if they don't exist in database.
             */
            Map<String, BookNamesake> namesakes;
            try {
                namesakes = shelf.groupAllNotebooksByName();
            } catch (Exception e) {
                e.printStackTrace();
                String msg = (e.getMessage() != null ? e.getMessage() : e.toString());
                status.set(SyncStatus.Type.FAILED, msg, 0, 0);
                announceActiveSyncStatus();
                return null;
            }

            if (namesakes.size() == 0) {
                status.set(SyncStatus.Type.FAILED, "No notebooks found", 0, 0);
                announceActiveSyncStatus();
                return null;
            }

            status.set(SyncStatus.Type.BOOKS_COLLECTED, null, 0, namesakes.size());
            announceActiveSyncStatus();

            /* Because android sometimes drops milliseconds on reported file lastModified,
             * wait until the next full second
             */
//            if (isTriggeredAutomatically) {
//                long now = System.currentTimeMillis();
//                long nowMsPart = now % 1000;
//                SystemClock.sleep(1000 - nowMsPart);
//            }

            /*
             * Update books' statuses, before starting to sync them.
             */
            for (BookNamesake namesake : namesakes.values()) {
                shelf.setBookStatus(namesake.getBook(), null, new BookAction(BookAction.Type.PROGRESS, getString(R.string.syncing_in_progress)));
            }

            /*
             * Start syncing name by name.
             */
            int curr = 0;
            for (BookNamesake namesake : namesakes.values()) {
                /* If task has been canceled, just mark the remaining books as such. */
                if (isCancelled()) {
                    shelf.setBookStatus(namesake.getBook(), null,
                            new BookAction(BookAction.Type.INFO, getString(R.string.canceled)));

                } else {
                    status.set(SyncStatus.Type.BOOK_STARTED, namesake.getName(), curr, namesakes.size());
                    announceActiveSyncStatus();

                    try {
                        BookAction action = shelf.syncNamesake(namesake);
                        shelf.setBookStatus(namesake.getBook(), namesake.getStatus().toString(), action);
                    } catch (Exception e) {
                        e.printStackTrace();
                        shelf.setBookStatus(namesake.getBook(), null, new BookAction(BookAction.Type.ERROR, e.getMessage()));
                    }

                    status.set(SyncStatus.Type.BOOK_ENDED, namesake.getName(), curr + 1, namesakes.size());
                    announceActiveSyncStatus();
                }

                curr++;
            }

            status.set(SyncStatus.Type.FINISHED, null, 0, 0);
            announceActiveSyncStatus();

            /* Save last successful sync time to preferences. */
            long time = System.currentTimeMillis();
            AppPreferences.lastSuccessfulSyncTime(getApplicationContext(), time);

            return null; /* Success. */
        }

        @Override
        protected void onCancelled(Void v) {
            status.set(SyncStatus.Type.CANCELED, getString(R.string.canceled), 0, 0);
            announceActiveSyncStatus();

            syncTask = null;
            stopSelf();
        }

        @Override
        protected void onPostExecute(Void v) {
            syncTask = null;
            stopSelf();
        }
    }

    public class LocalBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }
}
