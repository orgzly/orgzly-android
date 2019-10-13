@file:Suppress("unused")

package com.orgzly.android.db

import androidx.room.TypeConverter
import android.net.Uri
import com.orgzly.android.db.entity.BookAction
import com.orgzly.android.repos.RepoType

object TypeConverters {
    @TypeConverter
    @JvmStatic
    fun fromTypeToString(type: BookAction.Type): String {
        return type.toString()
    }

    @TypeConverter
    @JvmStatic
    fun fromStringToType(type: String): BookAction.Type {
        return BookAction.Type.valueOf(type)
    }

    @TypeConverter
    @JvmStatic
    fun fromUriToString(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    @JvmStatic
    fun fromStringToUri(uri: String?): Uri? {
        return if (uri == null) {
            null
        } else {
            Uri.parse(uri)
        }
    }

    @TypeConverter
    @JvmStatic
    fun fromIdToRepoType(id: Int): RepoType {
        return RepoType.fromId(id)
    }

    @TypeConverter
    @JvmStatic
    fun fromRepoTypeToId(type: RepoType): Int {
        return type.id
    }
}