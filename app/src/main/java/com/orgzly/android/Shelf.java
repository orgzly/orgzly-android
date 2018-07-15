package com.orgzly.android;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.filter.Filter;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.provider.clients.CurrentRooksClient;
import com.orgzly.android.provider.clients.DbClient;
import com.orgzly.android.provider.clients.FiltersClient;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.reminders.ReminderService;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.repos.RepoFactory;
import com.orgzly.android.repos.Rook;
import com.orgzly.android.repos.TwoWaySyncRepo;
import com.orgzly.android.repos.TwoWaySyncResult;
import com.orgzly.android.repos.VersionedRook;
import com.orgzly.android.sync.BookNamesake;
import com.orgzly.android.sync.BookSyncStatus;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.Place;
import com.orgzly.android.util.CircularArrayList;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.android.widgets.ListWidgetProvider;
import com.orgzly.org.OrgHead;
import com.orgzly.org.OrgProperties;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.parser.OrgParsedFile;
import com.orgzly.org.parser.OrgParser;
import com.orgzly.org.parser.OrgParserWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Local books storage
 *
 * FIXME: Turned into much more over time.
 */
public class Shelf {
    private static final String TAG = Shelf.class.getName();

    private Context mContext;
    private LocalStorage mLocalStorage;

    public Shelf(Context context) {
        mContext = context;
        mLocalStorage = new LocalStorage(context);
    }

    public List<Book> getBooks() {
        return BooksClient.getAll(mContext);
    }

    public Book getBook(long id) {
        return BooksClient.get(mContext, id);
    }

    public Book getBook(String name) {
        return BooksClient.get(mContext, name);
    }

    public boolean doesBookExist(String name) {
        return BooksClient.doesExist(mContext, name);
    }

    /**
     * Creates a new empty book.
     *
     * @param name name of the book
     */
    public Book createBook(String name) throws IOException {
        return BooksClient.insert(mContext, new Book(name));
    }

    /**
     * Creates new dummy book - temporary incomplete book
     * (before remote book has been downloaded, or linked etc.)
     */
    private Book createDummyBook(String name) throws IOException {
        return BooksClient.insert(mContext, new Book(name, "", 0, true));
    }

    /**
     * Loads book from resource.
     */
    public Book loadBookFromResource(String name, BookName.Format format, Resources resources, int resId) throws IOException {
        InputStream is = resources.openRawResource(resId);
        try {
            return loadBookFromStream(name, format, is);
        } finally {
            is.close();
        }
    }

    public Book loadBookFromStream(String name, BookName.Format format, InputStream inputStream) throws IOException {
        /* Save content to temporary file. */
        File tmpFile = getTempBookFile();

        try {
            MiscUtils.writeStreamToFile(inputStream, tmpFile);
            return loadBookFromFile(name, format, tmpFile);

        } finally {
            tmpFile.delete();
        }
    }

    /**
     * Imports {@code File} to database under specified book name.
     * Overwrites existing book with the same name.
     */
    // TODO: Separately update book here - we don't want to update its mtime if loaded from resource for example.
    public Book loadBookFromFile(String name, BookName.Format format, File file) throws IOException {
        return loadBookFromFile(name, format, file, null, null);
    }

    public Book loadBookFromFile(String name, BookName.Format format, File file, VersionedRook vrook) throws IOException {
        return loadBookFromFile(name, format, file, vrook, null);
    }

    public Book loadBookFromFile(String name, BookName.Format format, File file, VersionedRook vrook, String selectedEncoding) throws IOException {
        if (selectedEncoding == null && AppPreferences.forceUtf8(mContext)) {
            selectedEncoding = "UTF-8";
        }

        Uri uri = BooksClient.loadFromFile(mContext, name, format, file, vrook, selectedEncoding);

        notifyDataChanged(mContext);

        return BooksClient.get(mContext, ContentUris.parseId(uri));
    }

    public void setBookStatus(Book book, String status, BookAction action) {
        BooksClient.updateStatus(mContext, book.getId(), status, action);
    }

