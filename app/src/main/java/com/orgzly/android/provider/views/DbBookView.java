package com.orgzly.android.provider.views;

import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.GenericDatabaseUtils;
import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbBookColumns;
import com.orgzly.android.provider.models.DbBookLink;
import com.orgzly.android.provider.models.DbBookSync;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbRepo;
import com.orgzly.android.provider.models.DbRook;
import com.orgzly.android.provider.models.DbRookUrl;
import com.orgzly.android.provider.models.DbVersionedRook;

import static com.orgzly.android.provider.GenericDatabaseUtils.field;

/**
 * Books with link's data.
 */
public class DbBookView implements BaseColumns, DbBookColumns, DbBookViewColumns {
    public static final String VIEW_NAME = "books_view";

    public static final String DROP_SQL = "DROP VIEW IF EXISTS " + VIEW_NAME;

    public static final String CREATE_SQL =
            "CREATE VIEW " + VIEW_NAME + " AS " +
            "SELECT " + DbBook.TABLE + ".*, " +

            "t_link_repo.repo_url AS " + LINK_REPO_URL + ", " +

            "t_link_rook_urls.rook_url AS " + ROOK_URL + ", " +
            "t_link_rook_repos.repo_url AS " + ROOK_REPO_URL + ", " +

            "t_sync_revision_rook_repos.repo_url AS " + SYNCED_REPO_URL + ", " +
            "t_sync_revision_rook_urls.rook_url AS " + SYNCED_ROOK_URL + ", " +
            "t_sync_revisions.rook_revision AS " + SYNCED_ROOK_REVISION + ", " +
            "t_sync_revisions.rook_mtime AS " + SYNCED_ROOK_MTIME + ", " +

            "count(t_notes._id) AS " + NOTES_COUNT + " " +

            "FROM " + DbBook.TABLE + " " +

            GenericDatabaseUtils.join(DbBookLink.TABLE, "t_links", DbBookLink.BOOK_ID, DbBook.TABLE, DbBook._ID) +

            GenericDatabaseUtils.join(DbRepo.TABLE, "t_link_repo", DbRepo._ID, "t_links", DbBookLink.REPO_ID) +

            GenericDatabaseUtils.join(DbRook.TABLE, "t_link_rooks", DbRook._ID, "t_links", DbBookLink.ROOK_ID) +
            GenericDatabaseUtils.join(DbRepo.TABLE, "t_link_rook_repos", DbRepo._ID, "t_link_rooks", DbRook.REPO_ID) +
            GenericDatabaseUtils.join(DbRookUrl.TABLE, "t_link_rook_urls", DbRookUrl._ID, "t_link_rooks", DbRook.ROOK_URL_ID) +

            GenericDatabaseUtils.join(DbBookSync.TABLE, "t_syncs", DbBookSync.BOOK_ID, DbBook.TABLE, DbBook._ID) +
            GenericDatabaseUtils.join(DbVersionedRook.TABLE, "t_sync_revisions", DbVersionedRook._ID, "t_syncs", DbBookSync.BOOK_VERSIONED_ROOK_ID) +
            GenericDatabaseUtils.join(DbRook.TABLE, "t_sync_revision_rooks", DbRook._ID, "t_sync_revisions", DbVersionedRook.ROOK_ID) +
            GenericDatabaseUtils.join(DbRepo.TABLE, "t_sync_revision_rook_repos", DbRepo._ID, "t_sync_revision_rooks", DbRook.REPO_ID) +
            GenericDatabaseUtils.join(DbRookUrl.TABLE, "t_sync_revision_rook_urls", DbRookUrl._ID, "t_sync_revision_rooks", DbRook.ROOK_URL_ID) +

            GenericDatabaseUtils.join(DbNote.TABLE, "t_notes", DbNote.BOOK_ID, DbBook.TABLE, DbBook._ID) + " AND " + DatabaseUtils.WHERE_EXISTING_NOTES +

            " GROUP BY " + field(DbBook.TABLE, DbBook._ID);

    public static final String[] PROJECTION = {
            _ID,
            NAME,
            TITLE,
            MTIME,
            IS_DUMMY,
            IS_DELETED,
            PREFACE,
            IS_INDENTED,
            USED_ENCODING,
            DETECTED_ENCODING,
            SELECTED_ENCODING,
            SYNC_STATUS,
            LAST_ACTION_TIMESTAMP,
            LAST_ACTION_TYPE,
            LAST_ACTION,
            LINK_REPO_URL,
            ROOK_URL,
            ROOK_REPO_URL,
            SYNCED_REPO_URL,
            SYNCED_ROOK_URL,
            SYNCED_ROOK_REVISION,
            SYNCED_ROOK_MTIME,
            NOTES_COUNT
    };
}
