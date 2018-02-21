package com.orgzly.android.provider;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import com.orgzly.android.provider.models.DbBookColumns;
import com.orgzly.android.provider.models.DbDbRepoColumns;
import com.orgzly.android.provider.models.DbNoteColumns;
import com.orgzly.android.provider.models.DbRepoColumns;
import com.orgzly.android.provider.models.DbSearchColumns;
import com.orgzly.android.provider.views.DbBookViewColumns;
import com.orgzly.android.ui.NotePlace;

/**
 * Content provider's contract.
 */
public class ProviderContract {
    public static final String AUTHORITY = "com.orgzly";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public interface Books {
        class Param implements DbBookViewColumns, DbBookColumns, BaseColumns {
        }

        interface MatcherUri {
            String BOOKS = "books";
            String BOOKS_ID = BOOKS + "/#";
            String BOOKS_ID_NOTES = BOOKS + "/#/notes";
            String BOOKS_ID_CYCLE_VISIBILITY = BOOKS + "/#/cycle-visibility";
            String BOOKS_ID_SPARSE_TREE = BOOKS + "/#/sparse-tree";
        }

        class ContentUri {
            public static Uri books() {
                return Uri.withAppendedPath(AUTHORITY_URI, MatcherUri.BOOKS);
            }

            public static Uri booksId(long bookId) {
                return ContentUris.withAppendedId(books(), bookId);
            }

            public static Uri booksIdNotes(long bookId) {
                return booksId(bookId).buildUpon().appendPath("notes").build();
            }


            public static Uri booksIdCycleVisibility(long id) {
                return ContentUris.withAppendedId(books(), id).buildUpon()
                        .appendPath("cycle-visibility").build();
            }

            public static Uri booksIdSparseTree(long bookId) {
                return ContentUris.withAppendedId(books(), bookId).buildUpon()
                        .appendPath("sparse-tree").build();
            }
        }
    }

    public interface Filters {
        class Param implements DbSearchColumns, BaseColumns {
        }

        interface MatcherUri {
            String FILTERS = "filters";
            String FILTERS_ID = FILTERS + "/#";
            String FILTERS_ID_UP = FILTERS + "/#/up";
            String FILTERS_ID_DOWN = FILTERS + "/#/down";
        }

        class ContentUri {
            public static Uri filters() {
                return Uri.withAppendedPath(AUTHORITY_URI, MatcherUri.FILTERS);
            }

            public static Uri filtersIdUp(long id) {
                return ContentUris.withAppendedId(filters(), id).buildUpon()
                        .appendPath("up").build();
            }

            public static Uri filtersIdDown(long id) {
                return ContentUris.withAppendedId(filters(), id).buildUpon()
                        .appendPath("down").build();
            }
        }
    }

    public interface Repos {
        class Param implements DbRepoColumns, BaseColumns {
        }

        interface MatcherUri {
            String REPOS = "repos";
            String REPOS_ID = REPOS + "/#";
        }

        class ContentUri {
            public static Uri repos() {
                return Uri.withAppendedPath(AUTHORITY_URI, MatcherUri.REPOS);
            }
        }
    }

    public interface NoteProperties {
        class Param {
            public static final String NOTE_ID = "note_id";
            public static final String NAME = "name";
            public static final String VALUE = "value";
            public static final String POSITION = "position";
        }

        interface MatcherUri {
            String NOTES_PROPERTIES = "notes/properties";
            String NOTES_ID_PROPERTIES = "notes/#/properties";
        }

        class ContentUri {
            public static Uri notesProperties() {
                return Uri.withAppendedPath(AUTHORITY_URI, MatcherUri.NOTES_PROPERTIES);
            }

            public static Uri notesIdProperties(long id) {
                Uri.Builder builder = AUTHORITY_URI.buildUpon();
                builder = builder.appendPath("notes");
                builder = ContentUris.appendId(builder, id);
                builder = builder.appendPath("properties");
                return builder.build();
            }
        }
    }

    public interface Notes {
        class Param {
            public static final String PROPERTY_NAME = "property_name";
            public static final String PROPERTY_VALUE = "property_value";
        }

        class UpdateParam implements DbNoteColumns, BaseColumns {
            public static final String SCHEDULED_STRING = "scheduled_string"; // TODO: This is range, rename.
            public static final String DEADLINE_STRING = "deadline_string";
            public static final String CLOSED_STRING = "closed_string";
            public static final String CLOCK_STRING = "clock_string";
        }