    // TODO: Do in Provider under transaction
    public void deleteBook(Book book, boolean deleteLinked) throws IOException {
        if (deleteLinked) {
            Repo repo = RepoFactory.getFromUri(mContext, book.getLastSyncedToRook().getRepoUri());
            if (repo != null) {
                repo.delete(book.getLastSyncedToRook().getUri());
            }
        }

        NotesClient.deleteFromBook(mContext, book.getId());

        BooksClient.delete(mContext, book.getId());

        notifyDataChanged(mContext);
    }

    // TODO: This is used in tests, check if we are even deleting these books.
    public File getTempBookFile() throws IOException {
        return mLocalStorage.getTempBookFile();
    }

    /**
     * Returns full string content of the book in format specified. Used by tests.
     */
    public String getBookContent(String name, BookName.Format format) throws IOException {
        Book book = getBook(name);

        if (book != null) {
            File file = getTempBookFile();
            try {
                NotesExporter.getInstance(mContext, format).exportBook(book, file);
                return MiscUtils.readStringFromFile(file);
            } finally {
                file.delete();
            }
        }

        return null;
    }

    public File exportBook(long bookId, BookName.Format format) throws IOException {
        /* Get book from database. */
        Book book = getBook(bookId);

        /* Get file to write book to. */
        File file = mLocalStorage.getExportFile(book, format);

        /* Write book. */
        NotesExporter.getInstance(mContext, format).exportBook(book, file);

        /* Make file immediately visible when using MTP.
         * See https://github.com/orgzly/orgzly-android/issues/44
         */
        MediaScannerConnection.scanFile(mContext, new String[] { file.getAbsolutePath() }, null, null);

        return file;
    }

    public void setNotesScheduledTime(Set<Long> noteIds, OrgDateTime time) {
        NotesClient.updateScheduledTime(mContext, noteIds, time);
        notifyDataChanged(mContext);
        syncOnNoteUpdate();
    }

    public void setNotesDeadlineTime(Set<Long> noteIds, OrgDateTime time) {
        NotesClient.updateDeadlineTime(mContext, noteIds, time);
        notifyDataChanged(mContext);
        syncOnNoteUpdate();
    }

    public void setNoteState(long noteId, String state) {
        if (state != null) {
            setNotesState(MiscUtils.set(noteId), state);
        }
    }

    public void setNotesState(Set<Long> noteIds, String state) {
        NotesClient.setState(mContext, noteIds, state);
        // TODO: Check if there was a change
        notifyDataChanged(mContext);
        syncOnNoteUpdate();
    }

    public Note getNote(long id) {
        return NotesClient.getNote(mContext, id);
    }

    public OrgProperties getNoteProperties(long id) {
        return NotesClient.getNoteProperties(mContext, id);
    }

    public Note getNote(String title) {
        return NotesClient.getNote(mContext, title, false);
    }

    public Note getNoteWithExtras(String title) {
        return NotesClient.getNote(mContext, title, true);
    }

    public int updateNote(Note note) {
        int result = NotesClient.update(mContext, note);
        notifyDataChanged(mContext);
        syncOnNoteUpdate();
        return result;
    }

    public int updateContent(long bookId, long noteId, String content) {
        int result = NotesClient.updateContent(mContext, bookId, noteId, content);
        notifyDataChanged(mContext);
        syncOnNoteUpdate();
        return result;
    }

    public Note createNote(Note note, NotePlace target) {

        long time = System.currentTimeMillis();

        // Set created-at time
        note.setCreatedAt(time);

        // Set created-at property
        if (AppPreferences.createdAt(mContext)) {
            String propName = AppPreferences.createdAtProperty(mContext);
            note.getHead().addProperty(propName, new OrgDateTime(time, false).toString());
        }

        /* Create new note. */
        Note insertedNote = NotesClient.create(mContext, note, target, time);

        notifyDataChanged(mContext);
        syncOnNoteCreate();

        return insertedNote;
    }

    public String[] getAllTags(long bookId) {
        return NotesClient.getAllTags(mContext, bookId);
    }

    public void toggleFoldedState(long noteId) {
        NotesClient.toggleFoldedState(mContext, noteId);
    }

    public int promote(long bookId, long noteId) {
        return promote(bookId, MiscUtils.set(noteId));
    }

