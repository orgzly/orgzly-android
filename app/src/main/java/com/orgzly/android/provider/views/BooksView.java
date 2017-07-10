package com.orgzly.android.provider.views;

import android.provider.BaseColumns;

import com.orgzly.android.provider.GenericDatabaseUtils;
import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbBookLink;
import com.orgzly.android.provider.models.DbBookSync;
import com.orgzly.android.provider.models.DbRepo;
import com.orgzly.android.provider.models.DbRook;
import com.orgzly.android.provider.models.DbRookUrl;
import com.orgzly.android.provider.models.DbVersionedRook;

/**
 * Books with link's data.
 */
public class BooksView {
    public static final String VIEW_NAME = "books_view";

    public static final String DROP_SQL = "DROP VIEW IF EXISTS " + VIEW_NAME;

    public static final String CREATE_SQL =
            "CREATE VIEW " + VIEW_NAME + " AS " +
            "SELECT " + DbBook.TABLE + ".*, " +

            "t_link_rook_repos.repo_url AS " + Columns.LINK_REPO_URL + ", " +
            "t_link_rook_urls.rook_url AS " + Columns.LINK_ROOK_URL + ", " +

            "t_sync_revision_rook_repos.repo_url AS " + Columns.SYNCED_REPO_URL + ", " +
            "t_sync_revision_rook_urls.rook_url AS " + Columns.SYNCED_ROOK_URL + ", " +
            "t_sync_revisions.rook_revision AS " + Columns.SYNCED_ROOK_REVISION + ", " +
            "t_sync_revisions.rook_mtime AS " + Columns.SYNCED_ROOK_MTIME + " " +

            "FROM " + DbBook.TABLE + " " +

            GenericDatabaseUtils.join(DbBookLink.TABLE, "t_links", DbBookLink.Column.BOOK_ID, DbBook.TABLE, DbBook.Column._ID) +
            GenericDatabaseUtils.join(DbRook.TABLE, "t_link_rooks", DbRook.Column._ID, "t_links", DbBookLink.Column.ROOK_ID) +
            GenericDatabaseUtils.join(DbRepo.TABLE, "t_link_rook_repos", DbRepo.Column._ID, "t_link_rooks", DbRook.Column.REPO_ID) +
            GenericDatabaseUtils.join(DbRookUrl.TABLE, "t_link_rook_urls", DbRookUrl.Column._ID, "t_link_rooks", DbRook.Column.ROOK_URL_ID) +

            GenericDatabaseUtils.join(DbBookSync.TABLE, "t_syncs", DbBookSync.Column.BOOK_ID, DbBook.TABLE, DbBook.Column._ID) +
            GenericDatabaseUtils.join(DbVersionedRook.TABLE, "t_sync_revisions", DbVersionedRook.Column._ID, "t_syncs", DbBookSync.Column.BOOK_VERSIONED_ROOK_ID) +
            GenericDatabaseUtils.join(DbRook.TABLE, "t_sync_revision_rooks", DbRook.Column._ID, "t_sync_revisions", DbVersionedRook.Column.ROOK_ID) +
            GenericDatabaseUtils.join(DbRepo.TABLE, "t_sync_revision_rook_repos", DbRepo.Column._ID, "t_sync_revision_rooks", DbRook.Column.REPO_ID) +
            GenericDatabaseUtils.join(DbRookUrl.TABLE, "t_sync_revision_rook_urls", DbRookUrl.Column._ID, "t_sync_revision_rooks", DbRook.Column.ROOK_URL_ID) +

            "";

    public static class Columns implements DbBook.Columns, BaseColumns {
        public static String LINK_REPO_URL = "link_repo_url";
        public static String LINK_ROOK_URL = "link_rook_url";

        public static String SYNCED_REPO_URL      = "sync_repo_url";
        public static String SYNCED_ROOK_URL      = "sync_rook_url";
        public static String SYNCED_ROOK_REVISION = "sync_rook_revision";
        public static String SYNCED_ROOK_MTIME    = "sync_rook_mtime";
    }
}
