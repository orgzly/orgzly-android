package com.orgzly.android.sync


import android.annotation.TargetApi
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.AsyncTask
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.*
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.BookAction
import com.orgzly.android.db.entity.Repo
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.reminders.ReminderService
import com.orgzly.android.repos.*
import com.orgzly.android.ui.notifications.Notifications
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.LogUtils
import com.orgzly.android.widgets.ListWidgetProvider
import java.io.IOException
import java.util.*
import javax.inject.Inject

class SyncService : Service() {

    private val syncStatus = SyncStatus()

    private var syncTask: SyncTask? = null

    private val binder = LocalBinder()

    @Inject
    lateinit var dataRepository: DataRepository

    private val isRunning: Boolean
        get() = syncTask != null

    override fun onCreate() {
        App.appComponent.inject(this)
        super.onCreate()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        startForeground(
                Notifications.SYNC_IN_PROGRESS_ID,
                Notifications.createSyncInProgressNotification(applicationContext))

        syncStatus.loadFromPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent)

        val isTriggeredAutomatically = isTriggeredAutomatically(intent)

        if (intent != null && AppIntent.ACTION_SYNC_START == intent.action) {
            if (!isRunning) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Start requested while not running")
                start(isTriggeredAutomatically)
            } else {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Start requested while already running")
                stopSelf()
            }

        } else if (intent != null && AppIntent.ACTION_SYNC_STOP == intent.action) {
            if (isRunning) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Stop requested while already running")
                stop()
            } else {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Stop requested while not running")
                stopSelf()
            }

        } else {
            if (isRunning) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Sync requested while already running")
                stop()
            } else {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Sync requested while not running")
                start(isTriggeredAutomatically)
            }
        }

        return Service.START_REDELIVER_INTENT
    }

    private fun isTriggeredAutomatically(intent: Intent?): Boolean {
        return intent != null && intent.getBooleanExtra(AppIntent.EXTRA_IS_AUTOMATIC, false)
    }

    private fun start(isTriggeredAutomatically: Boolean) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        syncTask = SyncTask().apply {
            execute(isTriggeredAutomatically)
        }
    }

    private fun stop() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        syncStatus.set(SyncStatus.Type.CANCELING, null, syncStatus.currentBook, syncStatus.totalBooks)
        announceActiveSyncStatus()

        syncTask?.cancel(false)
    }

    private fun reposRequireStoragePermission(repos: Collection<SyncRepo>): Boolean {
        for (repo in repos) {
            if (DirectoryRepo.SCHEME == repo.uri.scheme) {
                return true
            }
        }
        return false
    }

    /**
     * Determines if there is internet connection available.
     */
    private fun haveNetworkConnection(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        return if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                haveNetworkConnection(cm)

            } else {
                haveNetworkConnectionPreM(cm)
            }
        } else false

    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun haveNetworkConnection(cm: ConnectivityManager): Boolean {
        val network = cm.activeNetwork

        val capabilities = cm.getNetworkCapabilities(network)

        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun haveNetworkConnectionPreM(cm: ConnectivityManager): Boolean {
        val networkInfo = cm.activeNetworkInfo

        if (networkInfo != null) {
            val type = networkInfo.type

            return type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_MOBILE
        }

        return false
    }

    override fun onBind(intent: Intent): IBinder? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
    }

    /**
     * Announce current sync syncStatus.
     */
    fun announceActiveSyncStatus() {
        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, syncStatus.type, syncStatus.message, syncStatus.currentBook, syncStatus.totalBooks)

        /* Broadcast the intent. */
        LocalBroadcastManager.getInstance(this).sendBroadcast(syncStatus.intent())

        syncStatus.saveToPreferences(this)
    }

    /**
     * Main sync task.
     */
    private inner class SyncTask : AsyncTask<Boolean, Any, Void?>() {
        override fun doInBackground(vararg params: Boolean?): Void? {
            val isTriggeredAutomatically = params.getOrNull(0) ?: false

            val context = this@SyncService

            val repos = getRepos(dataRepository)

            /* Do nothing if it's auto-sync and there are no repos or they require connection. */
            if (isTriggeredAutomatically) {
                if (repos.isEmpty() || !RepoUtils.isAutoSyncSupported(repos)) {
                    return null
                }
            }

            Notifications.ensureSyncNotificationSetup(context)

            /* There are no repositories configured. */
            if (repos.isEmpty()) {
                syncStatus.set(SyncStatus.Type.FAILED, getString(R.string.no_repos_configured), 0, 0)
                announceActiveSyncStatus()
                return null
            }

            /* If one of the repositories requires internet connection, check for it. */
            if (RepoUtils.isConnectionRequired(repos) && !haveNetworkConnection()) {
                syncStatus.set(SyncStatus.Type.FAILED, getString(R.string.no_connection), 0, 0)
                announceActiveSyncStatus()
                return null
            }

            /* Make sure we have permission to access local storage,
             * if there are repositories that would use it.
             */
            if (reposRequireStoragePermission(repos)) {
                if (!AppPermissions.isGranted(context, AppPermissions.Usage.SYNC_START)) {
                    syncStatus.set(SyncStatus.Type.NO_STORAGE_PERMISSION, null, 0, 0)
                    announceActiveSyncStatus()
                    return null
                }
            }

            syncStatus.set(SyncStatus.Type.STARTING, null, 0, 0)
            announceActiveSyncStatus()

            /* Get the list of local and remote books from all repositories.
             * Group them by name.
             * Inserts dummy books if they don't exist in database.
             */
            val namesakes: Map<String, BookNamesake>
            try {
                namesakes = groupAllNotebooksByName(dataRepository)
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = if (e.message != null) e.message else e.toString()
                syncStatus.set(SyncStatus.Type.FAILED, msg, 0, 0)
                announceActiveSyncStatus()
                return null
            }

            if (namesakes.isEmpty()) {
                syncStatus.set(SyncStatus.Type.FAILED, "No notebooks found", 0, 0)
                announceActiveSyncStatus()
                return null
            }

            if (isCancelled) {
                return null
            }

            syncStatus.set(SyncStatus.Type.BOOKS_COLLECTED, null, 0, namesakes.size)
            announceActiveSyncStatus()

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
            for (namesake in namesakes.values) {
                dataRepository.setBookLastActionAndSyncStatus(namesake.book.book.id, BookAction.forNow(
                        BookAction.Type.PROGRESS, getString(R.string.syncing_in_progress)))
            }

            /*
             * Start syncing name by name.
             */
            var curr = 0
            for (namesake in namesakes.values) {
                /* If task has been canceled, just mark the remaining books as such. */
                if (isCancelled) {
                    dataRepository.setBookLastActionAndSyncStatus(
                            namesake.book.book.id,
                            BookAction.forNow(BookAction.Type.INFO, getString(R.string.canceled)))

                } else {
                    syncStatus.set(SyncStatus.Type.BOOK_STARTED, namesake.name, curr, namesakes.size)
                    announceActiveSyncStatus()

                    try {
                        val action = syncNamesake(dataRepository, namesake)
                        dataRepository.setBookLastActionAndSyncStatus(
                                namesake.book.book.id,
                                action!!,
                                namesake.status.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        dataRepository.setBookLastActionAndSyncStatus(
                                namesake.book.book.id,
                                BookAction.forNow(BookAction.Type.ERROR, e.message ?: ""))
                    }

                    // TODO: Call only if book was loaded, move to usecase
                    ReminderService.notifyForDataChanged(App.getAppContext())
                    ListWidgetProvider.notifyDataChanged(App.getAppContext())

                    syncStatus.set(SyncStatus.Type.BOOK_ENDED, namesake.name, curr + 1, namesakes.size)
                    announceActiveSyncStatus()
                }

                curr++
            }

            syncStatus.set(SyncStatus.Type.FINISHED, null, 0, 0)
            announceActiveSyncStatus()

            /* Save last successful sync time to preferences. */
            val time = System.currentTimeMillis()
            AppPreferences.lastSuccessfulSyncTime(applicationContext, time)

            return null /* Success. */
        }

        override fun onCancelled(v: Void?) {
            syncStatus.set(SyncStatus.Type.CANCELED, getString(R.string.canceled), 0, 0)
            announceActiveSyncStatus()

            syncTask = null
            stopSelf()
        }

        override fun onPostExecute(v: Void?) {
            syncTask = null
            stopSelf()
        }
    }

    inner class LocalBinder : Binder() {
        val service: SyncService
            get() = this@SyncService
    }

    companion object {
        val TAG = SyncService::class.java.name

        @JvmStatic
        fun start(context: Context?, intent: Intent) {
            if (context == null) {
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }


        /**
         * Compares every local book with every remote one and calculates the syncStatus for each link.
         *
         * @return number of links (unique book names)
         * @throws IOException
         */
        @Throws(IOException::class)
        @JvmStatic
        fun groupAllNotebooksByName(dataRepository: DataRepository): Map<String, BookNamesake> {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Collecting all local and remote books ...")

            val repos = getRepos(dataRepository)

            val localBooks = dataRepository.getBooks()
            val versionedRooks = getBooksFromAllRepos(dataRepository, repos)

            /* Group local and remote books by name. */
            val namesakes = BookNamesake.getAll(
                    App.getAppContext(), localBooks, versionedRooks)

            /* If there is no local book, create empty "dummy" one. */
            for (namesake in namesakes.values) {
                if (namesake.book == null) {
                    namesake.book = dataRepository.createDummyBook(namesake.name)
                }

                namesake.updateStatus(repos.size)
            }

            return namesakes
        }


        /**
         * Goes through each repository and collects all books from each one.
         */
        @Throws(IOException::class)
        @JvmStatic
        fun getBooksFromAllRepos(dataRepository: DataRepository, repos: List<SyncRepo>? = null): List<VersionedRook> {
            val result = ArrayList<VersionedRook>()

            val repoList = repos ?: getRepos(dataRepository)

            for (repo in repoList) {
                val libBooks = repo.books

                /* Each book in repository. */
                result.addAll(libBooks)
            }

            return result
        }

        private fun getRepos(dataRepository: DataRepository): List<SyncRepo> {
            val list = ArrayList<SyncRepo>()
            for ((id, type, url) in dataRepository.getRepos()) {
                try {
                    list.add(dataRepository.getRepoInstance(id, type, url))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            return list
        }

        /**
         * Passed [com.orgzly.android.sync.BookNamesake] is NOT updated after load or save.
         *
         * FIXME: Hardcoded BookName.Format.ORG below
         */
        @Throws(Exception::class)
        @JvmStatic
        fun syncNamesake(dataRepository: DataRepository, namesake: BookNamesake): BookAction? {
            val repoEntity: Repo?
            val repoUrl: String
            val fileName: String
            var bookAction: BookAction? = null

            // FIXME: This is a pretty nasty hack that completely circumvents the existing code path
            if (namesake.rooks.isNotEmpty()) {
                val rook = namesake.rooks[0]
                if (rook != null && namesake.status !== BookSyncStatus.NO_CHANGE) {
                    val repo = dataRepository.getRepoInstance(
                            rook.repoId, rook.repoType, rook.repoUri.toString())
                    if (repo is TwoWaySyncRepo) {
                        handleTwoWaySync(dataRepository, repo as TwoWaySyncRepo, namesake)
                        return BookAction.forNow(
                                BookAction.Type.INFO,
                                namesake.status.msg(repo.uri.toString()))
                    }
                }
            }

            when (namesake.status!!) {
                BookSyncStatus.NO_CHANGE ->
                    bookAction = BookAction.forNow(BookAction.Type.INFO, namesake.status.msg())

                BookSyncStatus.BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST,
                BookSyncStatus.DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS,
                BookSyncStatus.NO_BOOK_MULTIPLE_ROOKS,
                BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS,
                BookSyncStatus.BOOK_WITH_LINK_AND_ROOK_EXISTS_BUT_LINK_POINTING_TO_DIFFERENT_ROOK,
                BookSyncStatus.CONFLICT_BOTH_BOOK_AND_ROOK_MODIFIED,
                BookSyncStatus.CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE,
                BookSyncStatus.CONFLICT_LAST_SYNCED_ROOK_AND_LATEST_ROOK_ARE_DIFFERENT,
                BookSyncStatus.ROOK_AND_VROOK_HAVE_DIFFERENT_REPOS,
                BookSyncStatus.ONLY_DUMMY ->
                    bookAction = BookAction.forNow(BookAction.Type.ERROR, namesake.status.msg())

                /* Load remote book. */

                BookSyncStatus.NO_BOOK_ONE_ROOK, BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK -> {
                    dataRepository.loadBookFromRepo(namesake.rooks[0])
                    bookAction = BookAction.forNow(
                            BookAction.Type.INFO,
                            namesake.status.msg(namesake.rooks[0].uri))
                }

                BookSyncStatus.DUMMY_WITH_LINK, BookSyncStatus.BOOK_WITH_LINK_AND_ROOK_MODIFIED -> {
                    dataRepository.loadBookFromRepo(namesake.latestLinkedRook)
                    bookAction = BookAction.forNow(
                            BookAction.Type.INFO,
                            namesake.status.msg(namesake.latestLinkedRook.uri))
                }

                /* Save local book to repository. */

                BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO -> {
                    repoEntity = dataRepository.getRepos().iterator().next()
                    repoUrl = repoEntity.url
                    fileName = BookName.fileName(namesake.book.book.name, BookFormat.ORG)
                    dataRepository.saveBookToRepo(repoEntity, fileName, namesake.book, BookFormat.ORG)
                    bookAction = BookAction.forNow(BookAction.Type.INFO, namesake.status.msg(repoUrl))
                }

                BookSyncStatus.BOOK_WITH_LINK_LOCAL_MODIFIED -> {
                    repoEntity = namesake.book.linkRepo
                    repoUrl = repoEntity!!.url
                    fileName = BookName.getFileName(App.getAppContext(), namesake.book.syncedTo!!.uri)
                    dataRepository.saveBookToRepo(repoEntity, fileName, namesake.book, BookFormat.ORG)
                    bookAction = BookAction.forNow(BookAction.Type.INFO, namesake.status.msg(repoUrl))
                }

                BookSyncStatus.ONLY_BOOK_WITH_LINK -> {
                    repoEntity = namesake.book.linkRepo
                    repoUrl = repoEntity!!.url
                    fileName = BookName.fileName(namesake.book.book.name, BookFormat.ORG)
                    dataRepository.saveBookToRepo(repoEntity, fileName, namesake.book, BookFormat.ORG)
                    bookAction = BookAction.forNow(BookAction.Type.INFO, namesake.status.msg(repoUrl))
                }
            }

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Syncing $namesake: $bookAction")
            return bookAction
        }

        @Throws(IOException::class)
        private fun handleTwoWaySync(dataRepository: DataRepository, repo: TwoWaySyncRepo, namesake: BookNamesake) {
            val (book, _, _, currentRook) = namesake.book
            val someRook = currentRook ?: namesake.rooks[0]
            var newRook = currentRook
            val dbFile = dataRepository.getTempBookFile()
            try {
                NotesOrgExporter(dataRepository).exportBook(book, dbFile)
                val (newRook1, loadFile) = repo.syncBook(someRook.uri, currentRook!!, dbFile)
                // We only need to write it if syncback is needed
                if (loadFile != null) {
                    newRook = newRook1
                    val fileName = BookName.getFileName(App.getAppContext(), newRook.uri)
                    val bookName = BookName.fromFileName(fileName)
                    Log.i("Git", String.format("Loading from file %s", loadFile.toString()))
                    val loadedBook = dataRepository.loadBookFromFile(
                            bookName.name,
                            bookName.format,
                            loadFile,
                            newRook)
                    // TODO: db.book().updateIsModified(bookView.book.id, false)
                    // Instead of:
                    // dataRepository.updateBookMtime(loadedBook.getBook().getId(), 0);
                }
            } finally {
                /* Delete temporary files. */
                dbFile.delete()
            }

            dataRepository.updateBookLinkAndSync(book.id, newRook!!)
        }
    }
}
