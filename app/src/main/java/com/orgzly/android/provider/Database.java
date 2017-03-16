package com.orgzly.android.provider;

import android.content.Context;
import android.content.Intent;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.android.Broadcasts;
import com.orgzly.android.provider.models.*;
import com.orgzly.android.provider.views.BooksView;
import com.orgzly.android.provider.views.NotesView;
import com.orgzly.android.util.LogUtils;

/**
 * Helper class that actually creates and manages the provider's underlying data repository.
 */
public class Database extends SQLiteOpenHelper {
    private static final String TAG = Database.class.getName();

    /** Context kept for broadcasts. */
    private Context context;

    /**
     * Instantiates an open helper for the provider's SQLite data repository.
     */
    public Database(Context context, String name) {
        super(context, name, null, DatabaseMigration.DB_VER_CURRENT);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, name, DatabaseMigration.DB_VER_CURRENT);

        this.context = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setWriteAheadLoggingEnabled(true);
        }
    }

    /**
     * Called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database for version " + DatabaseMigration.DB_VER_CURRENT);

        createAllTables(db);
        createAllViews(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from " + oldVersion + " to " + newVersion);

        /* Simply drop all views and create them after the upgrade. */
        dropAllViews(db);

        DatabaseMigration.upgrade(db, oldVersion, new Runnable() {
            @Override
            public void run() {
                LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(new Intent(Broadcasts.ACTION_DB_UPGRADE_STARTED));
            }
        });

        createAllViews(db);

        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(Broadcasts.ACTION_DB_UPGRADE_ENDED));
    }

    /**
     * User-requested recreate of all tables.
     */
    public void reCreateTables(SQLiteDatabase db) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db.getPath());

        dropAllViews(db);
        dropAllTables(db);

        onCreate(db);
    }

    private void createAllTables(SQLiteDatabase db) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db.getPath());

        /* CREATE tables */
        for (String sql : DbRepo.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbBook.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbNote.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbOrgTimestamp.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbOrgRange.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbSearch.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbBookLink.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbBookSync.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbRookUrl.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbRook.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbVersionedRook.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbCurrentVersionedRook.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbDbRepo.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbNoteProperty.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbPropertyName.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbPropertyValue.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbProperty.CREATE_SQL) db.execSQL(sql);
    }

    private void dropAllTables(SQLiteDatabase db) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db.getPath());

        /* DROP tables */
        db.execSQL(DbRepo.DROP_SQL);
        db.execSQL(DbBook.DROP_SQL);
        db.execSQL(DbNote.DROP_SQL);
        db.execSQL(DbOrgTimestamp.DROP_SQL);
        db.execSQL(DbOrgRange.DROP_SQL);
        db.execSQL(DbSearch.DROP_SQL);
        db.execSQL(DbBookLink.DROP_SQL);
        db.execSQL(DbBookSync.DROP_SQL);
        db.execSQL(DbRookUrl.DROP_SQL);
        db.execSQL(DbRook.DROP_SQL);
        db.execSQL(DbVersionedRook.DROP_SQL);
        db.execSQL(DbCurrentVersionedRook.DROP_SQL);
        db.execSQL(DbDbRepo.DROP_SQL);
        db.execSQL(DbNoteProperty.DROP_SQL);
        db.execSQL(DbPropertyName.DROP_SQL);
        db.execSQL(DbPropertyValue.DROP_SQL);
        db.execSQL(DbProperty.DROP_SQL);
    }

    /** DROP views */
    private void dropAllViews(SQLiteDatabase db) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db.getPath());

        db.execSQL(NotesView.DROP_SQL);
        db.execSQL(BooksView.DROP_SQL);
    }

    /** CREATE views */
    private void createAllViews(SQLiteDatabase db) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db.getPath());

        db.execSQL(NotesView.CREATE_SQL);
        db.execSQL(BooksView.CREATE_SQL);
    }
}