    public int promote(long bookId, Set<Long> noteIds) {
        int result = BooksClient.promote(mContext, bookId, noteIds);
        // updateSync(); handled on action bar destroy instead
        return result;
    }

    public int demote(long bookId, long noteId) {
        return demote(bookId, MiscUtils.set(noteId));
    }

    public int demote(long bookId, Set<Long> noteIds) {
        int result = BooksClient.demote(mContext, bookId, noteIds);
        // updateSync(); handled on action bar destroy instead
        return result;
    }

    public int cut(long bookId, long noteId) {
        return cut(bookId, MiscUtils.set(noteId));
    }

    public int cut(long bookId, Set<Long> noteIds) {
        int result = NotesClient.cut(mContext, bookId, noteIds);
        notifyDataChanged(mContext);
        syncOnNoteUpdate();
        return result;
    }

    public int move(long bookId, long noteId, int offset) {
        int result = BooksClient.moveNotes(mContext, bookId, noteId, offset);
        notifyDataChanged(mContext);
        // updateSync(); handled on action bar destroy instead
        return result;
    }


    public int refile(long bookId, Set<Long> noteIds, long targetBookId) {
        int result = cut(bookId, noteIds);
        Note root = NotesClient.getRootNode(mContext, targetBookId);
        paste(targetBookId, root.getId(), Place.UNDER);
        return result;
    }

    public NotesBatch paste(long bookId, long noteId, Place place) {
        NotesBatch batch = NotesClient.paste(mContext, bookId, noteId, place);
        if (batch != null) {
            notifyDataChanged(mContext);
            syncOnNoteUpdate();
        }
        return batch;

    }

    public int delete(long bookId, Set<Long> noteIds) {
        int result = NotesClient.delete(mContext, bookId, noteIds);
        notifyDataChanged(mContext);
        syncOnNoteUpdate();
        return result;
    }

    /**
     * Recreate all tables.
     */
    public void clearDatabase() {
        DbClient.recreateTables(mContext);

        /* Clear last sync time. */
        AppPreferences.lastSuccessfulSyncTime(mContext, 0L);

        notifyDataChanged(mContext);
        // sync?
    }

    /**
     * Compares every local book with every remote one and calculates the status for each link.
     *
     * @return number of links (unique book names)
     * @throws IOException
     */
    public Map<String, BookNamesake> groupAllNotebooksByName() throws IOException {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Collecting all local and remote books ...");

        Map<String, Repo> repos = ReposClient.getAll(mContext);

        List<Book> localBooks = getBooks();
        List<VersionedRook> versionedRooks = getBooksFromAllRepos(repos);

        /* Group local and remote books by name. */
        Map<String, BookNamesake> namesakes = BookNamesake.getAll(mContext, localBooks, versionedRooks);

        /* If there is no local book, create empty "dummy" one. */
        for (BookNamesake namesake : namesakes.values()) {
            if (namesake.getBook() == null) {
                Book book = createDummyBook(namesake.getName());
                namesake.setBook(book);
            }

            namesake.updateStatus(repos.size());
        }

        return namesakes;

    }

    public Map<String, Repo> getAllRepos() {
        return ReposClient.getAll(mContext);
    }

    /**
     * Goes through each repository and collects all books from each one.
     */
    public List<VersionedRook> getBooksFromAllRepos(Map<String, Repo> repos) throws IOException {
        List<VersionedRook> result = new ArrayList<>();

        if (repos == null) {
            repos = getAllRepos();
        }

        for (Repo repo: repos.values()) { /* Each repository. */
            List<VersionedRook> libBooks = repo.getBooks();

            for (VersionedRook vrook : libBooks) { /* Each book in repository. */
                result.add(vrook);
            }
        }

        CurrentRooksClient.set(mContext, result);

        return result;
    }