        interface MatcherUri {
            String NOTES = "notes";
            String NOTES_SEARCH_QUERIED = NOTES + "/queried";
            String NOTES_STATE = NOTES + "/state";
            String NOTES_WITH_PROPERTY = NOTES + "/with-property";
            String NOTES_ID = NOTES + "/#";
            String NOTES_ID_ABOVE = NOTES + "/#/above";
            String NOTES_ID_BELOW = NOTES + "/#/below";
            String NOTES_ID_UNDER = NOTES + "/#/under";
            String NOTES_ID_TOGGLE_FOLDED_STATE = NOTES + "/#/toggle-folded-state";
        }

        class ContentUri {
            public static Uri notes() {
                return Uri.withAppendedPath(AUTHORITY_URI, MatcherUri.NOTES);
            }

            public static Uri notesWithExtras() {
                return Uri.withAppendedPath(AUTHORITY_URI, MatcherUri.NOTES)
                        .buildUpon()
                        .appendQueryParameter("with-extras", "yes")
                        .build();
            }

            public static Uri notesId(long id) {
                return ContentUris.withAppendedId(notes(), id);
            }

            public static Uri notesWithProperty(String propName, String propValue) {
                return Uri.withAppendedPath(AUTHORITY_URI, MatcherUri.NOTES_WITH_PROPERTY).buildUpon()
                        .appendQueryParameter(Param.PROPERTY_NAME, propName)
                        .appendQueryParameter(Param.PROPERTY_VALUE, propValue).build();
            }

            public static Uri notesIdTarget(NotePlace target) {
                Uri.Builder builder = notesId(target.getNoteId()).buildUpon();

                switch (target.getPlace()) {
                    case ABOVE:
                        builder.appendPath("above");
                        break;
                    case UNDER:
                        builder.appendPath("under");
                        break;
                    case BELOW:
                        builder.appendPath("below");
                        break;
                }

                return builder.build();
            }

            public static Uri notesIdToggleFoldedState(long id) {
                return notesId(id).buildUpon().appendPath("toggle-folded-state").build();
            }

            public static Uri notesSearchQueried(String query) {
                return notes().buildUpon().appendPath("queried").query(query).build();
            }
        }
    }

    public interface CurrentRooks {
        class Param {
            public static final String REPO_URL      = "repo_url";
            public static final String ROOK_URL      = "rook_url";
            public static final String ROOK_REVISION = "rook_revision";
            public static final String ROOK_MTIME    = "rook_mtime";
        }

        interface MatcherUri {
            String CURRENT_ROOKS = "current-rooks";
        }

        class ContentUri {
            public static Uri currentRooks() {
                return Uri.withAppendedPath(AUTHORITY_URI, "current-rooks");
            }
        }
    }

    public interface BookLinks {
        class Param {
            public static final String REPO_URL = "repo_url";
            public static final String ROOK_URL = "rook_url";
        }

        interface MatcherUri {
            String BOOKS_ID_LINKS = "books/#/links";
        }

        class ContentUri {
            public static Uri booksIdLinks(long id) {
                Uri.Builder builder = AUTHORITY_URI.buildUpon();
                builder = builder.appendPath("books");
                builder = ContentUris.appendId(builder, id);
                builder = builder.appendPath("links");
                return builder.build();
            }
        }
    }

    public interface Paste {
        class Param {
            public static final String NOTE_ID = "note_id";
            public static final String BATCH_ID = "batch_id";
            public static final String SPOT = "spot";
        }

        interface MatcherUri {
            String PASTE = "paste";
        }

        class ContentUri {
            public static Uri paste() {
                return Uri.withAppendedPath(AUTHORITY_URI, "paste");
            }
        }
    }

    public interface Cut {
        class Param {
            public static final String BOOK_ID = "book_id";
            public static final String IDS = "ids";
        }

        interface MatcherUri {
            String CUT = "cut";
        }

        class ContentUri {
            public static Uri cut() {
                return Uri.withAppendedPath(AUTHORITY_URI, "cut");
            }
        }
    }

    public interface Delete {
        class Param {
            public static final String BOOK_ID = "book_id";
            public static final String IDS = "ids";
        }

        interface MatcherUri {
            String DELETE = "delete";
        }

        class ContentUri {
            public static Uri delete() {
                return Uri.withAppendedPath(AUTHORITY_URI, "delete");
            }
        }
    }

