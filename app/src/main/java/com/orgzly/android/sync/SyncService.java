package com.orgzly.android.sync;


import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.App;
import com.orgzly.android.AppIntent;
import com.orgzly.android.BookFormat;
import com.orgzly.android.BookName;
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.NotesOrgExporter;
import com.orgzly.android.reminders.ReminderService;
import com.orgzly.android.ui.notifications.Notifications;
import com.orgzly.android.db.entity.BookAction;
import com.orgzly.android.db.entity.BookView;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.repos.DirectoryRepo;
import com.orgzly.android.repos.RepoUtils;
import com.orgzly.android.repos.SyncRepo;
import com.orgzly.android.repos.TwoWaySyncRepo;
import com.orgzly.android.repos.TwoWaySyncResult;
import com.orgzly.android.repos.VersionedRook;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.widgets.ListWidgetProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.DaggerService;

public class SyncService extends DaggerService {
    public static final String TAG = SyncService.class.getName();

    private SyncStatus status = new SyncStatus();

    private SyncTask syncTask;

    private final IBinder binder = new LocalBinder();

    @Inject
    DataRepository dataRepository;

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
        super.onCreate();

        startForeground(
                Notifications.SYNC_IN_PROGRESS_ID,
                Notifications.createSyncInProgressNotification(getApplicationContext()));

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

