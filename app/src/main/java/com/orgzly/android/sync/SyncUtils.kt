package com.orgzly.android.sync

import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.BookFormat
import com.orgzly.android.BookName
import com.orgzly.android.NotesOrgExporter
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.BookAction
import com.orgzly.android.db.entity.Repo
import com.orgzly.android.repos.GitRepo
import com.orgzly.android.repos.SyncRepo
import com.orgzly.android.repos.TwoWaySyncRepo
import com.orgzly.android.repos.VersionedRook
import com.orgzly.android.util.LogUtils
import java.io.IOException

object SyncUtils {
    private val TAG: String = SyncUtils::class.java.name

    /**
     * Goes through each repository and collects all books from each one.
     */
    @Throws(IOException::class)
    @JvmStatic
    fun getBooksFromAllRepos(dataRepository: DataRepository, repos: List<SyncRepo>? = null): List<VersionedRook> {
        val result = ArrayList<VersionedRook>()

        val repoList = repos ?: dataRepository.getSyncRepos()

        for (repo in repoList) {
            if (repo is GitRepo && repo.isUnchanged) {
                for (book in dataRepository.getBooks()) {
                    if (book.hasLink()) {
                        if (book.linkRepo!!.url == repo.uri.toString()) {
                            result.add(book.syncedTo!!)
                        }
                    }
                }
                if (result.isNotEmpty()) {
                    continue
                }
            }
            val libBooks = repo.books
            /* Each book in repository. */
            result.addAll(libBooks)
        }
        return result
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

        val repos = dataRepository.getSyncRepos()

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
     * Passed [com.orgzly.android.sync.BookNamesake] is NOT updated after load or save.
     *
     * FIXME: Hardcoded BookName.Format.ORG below
     */
    @Throws(Exception::class)
    @JvmStatic
    fun syncNamesake(dataRepository: DataRepository, namesake: BookNamesake): BookAction {
        val repoEntity: Repo?
        val repoUrl: String
        val fileName: String
        var bookAction: BookAction? = null

        // FIXME: This is a pretty nasty hack that completely circumvents the existing code path
        if (namesake.rooks.isNotEmpty()) {
            val rook = namesake.rooks[0]
            if (rook != null && namesake.status !== BookSyncStatus.NO_CHANGE && namesake.status !== BookSyncStatus.BOOK_WITH_LINK_LOCAL_MODIFIED) {
                val repo = dataRepository.getRepoInstance(
                    rook.repoId, rook.repoType, rook.repoUri.toString())
                if (repo is TwoWaySyncRepo) {
                    return if (handleTwoWaySync(dataRepository, repo as TwoWaySyncRepo, namesake)) {
                        BookAction.forNow(
                            BookAction.Type.INFO,
                            namesake.status.msg(repo.uri.toString()))
                    } else {
                        throw Exception("Merge conflict; saved to temporary branch.")
                    }
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

        // if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Syncing $namesake: $bookAction")

        return bookAction
    }

    @Throws(IOException::class)
    private fun handleTwoWaySync(dataRepository: DataRepository, repo: TwoWaySyncRepo, namesake: BookNamesake): Boolean {
        val (book, _, _, currentRook) = namesake.book
        val someRook = currentRook ?: namesake.rooks[0]
        val newRook: VersionedRook?
        val onMainBranch: Boolean
        val dbFile = dataRepository.getTempBookFile()
        try {
            NotesOrgExporter(dataRepository).exportBook(book, dbFile)
            val (newRook1, onMainBranch1, loadFile) =
                repo.syncBook(someRook.uri, currentRook, dbFile)
            onMainBranch = onMainBranch1
            newRook = newRook1
            // We only need to write it if syncback is needed
            if (loadFile != null) {
                val fileName = BookName.getFileName(App.getAppContext(), newRook.uri)
                val bookName = BookName.fromFileName(fileName)
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loading from file '$loadFile'")
                dataRepository.loadBookFromFile(
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

        return onMainBranch
    }
}