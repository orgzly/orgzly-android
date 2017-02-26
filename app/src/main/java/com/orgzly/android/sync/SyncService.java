package com.orgzly.android.sync;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.BookAction;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.repos.DirectoryRepo;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class SyncService extends Service {
    public static final String TAG = SyncService.class.getName();


    private SyncStatus status = new SyncStatus();

    private Shelf shelf;

    private SyncTask syncTask;

    // private NotificationManager notificationManager;

    private final IBinder binder = new LocalBinder();


    @Override
    public void onCreate() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        shelf = new Shelf(this);

        // notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // startForeground();

        status.loadFromPreferences(this);
    }

//    private void startForeground() {
//        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.syncing_in_progress)));
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);


        /* If syncing is already in progress, cancel it. */
        if (syncTask != null) {
            status.set(SyncStatus.Type.CANCELING, null, status.currentBook, status.totalBooks);
            broadcastCurrentSyncStatus();

            syncTask.cancel(false);

            return super.onStartCommand(intent, flags, startId);
        }

        Map<String, Repo> repos = shelf.getAllRepos();

        /* There are no repositories configured. */
        if (repos.size() == 0) {
            status.set(SyncStatus.Type.FAILED, getString(R.string.no_repos_configured), 0, 0);
            broadcastCurrentSyncStatus();
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        /* If one of the repositories requires internet connection, check for it. */
        if (reposRequireConnection(repos.values()) && !haveNetworkConnection()) {
            status.set(SyncStatus.Type.FAILED, getString(R.string.no_connection), 0, 0);
            broadcastCurrentSyncStatus();
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        /* Make sure we have permission to access local storage,
         * if there are repositories that would use it.
         */
        if (reposRequireStoragePermission(repos.values())) {
            if (AppPermissions.isNotGranted(this, AppPermissions.FOR_SYNC_START)) {
                status.set(SyncStatus.Type.NO_STORAGE_PERMISSION, null, 0, 0);
                broadcastCurrentSyncStatus();
                stopSelf();
                return super.onStartCommand(intent, flags, startId);
            }
        }

        syncTask = new SyncTask();
        syncTask.execute();

        return super.onStartCommand(intent, flags, startId);
    }


    private boolean reposRequireConnection(Collection<Repo> repos) {
        for (Repo repo: repos) {
            if (repo.requiresConnection()) {
                return true;
            }
        }
        return false;
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

        /* Kill the on-going notification (might not exist). */
        // notificationManager.cancel(NOTIFICATION_ID);

        super.onDestroy();
    }

    /**
     * Announce current sync status.
     */
    public void broadcastCurrentSyncStatus() {
        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, status.type, status.message, status.currentBook, status.totalBooks);

        /* Broadcast the intent. */
        LocalBroadcastManager.getInstance(this).sendBroadcast(status.intent());

        status.saveToPreferences(this);
    }

    public SyncStatus getStatus() {
        return status;
    }

    /**
     * Main sync task.
     */
    private class SyncTask extends AsyncTask<Void, Object, Exception> {
        @Override
        protected void onPreExecute() {
            status.set(SyncStatus.Type.STARTING, null, 0, 0);
            broadcastCurrentSyncStatus();
        }

        @Override
        protected Exception doInBackground(Void... params) { /* Executing on a different thread. */

            /* Get the list of local and remote books from all repositories.
             * Group them by name.
             * Inserts dummy books if they don't exist in database.
             */
            Map<String, BookNamesake> namesakes;
            try {
                namesakes = shelf.groupAllNotebooksByName();
            } catch (Exception e) {
                e.printStackTrace();
                return e;
            }

            if (namesakes.size() == 0) {
                return new IOException("No notebooks found");
            }

            status.set(SyncStatus.Type.BOOKS_COLLECTED, null, 0, namesakes.size());
            broadcastCurrentSyncStatus();

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
                    broadcastCurrentSyncStatus();

                    try {
                        BookAction action = shelf.syncNamesake(namesake);
                        shelf.setBookStatus(namesake.getBook(), namesake.getStatus().toString(), action);
                    } catch (Exception e) {
                        e.printStackTrace();
                        shelf.setBookStatus(namesake.getBook(), null, new BookAction(BookAction.Type.ERROR, e.getMessage()));
                    }

                    status.set(SyncStatus.Type.BOOK_ENDED, namesake.getName(), curr + 1, namesakes.size());
                    broadcastCurrentSyncStatus();
                }

                curr++;
            }

            return null; /* Success. */
        }

        @Override
        protected void onCancelled(Exception e) {
            status.set(SyncStatus.Type.CANCELED, getString(R.string.canceled), 0, 0);
            broadcastCurrentSyncStatus();

            syncTask = null;

            stopSelf();
        }

        @Override
        protected void onPostExecute(Exception exception) {
            if (exception != null) {
                String msg = (exception.getMessage() != null ?
                        exception.getMessage() : exception.toString());

                status.set(SyncStatus.Type.FAILED, msg, 0, 0);

            } else {
                status.set(SyncStatus.Type.FINISHED, null, 0, 0);

                /** Save last successful sync time to preferences. */
                long time = System.currentTimeMillis();
                AppPreferences.lastSuccessfulSyncTime(getApplicationContext(), time);
            }

            broadcastCurrentSyncStatus();

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
