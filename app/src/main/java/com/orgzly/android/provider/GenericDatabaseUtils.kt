package com.orgzly.android.provider


import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import java.util.*

/**
 * Generic database utilities, not specific to Orgzly database schema.
 */
object GenericDatabaseUtils {
    @JvmStatic
    fun incrementFields(db: SQLiteDatabase, table: String, selection: String, count: Int, vararg fields: String) {
        if (fields.isEmpty()) {
            throw IllegalArgumentException("No fields passed to incrementFields")
        }

        val updates = ArrayList<String>()

        for (field in fields) {
            updates.add("$field = $field + ($count)")
        }

        val sql = "UPDATE " + table + " SET " + TextUtils.join(", ", updates) + " WHERE " + selection

        db.execSQL(sql)
    }

    @JvmStatic
    fun whereNullOrZero(field: String): String {
        return "($field IS NULL OR $field = 0 )"
    }

    @JvmStatic
    fun getCount(context: Context, uri: Uri, selection: String): Int {
        val cursor = context.contentResolver.query(
                uri, arrayOf("COUNT(*) AS count"), selection, null, null)

        cursor.use {
            return if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
    }

    @JvmStatic
    fun join(table: String, alias: String, field1: String, onTable: String, field2: String): String {
        return " LEFT OUTER JOIN " + table + " " + alias + " ON " + field(alias, field1) + " = " + field(onTable, field2) + " "
    }

    @JvmStatic
    fun field(table: String, name: String): String {
        return "$table.$name"
    }

    @JvmStatic
    fun ms2StartOfDay(s: String): String {
        return "datetime($s/1000, 'unixepoch', 'localtime', 'start of day')"
    }

    @JvmStatic
    fun forEachRow(cursor: Cursor, f: (Cursor) -> Unit) {
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            f(cursor)
            cursor.moveToNext()
        }
    }

    @JvmStatic
    fun delete(db: SQLiteDatabase, table: String, id: Long): Int {
        return db.delete(table, BaseColumns._ID + " = " + id, null)
    }
}