    /**
     * Passed {@link com.orgzly.android.sync.BookNamesake} is NOT updated after load or save.
     *
     * FIXME: Hardcoded BookName.Format.ORG below
     */
    public BookAction syncNamesake(final BookNamesake namesake) throws IOException {
        String repoUrl;
        String fileName;
        BookAction bookAction = null;

        // XXX: This is a pretty nasty hack that completely circumvents the existing code path
        if (!namesake.getRooks().isEmpty()) {
            VersionedRook rook = namesake.getRooks().get(0);
            if (rook != null && namesake.getStatus() != BookSyncStatus.NO_CHANGE) {
                Uri repoUri = rook.getRepoUri();
                Repo repo = getRepo(repoUri);
                if (repo instanceof TwoWaySyncRepo) {
                    handleTwoWaySync((TwoWaySyncRepo) repo, namesake);
                    return new BookAction(
                            BookAction.Type.INFO,
                            namesake.getStatus().msg(repo.getUri().toString()));
                }
            }
        }

        switch (namesake.getStatus()) {
            case NO_CHANGE:
                bookAction = new BookAction(BookAction.Type.INFO, namesake.getStatus().msg());
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
                bookAction = new BookAction(BookAction.Type.ERROR, namesake.getStatus().msg());
                break;

            /* Load remote book. */

            case NO_BOOK_ONE_ROOK:
            case DUMMY_WITHOUT_LINK_AND_ONE_ROOK:
                loadBookFromRepo(namesake.getRooks().get(0));
                bookAction = new BookAction(BookAction.Type.INFO,
                        namesake.getStatus().msg(namesake.getRooks().get(0).getUri()));
                break;

            case BOOK_WITH_LINK_AND_ROOK_MODIFIED:
                loadBookFromRepo(namesake.getLatestLinkedRook());
                bookAction = new BookAction(BookAction.Type.INFO,
                        namesake.getStatus().msg(namesake.getLatestLinkedRook().getUri()));
                break;

            case DUMMY_WITH_LINK:
                loadBookFromRepo(namesake.getLatestLinkedRook());
                bookAction = new BookAction(BookAction.Type.INFO,
                        namesake.getStatus().msg(namesake.getLatestLinkedRook().getUri()));
                break;

            /* Save local book to repository. */

            case ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO:
                /* Save local book to the one and only repository. */
                repoUrl = getAllRepos().entrySet().iterator().next().getValue().getUri().toString();
                fileName = BookName.fileName(namesake.getBook().getName(), BookName.Format.ORG);
                saveBookToRepo(repoUrl, fileName, namesake.getBook(), BookName.Format.ORG);
                bookAction = new BookAction(BookAction.Type.INFO, namesake.getStatus().msg(repoUrl));
                break;

            case BOOK_WITH_LINK_LOCAL_MODIFIED:
                repoUrl = namesake.getBook().getLastSyncedToRook().getRepoUri().toString();
                fileName = BookName.getFileName(mContext, namesake.getBook().getLastSyncedToRook().getUri());
                saveBookToRepo(repoUrl, fileName, namesake.getBook(), BookName.Format.ORG);
                bookAction = new BookAction(BookAction.Type.INFO, namesake.getStatus().msg(repoUrl));
                break;

            case ONLY_BOOK_WITH_LINK:
                repoUrl = namesake.getBook().getLinkRepo().toString();
                fileName = BookName.fileName(namesake.getBook().getName(), BookName.Format.ORG);
                saveBookToRepo(repoUrl, fileName, namesake.getBook(), BookName.Format.ORG);
                bookAction = new BookAction(BookAction.Type.INFO, namesake.getStatus().msg(repoUrl));
                break;
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Syncing " + namesake + ": " + bookAction);
        return bookAction;
    }

    private Repo getRepo(Uri repoUrl) throws IOException {
        Repo repo = RepoFactory.getFromUri(mContext, repoUrl);

        if (repo == null) {
            throw new IOException("Unsupported repository URL \"" + repoUrl + "\"");
        }
        return repo;
    }

    /**
     * Downloads remote book, parses it and stores it to {@link Shelf}.
     * @return book now linked to remote one
     * @throws IOException
     */
    public Book loadBookFromRepo(Rook rook) throws IOException {
        String fileName = BookName.getFileName(mContext, rook.getUri());

        return loadBookFromRepo(rook.getRepoUri(), fileName);
    }

    public Book loadBookFromRepo(Uri repoUri, String fileName) throws IOException {
        Book book;

        Repo repo = RepoFactory.getFromUri(mContext, repoUri);
        if (repo == null) {
            throw new IOException("Unsupported repository URL \"" + repoUri + "\"");
        }

        File tmpFile = getTempBookFile();
        try {
            /* Download from repo. */
            VersionedRook vrook = repo.retrieveBook(fileName, tmpFile);

            BookName bookName = BookName.fromFileName(fileName);

            /* Store from file to Shelf. */
            book = loadBookFromFile(bookName.getName(), bookName.getFormat(), tmpFile, vrook);

        } finally {
            tmpFile.delete();
        }

        return book;
    }

    /**
     * Exports {@code Book}, uploads it to repo and link it to newly created
     * {@link com.orgzly.android.repos.VersionedRook}.
     *
     * @return {@link Book}
     * @throws IOException
     */
    public Book saveBookToRepo(String repoUrl, String fileName, Book book, BookName.Format format) throws IOException {

        VersionedRook uploadedBook;

        Repo repo = getRepo(Uri.parse(repoUrl));

        File tmpFile = getTempBookFile();
        try {
            /* Write to temporary file. */
            NotesExporter.getInstance(mContext, format).exportBook(book, tmpFile);

            /* Upload to repo. */
            uploadedBook = repo.storeBook(tmpFile, fileName);

        } finally {
            /* Delete temporary file. */
            tmpFile.delete();
        }

        book.setLastSyncedToRook(uploadedBook);

        BooksClient.saved(mContext, book.getId(), uploadedBook);

        return book;
    }

    public Book handleTwoWaySync(TwoWaySyncRepo repo, BookNamesake namesake) throws IOException {
        Book book = namesake.getBook();
        VersionedRook currentRook = book.getLastSyncedToRook();
        VersionedRook someRook = currentRook == null ? namesake.getRooks().get(0) : currentRook;
        VersionedRook newRook = currentRook;
        File dbFile = getTempBookFile();
        try {
            NotesExporter.getInstance(mContext, BookName.Format.ORG).exportBook(book, dbFile);
            TwoWaySyncResult result = repo.syncBook(someRook.getUri(), currentRook, dbFile);
            newRook = result.getNewRook();
            if (result.getLoadFile() != null) {
                String fileName = BookName.getFileName(mContext, newRook.getUri());
                BookName bookName = BookName.fromFileName(fileName);
                Log.i("Git", String.format("Loading from file %s", result.getLoadFile().toString()));
                book = loadBookFromFile(bookName.getName(), bookName.getFormat(),
                        result.getLoadFile(), newRook);
                BooksClient.setModificationTime(mContext, book.getId(), 0);
            }
            book.setLastSyncedToRook(newRook);
        } finally {
            /* Delete temporary files. */
            dbFile.delete();
        }

        BooksClient.saved(mContext, book.getId(), newRook);

        return book;
    }

    public int updateBookSettings(Book book) {
        return BooksClient.updateSettings(mContext, book);
    }

    public int updateBookPreface(long bookId, String preface) {
        int result = BooksClient.updatePreface(mContext, bookId, preface);
        notifyDataChanged(mContext);
        syncOnNoteUpdate();
        return result;
    }

    public void renameBook(Book book, String name) throws IOException {
        String oldName = book.getName();

        /* Make sure there is no notebook with this name. */
        if (getBook(name) != null) {
            throw new IOException("Notebook with that name already exists");
        }

        /* Make sure link's repo is the same as sync book repo. */
        if (book.hasLink() && book.getLastSyncedToRook() != null) {
            if (! book.getLinkRepo().equals(book.getLastSyncedToRook().getRepoUri())) {
                String s = BookSyncStatus.ROOK_AND_VROOK_HAVE_DIFFERENT_REPOS.toString();
                setBookStatus(book, s, new BookAction(BookAction.Type.ERROR, s));
                return;
            }
        }

        /* Do not rename if there are local changes. */
        if (book.getLastSyncedToRook() != null) {
            if (book.isModifiedAfterLastSync()) {
                throw new IOException("Notebook is not synced");
            }
        }

        /* Prefer link. */
        if (book.getLastSyncedToRook() != null) {
            VersionedRook vrook = book.getLastSyncedToRook();
            Repo repo = RepoFactory.getFromUri(mContext, vrook.getRepoUri());

            VersionedRook movedVrook = repo.renameBook(vrook.getUri(), name);

            book.setLastSyncedToRook(movedVrook);

            BooksClient.saved(mContext, book.getId(), movedVrook);
        }

        if (BooksClient.updateName(mContext, book.getId(), name) != 1) {
            String msg = mContext.getString(R.string.failed_renaming_book);
            setBookStatus(book, null, new BookAction(BookAction.Type.ERROR, msg));
            throw new IOException(msg);
        }

        setBookStatus(book, null, new BookAction(BookAction.Type.INFO, mContext.getString(R.string.renamed_book_from, oldName)));
    }

    public Uri addRepoUrl(String url) {
        return ReposClient.insert(mContext, url);
    }

    public int updateRepoUrl(long id, String url) {
        return ReposClient.updateUrl(mContext, id, url);
    }

    public int deleteRepo(long id) {
        return ReposClient.delete(mContext, id);
    }

    public void shiftState(long id, int direction) {
        Note note = getNote(id);
        if (note != null) {
            shiftState(note, direction);
        }
    }

    public void shiftState(Note note, int direction) {
        String currentState = note.getHead().getState();

        ArrayList<String> array = new ArrayList<>();
        array.add(null);
        array.addAll(AppPreferences.todoKeywordsSet(mContext));
        array.addAll(AppPreferences.doneKeywordsSet(mContext));
        CircularArrayList<String> allStates = new CircularArrayList<>(array.toArray(new String[array.size()]));

        int currentIndex = allStates.indexOf(currentState);
        int nextIndex = currentIndex + direction;

        String nextState = allStates.get(nextIndex);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Setting state for " + note.getHead().getTitle() + " to " + nextState +
                                                   ": " + currentIndex + " -> " + nextIndex + " (" + allStates.size() + " total states)");

        setNotesState(MiscUtils.set(note.getId()), nextState);
    }

