package com.orgzly.android.provider;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.android.AppIntent;
import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbBookLink;
import com.orgzly.android.provider.models.DbBookSync;
import com.orgzly.android.provider.models.DbCurrentVersionedRook;
import com.orgzly.android.provider.models.DbDbRepo;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbNoteAncestor;
import com.orgzly.android.provider.models.DbNoteProperty;
import com.orgzly.android.provider.models.DbOrgRange;
import com.orgzly.android.provider.models.DbOrgTimestamp;
import com.orgzly.android.provider.models.DbProperty;
import com.orgzly.android.provider.models.DbPropertyName;
import com.orgzly.android.provider.models.DbPropertyValue;
import com.orgzly.android.provider.models.DbRepo;
import com.orgzly.android.provider.models.DbRook;
import com.orgzly.android.provider.models.DbRookUrl;
import com.orgzly.android.provider.models.DbSearch;
import com.orgzly.android.provider.models.DbVersionedRook;
import com.orgzly.android.provider.views.DbNoteBasicView;
import com.orgzly.android.provider.views.DbTimeView;
import com.orgzly.android.provider.views.DbBookView;
import com.orgzly.android.provider.views.DbNoteView;
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

        setWriteAheadLoggingEnabled(true);
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

        DatabaseMigration
                .upgrade(db, context, oldVersion, () -> LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(AppIntent.ACTION_DB_UPGRADE_STARTED)));

        createAllViews(db);

        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(AppIntent.ACTION_DB_UPGRADE_ENDED));
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
        for (String sql : DbBook.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbBookLink.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbBookSync.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbCurrentVersionedRook.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbDbRepo.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbNote.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbNoteAncestor.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbNoteProperty.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbOrgRange.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbOrgTimestamp.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbProperty.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbPropertyName.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbPropertyValue.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbRepo.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbRook.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbRookUrl.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbSearch.CREATE_SQL) db.execSQL(sql);
        for (String sql : DbVersionedRook.CREATE_SQL) db.execSQL(sql);
    }

    private void dropAllTables(SQLiteDatabase db) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db.getPath());

        /* DROP tables */
        db.execSQL(DbBook.DROP_SQL);
        db.execSQL(DbBookLink.DROP_SQL);
        db.execSQL(DbBookSync.DROP_SQL);
        db.execSQL(DbCurrentVersionedRook.DROP_SQL);
        db.execSQL(DbDbRepo.DROP_SQL);
        db.execSQL(DbNote.DROP_SQL);
        db.execSQL(DbNoteAncestor.DROP_SQL);
        db.execSQL(DbNoteProperty.DROP_SQL);
        db.execSQL(DbOrgRange.DROP_SQL);
        db.execSQL(DbOrgTimestamp.DROP_SQL);
        db.execSQL(DbProperty.DROP_SQL);
        db.execSQL(DbPropertyName.DROP_SQL);
        db.execSQL(DbPropertyValue.DROP_SQL);
        db.execSQL(DbRepo.DROP_SQL);
        db.execSQL(DbRook.DROP_SQL);
        db.execSQL(DbRookUrl.DROP_SQL);
        db.execSQL(DbSearch.DROP_SQL);
        db.execSQL(DbVersionedRook.DROP_SQL);
    }

    /** DROP views */
    private void dropAllViews(SQLiteDatabase db) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db.getPath());

        db.execSQL(DbNoteBasicView.DROP_SQL);
        db.execSQL(DbNoteView.DROP_SQL);
        db.execSQL(DbBookView.DROP_SQL);
        db.execSQL(DbTimeView.DROP_SQL);
    }

    /** CREATE views */
    private void createAllViews(SQLiteDatabase db) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db.getPath());

        db.execSQL(DbNoteBasicView.CREATE_SQL);
        db.execSQL(DbNoteView.CREATE_SQL);
        db.execSQL(DbBookView.CREATE_SQL);
        db.execSQL(DbTimeView.CREATE_SQL);
    }
}