    public interface NotesState {
        class Param {
            public static final String NOTE_IDS = "note_ids";
            public static final String STATE = "state";
        }

        class ContentUri {
            public static Uri notesState() {
                return Uri.withAppendedPath(AUTHORITY_URI, "notes/state");
            }
        }
    }

    public interface Promote {
        class Param {
            public static final String BOOK_ID = "book_id";
            public static final String IDS = "ids";
        }

        interface MatcherUri {
            String PROMOTE = "promote";
        }

        class ContentUri {
            public static Uri promote() {
                return Uri.withAppendedPath(AUTHORITY_URI, "promote");
            }
        }
    }

    public interface Demote {
        class Param {
            public static final String BOOK_ID = "book_id";
            public static final String IDS = "ids";
        }

        interface MatcherUri {
            String DEMOTE = "demote";
        }

        class ContentUri {
            public static Uri demote() {
                return Uri.withAppendedPath(AUTHORITY_URI, "demote");
            }
        }
    }

    public interface Move {
        class Param {
            public static final String BOOK_ID = "book_id";
            public static final String IDS = "ids";
            public static final String DIRECTION = "direction"; /* up or down */
        }

        interface MatcherUri {
            String MOVE = "move";
        }

        class ContentUri {
            public static Uri move() {
                return Uri.withAppendedPath(AUTHORITY_URI, "move");
            }
        }
    }

    public interface LoadBookFromFile {
        class Param {
            public static final String BOOK_NAME = "book_name";
            public static final String FILE_PATH = "file_path";
            public static final String FORMAT = "format";
            public static final String ROOK_URL = "rook_url";
            public static final String ROOK_REPO_URL = "rook_repo_url";
            public static final String ROOK_REVISION = "rook_revision";
            public static final String ROOK_MTIME = "rook_mtime";
            public static final String SELECTED_ENCODING = "selected_encoding";
        }

        interface MatcherUri {
            String LOAD_FROM_FILE = "load-from-file";
        }

        class ContentUri {
            public static Uri loadBookFromFile() {
                return Uri.withAppendedPath(AUTHORITY_URI, "load-from-file");
            }
        }
    }

    public interface LocalDbRepo {
        class Param implements DbDbRepoColumns {
        }

        interface MatcherUri {
            String DB_REPOS = "db-repos";
        }

        class ContentUri {
            public static Uri dbRepos() {
                return Uri.withAppendedPath(AUTHORITY_URI, "db-repos");
            }
        }
    }

    public interface DbTest {
        interface MatcherUri {
            String DB_TEST = "db/test";
        }

        class ContentUri {
            public static Uri dbTest() {
                return Uri.withAppendedPath(AUTHORITY_URI, "db/test");
            }
        }
    }

    public interface DbRecreate {
        interface MatcherUri {
            String DB_RECREATE = "db/recreate";
        }

        class ContentUri {
            public static Uri dbRecreate() {
                return Uri.withAppendedPath(AUTHORITY_URI, "db/recreate");
            }
        }
    }

    public interface BooksIdSaved {
        class Param {
            public static final String REPO_URL = "repo_url";
            public static final String ROOK_URL = "rook_url";
            public static final String ROOK_REVISION = "rook_revision";
            public static final String ROOK_MTIME = "rook_mtime";
        }

        interface MatcherUri {
            String BOOKS_ID_SAVED = "books/#/saved";
        }

        class ContentUri {
            public static Uri booksIdSaved(long id) {
                Uri.Builder builder = AUTHORITY_URI.buildUpon();
                builder = builder.appendPath("books");
                builder = ContentUris.appendId(builder, id);
                builder = builder.appendPath("saved");
                return builder.build();

            }
        }
    }

    public interface Times {
        interface MatcherUri {
            String TIMES = "times";
        }

        class ColumnIndex {
            public static final int NOTE_ID = 0;
            public static final int BOOK_ID = 1;
            public static final int BOOK_NAME = 2;
            public static final int NOTE_STATE = 3;
            public static final int NOTE_TITLE = 4;
            public static final int TIME_TYPE = 5;
            public static final int ORG_TIMESTAMP_STRING = 6;
        }

        class ContentUri {
            public static Uri times() {
                return Uri.withAppendedPath(AUTHORITY_URI, MatcherUri.TIMES)
                        .buildUpon()
                        .build();
            }
        }
    }
}