    public void deleteFilters(Set<Long> ids) {
        // TODO: Send a single request. */
        for (long id: ids) {
            FiltersClient.delete(mContext, id);
        }
    }

    public void createFilter(Filter filter) {
        FiltersClient.create(mContext, filter);
    }

    public void updateFilter(long id, Filter filter) {
        FiltersClient.update(mContext, id, filter);
    }

    public void moveFilterUp(long id) {
        FiltersClient.moveUp(mContext, id);
    }

    public void moveFilterDown(long id) {
        FiltersClient.moveDown(mContext, id);
    }

    public void cycleVisibility(Book book) {
        BooksClient.cycleVisibility(mContext, book);
    }

    public void setLink(Book book, String repoUrl) {
        if (repoUrl == null) {
            BooksClient.removeLink(mContext, book.getId());
        } else {
            BooksClient.setLink(mContext, book.getId(), repoUrl);
        }
    }

    /** Check-mark button pressed. */
    public void flipState(long noteId) {
        Note note = getNote(noteId);

        if (note != null) {
            String state = note.getHead().getState();

            if (state != null && AppPreferences.isDoneKeyword(mContext, state)) {
                setStateToFirstTodo(noteId);
            } else {
                setStateToFirstDone(noteId);
            }
        }
    }

    /**
     * Sets the state of the note to the first to-do-type state defined in Settings.
     */
    private void setStateToFirstTodo(long noteId) {
        String state = AppPreferences.getFirstTodoState(mContext);
        setNoteState(noteId, state);
    }