    private boolean reposRequireStoragePermission(Collection<SyncRepo> repos) {
        for (SyncRepo repo: repos) {
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

        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return haveNetworkConnection(cm);

            } else {
                return haveNetworkConnectionPreM(cm);
            }
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean haveNetworkConnection(ConnectivityManager cm) {
        Network network = cm.getActiveNetwork();

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);

        return capabilities != null
               && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    @SuppressWarnings("deprecation")
    private boolean haveNetworkConnectionPreM(ConnectivityManager cm) {
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null) {
            int type = networkInfo.getType();

            return type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_MOBILE;
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

            Map<String, SyncRepo> repos = dataRepository.getRepos();

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
                namesakes = groupAllNotebooksByName(dataRepository);
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

            if (isCancelled()) {
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
                dataRepository.setBookLastActionAndSyncStatus(namesake.getBook().getBook().getId(), BookAction.forNow(
                        BookAction.Type.PROGRESS, getString(R.string.syncing_in_progress)));
            }

            /*
             * Start syncing name by name.
             */
            int curr = 0;
            for (BookNamesake namesake : namesakes.values()) {
                /* If task has been canceled, just mark the remaining books as such. */
                if (isCancelled()) {
                    dataRepository.setBookLastActionAndSyncStatus(
                            namesake.getBook().getBook().getId(),
                            BookAction.forNow(BookAction.Type.INFO, getString(R.string.canceled)));

                } else {
                    status.set(SyncStatus.Type.BOOK_STARTED, namesake.getName(), curr, namesakes.size());
                    announceActiveSyncStatus();

                    try {
                        BookAction action = syncNamesake(dataRepository, namesake);
                        dataRepository.setBookLastActionAndSyncStatus(
                                namesake.getBook().getBook().getId(),
                                action,
                                namesake.getStatus().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        dataRepository.setBookLastActionAndSyncStatus(
                                namesake.getBook().getBook().getId(),
                                BookAction.forNow(BookAction.Type.ERROR, e.getMessage()));
                    }

                    // TODO: Call only if book was loaded, move to usecase
                    ReminderService.notifyDataChanged(App.getAppContext());
                    ListWidgetProvider.notifyDataChanged(App.getAppContext());

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


    /**
     * Compares every local book with every remote one and calculates the status for each link.
     *
     * @return number of links (unique book names)
     * @throws IOException
     */
    public static Map<String, BookNamesake> groupAllNotebooksByName(DataRepository dataRepository) throws IOException {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Collecting all local and remote books ...");

        Map<String, SyncRepo> repos = dataRepository.getRepos();

        List<BookView> localBooks = dataRepository.getBooks();
        List<VersionedRook> versionedRooks = getBooksFromAllRepos(dataRepository, repos);

        /* Group local and remote books by name. */
        Map<String, BookNamesake> namesakes = BookNamesake.getAll(
                App.getAppContext(), localBooks, versionedRooks);

        /* If there is no local book, create empty "dummy" one. */
        for (BookNamesake namesake : namesakes.values()) {
            if (namesake.getBook() == null) {
                namesake.setBook(dataRepository.createDummyBook(namesake.getName()));
            }

            namesake.updateStatus(repos.size());
        }

        return namesakes;
    }


    /**
     * Goes through each repository and collects all books from each one.
     */
    public static List<VersionedRook> getBooksFromAllRepos(DataRepository dataRepository, Map<String, SyncRepo> repos) throws IOException {
        List<VersionedRook> result = new ArrayList<>();

        if (repos == null) {
            repos = dataRepository.getRepos();
        }

        for (SyncRepo repo: repos.values()) { /* Each repository. */
            List<VersionedRook> libBooks = repo.getBooks();

            /* Each book in repository. */
            result.addAll(libBooks);
        }

        return result;
    }

    /**
     * Passed {@link com.orgzly.android.sync.BookNamesake} is NOT updated after load or save.
     *
     * FIXME: Hardcoded BookName.Format.ORG below
     */
    public static BookAction syncNamesake(DataRepository dataRepository, final BookNamesake namesake) throws IOException {
        String repoUrl;
        String fileName;
        BookAction bookAction = null;

        // FIXME: This is a pretty nasty hack that completely circumvents the existing code path
        if (!namesake.getRooks().isEmpty()) {
            VersionedRook rook = namesake.getRooks().get(0);
            if (rook != null && namesake.getStatus() != BookSyncStatus.NO_CHANGE) {
                Uri repoUri = rook.getRepoUri();
                SyncRepo repo = dataRepository.getRepo(repoUri);
                if (repo instanceof TwoWaySyncRepo) {
                    handleTwoWaySync(dataRepository, (TwoWaySyncRepo) repo, namesake);
                    return BookAction.forNow(
                            BookAction.Type.INFO,
                            namesake.getStatus().msg(repo.getUri().toString()));
                }
            }
        }

        switch (namesake.getStatus()) {
            case NO_CHANGE:
                bookAction = BookAction.forNow(BookAction.Type.INFO, namesake.getStatus().msg());
                break;

            case BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST:
            case DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS:
            case NO_BOOK_MULTIPLE_ROOKS:
            case ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS:
            case BOOK_WITH_LINK_AND_ROOK_EXISTS_BUT_LINK_POINTING_TO_DIFFERENT_ROOK:
            case CONFLICT_BOTH_BOOK_AND_ROOK_MODIFIED:

            case CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE:
            case CONFLICT_LAST_SYNCED_ROOK_AND_LATEST_ROOK_ARE_DIFFERENT:
            case ONLY_DUMMY:
                bookAction = BookAction.forNow(BookAction.Type.ERROR, namesake.getStatus().msg());
                break;

            /* Load remote book. */

            case NO_BOOK_ONE_ROOK:
            case DUMMY_WITHOUT_LINK_AND_ONE_ROOK:
                dataRepository.loadBookFromRepo(namesake.getRooks().get(0));
                bookAction = BookAction.forNow(
                        BookAction.Type.INFO,
                        namesake.getStatus().msg(namesake.getRooks().get(0).getUri()));
                break;

            case BOOK_WITH_LINK_AND_ROOK_MODIFIED:
                dataRepository.loadBookFromRepo(namesake.getLatestLinkedRook());
                bookAction = BookAction.forNow(
                        BookAction.Type.INFO,
                        namesake.getStatus().msg(namesake.getLatestLinkedRook().getUri()));
                break;

            case DUMMY_WITH_LINK:
                dataRepository.loadBookFromRepo(namesake.getLatestLinkedRook());
                bookAction = BookAction.forNow(
                        BookAction.Type.INFO,
                        namesake.getStatus().msg(namesake.getLatestLinkedRook().getUri()));
                break;

            /* Save local book to repository. */

            case ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO:
                /* Save local book to the one and only repository. */
                repoUrl = dataRepository.getRepos().entrySet().iterator().next().getValue().getUri().toString();
                fileName = BookName.fileName(namesake.getBook().getBook().getName(), BookFormat.ORG);
                dataRepository.saveBookToRepo(repoUrl, fileName, namesake.getBook(), BookFormat.ORG);
                bookAction = BookAction.forNow(BookAction.Type.INFO, namesake.getStatus().msg(repoUrl));
                break;

            case BOOK_WITH_LINK_LOCAL_MODIFIED:
                repoUrl = namesake.getBook().getSyncedTo().getRepoUri().toString();
                fileName = BookName.getFileName(App.getAppContext(), namesake.getBook().getSyncedTo().getUri());
                dataRepository.saveBookToRepo(repoUrl, fileName, namesake.getBook(), BookFormat.ORG);
                bookAction = BookAction.forNow(BookAction.Type.INFO, namesake.getStatus().msg(repoUrl));
                break;

            case ONLY_BOOK_WITH_LINK:
                repoUrl = namesake.getBook().getLinkedTo();
                fileName = BookName.fileName(namesake.getBook().getBook().getName(), BookFormat.ORG);
                dataRepository.saveBookToRepo(repoUrl, fileName, namesake.getBook(), BookFormat.ORG);
                bookAction = BookAction.forNow(BookAction.Type.INFO, namesake.getStatus().msg(repoUrl));
                break;
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Syncing " + namesake + ": " + bookAction);
        return bookAction;
    }

    private static void handleTwoWaySync(DataRepository dataRepository, TwoWaySyncRepo repo, BookNamesake namesake) throws IOException {
        BookView bookView = namesake.getBook();
        VersionedRook currentRook = bookView.getSyncedTo();
        VersionedRook someRook = currentRook == null ? namesake.getRooks().get(0) : currentRook;
        VersionedRook newRook = currentRook;
        File dbFile = dataRepository.getTempBookFile();
        try {
            new NotesOrgExporter(App.getAppContext(), dataRepository).exportBook(bookView.getBook(), dbFile);
            TwoWaySyncResult result = repo.syncBook(someRook.getUri(), currentRook, dbFile);
            // We only need to write it if syncback is needed
            if (result.getLoadFile() != null) {
                newRook = result.getNewRook();
                String fileName = BookName.getFileName(App.getAppContext(), newRook.getUri());
                BookName bookName = BookName.fromFileName(fileName);
                Log.i("Git", String.format("Loading from file %s", result.getLoadFile().toString()));
                BookView loadedBook = dataRepository.loadBookFromFile(
                        bookName.getName(),
                        bookName.getFormat(),
                        result.getLoadFile(),
                        newRook);
                // TODO: db.book().updateIsModified(bookView.book.id, false)
                // Instead of:
                // dataRepository.updateBookMtime(loadedBook.getBook().getId(), 0);
            }
        } finally {
            /* Delete temporary files. */
            dbFile.delete();
        }

        dataRepository.updateBookLinkAndSync(bookView.getBook().getId(), newRook);
    }

    public class LocalBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }
}
