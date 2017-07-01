package com.orgzly.android;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import com.orgzly.BuildConfig;
import com.orgzly.R;
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
import com.orgzly.android.repos.VersionedRook;
import com.orgzly.android.sync.BookNamesake;
import com.orgzly.android.sync.BookSyncStatus;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.Place;
import com.orgzly.android.util.CircularArrayList;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.android.util.UriUtils;
import com.orgzly.org.OrgHead;
import com.orgzly.org.OrgProperty;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.parser.OrgParsedFile;
import com.orgzly.org.parser.OrgParser;
import com.orgzly.org.parser.OrgParserSettings;
import com.orgzly.org.parser.OrgParserWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
            Repo repo = RepoFactory.getFromUri(mContext, book.getLink().getRepoUri());
            if (repo != null) {
                repo.delete(book.getLink().getUri());
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
                writeBookToFile(book, format, file);
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
        writeBookToFile(book, format, file);

        return file;
    }

    /**
     * Writes content of book from database to specified file.
     * TODO: Do in Provider under transaction
     */
    public void writeBookToFile(final Book book, BookName.Format format, File file) throws IOException {

        /* Use the same encoding. */
        String encoding = book.getUsedEncoding();
        if (encoding == null) {
            encoding = Charset.defaultCharset().name();
        }

        final PrintWriter out = new PrintWriter(file, encoding);

        try {
            String prefValue = AppPreferences.separateNotesWithNewLine(mContext);

            OrgParserSettings parserSettings = OrgParserSettings.getBasic();

            if (mContext.getString(R.string.pref_value_separate_notes_with_new_line_always).equals(prefValue)) {
                parserSettings.separateNotesWithNewLine = OrgParserSettings.SeparateNotesWithNewLine.ALWAYS;
            } else if (mContext.getString(R.string.pref_value_separate_notes_with_new_line_multi_line_notes_only).equals(prefValue)) {
                parserSettings.separateNotesWithNewLine = OrgParserSettings.SeparateNotesWithNewLine.MULTI_LINE_NOTES_ONLY;
            } else if (mContext.getString(R.string.pref_value_separate_notes_with_new_line_never).equals(prefValue)) {
                parserSettings.separateNotesWithNewLine = OrgParserSettings.SeparateNotesWithNewLine.NEVER;
            }

            parserSettings.separateHeaderAndContentWithNewLine = AppPreferences.separateHeaderAndContentWithNewLine(mContext);

            final OrgParserWriter parserWriter = new OrgParserWriter(parserSettings);

            out.write(parserWriter.whiteSpacedFilePreface(book.getPreface()));

            NotesClient.forEachBookNote(mContext, book.getName(), new NotesClient.NotesClientInterface() {
                @Override
                public void onNote(Note note) {
                    out.write(parserWriter.whiteSpacedHead(
                            note.getHead(),
                            note.getPosition().getLevel(),
                            book.getOrgFileSettings().isIndented()));
                }
            });

        } finally {
            out.close();
        }
    }

    public void setNotesScheduledTime(Set<Long> noteIds, OrgDateTime time) {
        NotesClient.updateScheduledTime(mContext, noteIds, time);
        notifyDataChanged(mContext);
    }

    public void setNotesState(Set<Long> noteIds, String state) {
        NotesClient.setState(mContext, noteIds, state);
        notifyDataChanged(mContext);
    }

    public Note getNote(long id) {
        return NotesClient.getNote(mContext, id);
    }

    public List<OrgProperty> getNoteProperties(long id) {
        return NotesClient.getNoteProperties(mContext, id);
    }

    public Note getNote(String title) {
        return NotesClient.getNote(mContext, title);
    }

    public int updateNote(Note note) {
        int result = NotesClient.update(mContext, note);
        notifyDataChanged(mContext);
        return result;
    }

    public Note createNote(Note note, NotePlace target) {
        /* Create new note. */
        Note insertedNote = NotesClient.create(mContext, note, target);

        BooksClient.setModifiedTime(mContext, note.getPosition().getBookId(), System.currentTimeMillis());

        notifyDataChanged(mContext);

        return insertedNote;
    }

    public String[] getAllTags(long bookId) {
        return NotesClient.getAllTags(mContext, bookId);
    }

    public void toggleFoldedState(long noteId) {
        NotesClient.toggleFoldedState(mContext, noteId);
    }

    public void promoteNotes(long bookId, Set<Long> noteIds) {
        BooksClient.promote(mContext, bookId, noteIds);
    }

    public int promoteNotes(long bookId, long noteId) {
        Set<Long> noteIds = new HashSet<>();
        noteIds.add(noteId);

        return BooksClient.promote(mContext, bookId, noteIds);
    }

    public void demoteNotes(long bookId, Set<Long> noteIds) {
        BooksClient.demote(mContext, bookId, noteIds);
    }

    public int demoteNotes(long bookId, long noteId) {
        Set<Long> noteIds = new HashSet<>();
        noteIds.add(noteId);

        return BooksClient.demote(mContext, bookId, noteIds);
    }

    public int cut(long bookId, long noteId) {
        Set<Long> noteIds = new HashSet<>();
        noteIds.add(noteId);

        return cut(bookId, noteIds);
    }

    public int cut(long bookId, Set<Long> noteIds) {
        int result = NotesClient.cut(mContext, bookId, noteIds);
        notifyDataChanged(mContext);
        return result;
    }

    public NotesBatch paste(long bookId, long noteId, Place place) {
        NotesBatch batch = NotesClient.paste(mContext, bookId, noteId, place);
        notifyDataChanged(mContext);
        return batch;

    }

    public int delete(long bookId, Set<Long> noteIds) {
        int result = NotesClient.delete(mContext, bookId, noteIds);
        notifyDataChanged(mContext);
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
                        namesake.getStatus().msg(UriUtils.friendlyUri(namesake.getRooks().get(0).getUri())));
                break;

            case BOOK_WITH_LINK_AND_ROOK_MODIFIED:
                loadBookFromRepo(namesake.getLatestLinkedRook());
                bookAction = new BookAction(BookAction.Type.INFO,
                        namesake.getStatus().msg(UriUtils.friendlyUri(namesake.getLatestLinkedRook().getUri())));
                break;

            case DUMMY_WITH_LINK:
                loadBookFromRepo(namesake.getLatestLinkedRook());
                bookAction = new BookAction(BookAction.Type.INFO,
                        namesake.getStatus().msg(UriUtils.friendlyUri(namesake.getLatestLinkedRook().getUri())));
                break;

            /* Save local book to repository. */

            case ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO:
                /* Save local book to the one and only repository. */
                repoUrl = getAllRepos().entrySet().iterator().next().getValue().getUri().toString();
                fileName = BookName.fileName(namesake.getBook().getName(), BookName.Format.ORG);
                saveBookToRepo(repoUrl, fileName, namesake.getBook(), BookName.Format.ORG);
                bookAction = new BookAction(BookAction.Type.INFO, namesake.getStatus().msg(UriUtils.friendlyUri(repoUrl)));
                break;

            case BOOK_WITH_LINK_LOCAL_MODIFIED:
                repoUrl = namesake.getBook().getLastSyncedToRook().getRepoUri().toString();
                fileName = BookName.getFileName(mContext, namesake.getBook().getLastSyncedToRook().getUri());
                saveBookToRepo(repoUrl, fileName, namesake.getBook(), BookName.Format.ORG);
                bookAction = new BookAction(BookAction.Type.INFO, namesake.getStatus().msg(UriUtils.friendlyUri(repoUrl)));
                break;

            case ONLY_BOOK_WITH_LINK:
                repoUrl = namesake.getBook().getLink().getRepoUri().toString();
                fileName = BookName.getFileName(mContext, namesake.getBook().getLink().getUri());
                saveBookToRepo(repoUrl, fileName, namesake.getBook(), BookName.Format.ORG);
                bookAction = new BookAction(BookAction.Type.INFO, namesake.getStatus().msg(UriUtils.friendlyUri(repoUrl)));
                break;
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Syncing " + namesake + ": " + bookAction);
        return bookAction;
    }

    /**
     * Downloads remote book, parses it and stores it to {@link Shelf}.
     * @return book now linked to remote one
     * @throws IOException
     */
    public Book loadBookFromRepo(Rook rook) throws IOException {
        Book book;

        Repo repo = RepoFactory.getFromUri(mContext, rook.getRepoUri());
        if (repo == null) {
            throw new IOException("Unsupported repository URL \"" + rook.getRepoUri() + "\"");
        }

        File tmpFile = getTempBookFile();

        try {
            /* Download from repo. */
            VersionedRook vrook = repo.retrieveBook(rook.getUri(), tmpFile);

            String fileName = BookName.getFileName(mContext, vrook.getUri());
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
        Repo repo = RepoFactory.getFromUri(mContext, repoUrl);

        if (repo == null) {
            throw new IOException("Unsupported repository URL \"" + repoUrl + "\"");
        }

        VersionedRook uploadedBook;

        File tmpFile = getTempBookFile();
        try {
            /* Write to temporary file. */
            writeBookToFile(book, format, tmpFile);

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

    public int updateBookSettings(Book book) {
        return BooksClient.updateSettings(mContext, book);
    }

    public void renameBook(Book book, String name) throws IOException {
        String oldName = book.getName();

        /* Make sure there is no notebook with this name. */
        if (getBook(name) != null) {
            throw new IOException("Notebook with that name already exists");
        }

        /* Make sure link's repo is the same as sync book repo. */
        if (book.getLink() != null && book.getLastSyncedToRook() != null) {
            if (! book.getLink().getUri().equals(book.getLastSyncedToRook().getUri())) {
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
        Note note = NotesClient.getNote(mContext, id);
        shiftState(note, direction);
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

        Set<Long> noteIds = new HashSet<>();
        noteIds.add(note.getId());

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Setting state for " + note.getHead().getTitle() + " to " + nextState +
                                                   ": " + currentIndex + " -> " + nextIndex + " (" + allStates.size() + " total states)");

        setNotesState(noteIds, nextState);
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
            String fileName;

            /* Use file name used in last sync, if last sync exists. */
            if (book.getLastSyncedToRook() != null) {
                fileName = BookName.getFileName(mContext, book.getLastSyncedToRook().getUri());
            } else {
                fileName = BookName.fileName(book.getName(), BookName.Format.ORG);
            }

            Uri rookUri = Uri.parse(repoUrl).buildUpon().appendPath(fileName).build();

            BooksClient.setLink(mContext, book.getId(), repoUrl, rookUri.toString());
        }
    }

    public void setStateToDone(long noteId) {
        /* Get the *first* DONE state from preferences. */
        Set<String> doneStates = AppPreferences.doneKeywordsSet(mContext);
        String firstState = doneStates.iterator().hasNext() ? doneStates.iterator().next() : null;

        if (firstState != null) {
            Set<Long> ids = new TreeSet<>();
            ids.add(noteId);

            setNotesState(ids, firstState);
        }
    }

    public static void notifyDataChanged(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        ReminderService.notifyDataChanged(context);

        context.sendBroadcast(new Intent(AppIntent.ACTION_LIST_WIDGET_UPDATE));
    }

    public interface ReParsingNotesListener {
        void noteParsed(int current, int total, String msg);
    }

    /**
     * Using current states configuration, update states and titles for all notes.
     * Also updates created time in the database for all notes based on the created property keyword,
     * but does not affect the property itself in the org file.
     * Keywords that were part of the title can become states and vice versa.
     * Affected books' mtime will *not* be updated.
     *
     * @return Number of modified notes.
     */
    public int reParseNotesStateAndTitles(ReParsingNotesListener listener) throws IOException {
        int modifiedNotesCount = 0;

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        /* Get all notes. */
        Cursor cursor = mContext.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notes(), null, null, null, null);

        try {
            int current = 0;
            int total = 0;

            OrgParser.Builder parserBuilder = new OrgParser.Builder()
                    .setTodoKeywords(AppPreferences.todoKeywordsSet(mContext))
                    .setDoneKeywords(AppPreferences.doneKeywordsSet(mContext));

            OrgParserWriter parserWriter = new OrgParserWriter();

            /* Get total number of notes for displaying the stats. */
            if (cursor.moveToFirst()) {
                total = cursor.getCount();
            }

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                current++;

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

                    modifiedNotesCount++;

                    values.put(ProviderContract.Notes.UpdateParam.TITLE, newHead.getTitle());
                    values.put(ProviderContract.Notes.UpdateParam.STATE, newHead.getState());
                    values.put(ProviderContract.Notes.UpdateParam.PRIORITY, newHead.getPriority());
                }

                /* Update created time.
                 * Because the note was only modified in the internal database, modifiedNotesCount
                 * isn't incremented.
                 * We use the properties from the database because NotesClient.fromCursor returns
                 * a note without any properties
                 */
                found: {
                    for (OrgProperty prop : NotesClient.getNoteProperties(mContext, NotesClient.idFromCursor(cursor))) {
                        if (prop.getName().equals(AppPreferences.createdAtProperty(mContext))) {
                            try {
                                OrgDateTime x = OrgDateTime.parse(prop.getValue());
                                values.put(DbNote.Column.CREATED_AT, x.getCalendar().getTimeInMillis());
                                break found;
                            } catch (IllegalArgumentException e) {
                                // Parsing failed, give up immediately and insert null
                                break;
                            }
                        }
                    }
                    if (cursor.getColumnIndex(DbNote.Column.CREATED_AT_INTERNAL) != -1) {
                        values.put(DbNote.Column.CREATED_AT, cursor.getLong(cursor.getColumnIndex(DbNote.Column.CREATED_AT_INTERNAL)));
                    } else {
                        long d = new Date().getTime();
                        values.put(DbNote.Column.CREATED_AT, d);
                        values.put(DbNote.Column.CREATED_AT_INTERNAL, d);
                    }
                }

                ops.add(ContentProviderOperation
                        .newUpdate(ContentUris.withAppendedId(ProviderContract.Notes.ContentUri.notes(), cursor.getLong(0)))
                        .withValues(values)
                        .build()
                );

                if (listener != null) {
                    listener.noteParsed(current, total, "Updating notes...");
                }
            }

        } finally {
            cursor.close();
        }

        if (listener != null) {
            listener.noteParsed(0, 0, "Updating database...");
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

        return modifiedNotesCount;
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