    /**
     * Sets the state of the note to the first done-type state defined in Settings.
     */
    public void setStateToFirstDone(long noteId) {
        String state = AppPreferences.getFirstDoneState(mContext);
        setNoteState(noteId, state);
    }

    public static void notifyDataChanged(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        ReminderService.notifyDataChanged(context);

        Intent intent = new Intent(context, ListWidgetProvider.class);
        intent.setAction(AppIntent.ACTION_UPDATE_LIST_WIDGET);
        context.sendBroadcast(intent);
    }

    public void syncOnNoteCreate() {
        if (AppPreferences.autoSync(mContext) && AppPreferences.syncOnNoteCreate(mContext)) {
            autoSync();
        }
    }

    public void syncOnNoteUpdate() {
        if (AppPreferences.autoSync(mContext) && AppPreferences.syncOnNoteUpdate(mContext)) {
            autoSync();
        }
    }

    public void syncOnResume() {
        if (AppPreferences.autoSync(mContext) && AppPreferences.syncOnResume(mContext)) {
            autoSync();
        }
    }

    private void autoSync() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        /* Skip sync if there are no repos. */
        if (getAllRepos().size() == 0) {
            return;
        }

        Intent intent = new Intent(mContext, SyncService.class);
        intent.setAction(AppIntent.ACTION_SYNC_START);
        intent.putExtra(AppIntent.EXTRA_IS_AUTOMATIC, true);

        SyncService.start(mContext, intent);
    }

    public void directedSync() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        Intent intent = new Intent(mContext, SyncService.class);
        SyncService.start(mContext, intent);
    }

    /**
     * Using current states configuration, update states and titles for all notes.
     * Keywords that were part of the title can become states and vice versa.
     */
    public void reParseNotesStateAndTitles() throws IOException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        /* Get all notes. */
        Cursor cursor = mContext.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notes(), null, null, null, null);

        try {
            OrgParser.Builder parserBuilder = new OrgParser.Builder()
                    .setTodoKeywords(AppPreferences.todoKeywordsSet(mContext))
                    .setDoneKeywords(AppPreferences.doneKeywordsSet(mContext));

            OrgParserWriter parserWriter = new OrgParserWriter();

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                /* Get current heading string. */
                Note note = NotesClient.fromCursor(cursor);

                /* Skip root node. */
                if (note.getPosition().getLevel() == 0) {
                    continue;
                }

                OrgHead head = note.getHead();
                String headString = parserWriter.whiteSpacedHead(head, note.getPosition().getLevel(), false);

                /* Re-parse heading using current setting of keywords. */
                OrgParsedFile file = parserBuilder
                        .setInput(headString)
                        .build()
                        .parse();

                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Note " + note + " parsed has " + file.getHeadsInList().size() + " notes");

                if (file.getHeadsInList().size() != 1) {
                    throw new IOException("Got " + file.getHeadsInList().size() + " notes after parsing \"" + headString + "\"");
                }

                OrgHead newHead = file.getHeadsInList().get(0).getHead();

                ContentValues values = new ContentValues();

                /* Update if state, title or priority are different. */
                if (! TextUtils.equals(newHead.getState(), head.getState()) ||
                    ! TextUtils.equals(newHead.getTitle(), head.getTitle()) ||
                    ! TextUtils.equals(newHead.getPriority(), head.getPriority())) {

                    values.put(ProviderContract.Notes.UpdateParam.TITLE, newHead.getTitle());
                    values.put(ProviderContract.Notes.UpdateParam.STATE, newHead.getState());
                    values.put(ProviderContract.Notes.UpdateParam.PRIORITY, newHead.getPriority());
                }

                if (values.size() > 0) {
                    ops.add(ContentProviderOperation
                            .newUpdate(ContentUris.withAppendedId(ProviderContract.Notes.ContentUri.notes(), cursor.getLong(0)))
                            .withValues(values)
                            .build()
                    );
                }
            }

        } finally {
            cursor.close();
        }

        /*
         * Apply batch.
         */
        try {
            mContext.getContentResolver().applyBatch(ProviderContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        notifyDataChanged(mContext);
    }

    /**
     * Syncs created-at time and property, using lower value if both exist.
     */
    public void syncCreatedAtTimeWithProperty() throws IOException {
        boolean useCreatedAtProperty = AppPreferences.createdAt(mContext);
        String createdAtPropName = AppPreferences.createdAtProperty(mContext);

        if (!useCreatedAtProperty) {
            return;
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // If new property is added to the note below, book has to be marked as modified.
        Set<Long> bookIds = new HashSet<>();

        /*
         * Get all notes.
         * This is slow and only notes that have either created-at time or created-at property
         * are actually needed. But since this syncing (triggered on preference change) is done
         * so rarely, we don't bother.
         */
        try (Cursor cursor = mContext.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notes(), null, null, null, null)) {

            if (cursor != null) {

                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    /* Get current heading string. */
                    Note note = NotesClient.fromCursor(cursor);

                    /* Skip root node. */
                    if (note.getPosition().getLevel() == 0) {
                        continue;
                    }

                    long dbCreatedAt = note.getCreatedAt();

                    OrgProperties properties = NotesClient.getNoteProperties(mContext, NotesClient.idFromCursor(cursor));
                    String dbPropValue = properties.get(createdAtPropName);
                    OrgDateTime dbPropertyValue = OrgDateTime.doParse(dbPropValue);

                    // Compare dbCreatedAt and dbPropertyValue
                    if (dbCreatedAt > 0 && dbPropertyValue == null) {
                        addOpUpdateProperty(ops, note, createdAtPropName, dbCreatedAt, dbPropValue, bookIds);

                    } else if (dbCreatedAt > 0 && dbPropertyValue != null) {
                        // Use older created-at
                        if (dbPropertyValue.getCalendar().getTimeInMillis() < dbCreatedAt) {
                            addOpUpdateCreatedAt(ops, note, dbPropertyValue, note.getCreatedAt());
                        } else {
                            addOpUpdateProperty(ops, note, createdAtPropName, dbCreatedAt, dbPropValue, bookIds);
                        }

                        // Or prefer property and set created-at time?
                        // addOpUpdateCreatedAt(ops, note.getId(), dbPropertyValue, note.getCreatedAt());

                    } else if (dbCreatedAt == 0 && dbPropertyValue != null) {
                        addOpUpdateCreatedAt(ops, note, dbPropertyValue, note.getCreatedAt());

                    } // else: Neither created-at time nor property are set
                }
            }
        }

        long time = System.currentTimeMillis();
        for (long bookId: bookIds) {
            BooksClient.setModifiedTime(mContext, bookId, time);
        }

        /*
         * Apply batch.
         */
        try {
            mContext.getContentResolver().applyBatch(ProviderContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        notifyDataChanged(mContext);
        syncOnNoteUpdate();
    }

    private void addOpUpdateCreatedAt(ArrayList<ContentProviderOperation> ops, Note note, OrgDateTime dbPropertyValue, long currValue) {
        long value = dbPropertyValue.getCalendar().getTimeInMillis();

        if (value != currValue) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Updating created-at time", note.getId(), currValue, value);

            ops.add(ContentProviderOperation
                    .newUpdate(ProviderContract.Notes.ContentUri.notesId(note.getId()))
                    .withValue(DbNote.CREATED_AT, value)
                    .build());

        } else {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Skipping update", note.getId(), value);
        }
    }

    private void addOpUpdateProperty(ArrayList<ContentProviderOperation> ops, Note note, String createdAtPropName, long dbCreatedAt, String currPropValue, Set<Long> bookIds) {
        String value = new OrgDateTime(dbCreatedAt, false).toString();

        if (! value.equals(currPropValue)) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Updating property", note.getId(), createdAtPropName, currPropValue, value);

            ops.add(ContentProviderOperation
                    .newUpdate(ProviderContract.NoteProperties.ContentUri.notesIdProperties(note.getId()))
                    .withValue(createdAtPropName, value)
                    .build());

            // Should we remove the old property?

            bookIds.add(note.getPosition().getBookId());

        } else {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Skipping update", note.getId(), createdAtPropName, value);
        }
    }

    public void openFirstNoteWithProperty(String propName, String propValue) {
        Intent intent;

        List<Long[]> notes = NotesClient.getNotesWithProperty(mContext, propName, propValue);

        if (!notes.isEmpty()) {
            long noteId = notes.get(0)[0];
            long bookId = notes.get(0)[1];

            intent = new Intent(AppIntent.ACTION_OPEN_NOTE);
            intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId);
            intent.putExtra(AppIntent.EXTRA_BOOK_ID, bookId);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

        } else {
            String msg = mContext.getString(R.string.no_such_link_target, propName, propValue);
            CommonActivity.showSnackbar(mContext, msg);
        }
    }

    // TODO: Used by tests only for now
    public Map<String, BookNamesake> sync() {
        try {
            Map<String, BookNamesake> nameGroups = groupAllNotebooksByName();

            for (BookNamesake group : nameGroups.values()) {
                BookAction action = syncNamesake(group);
                setBookStatus(group.getBook(), group.getStatus().toString(), action);
            }

            return nameGroups;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
