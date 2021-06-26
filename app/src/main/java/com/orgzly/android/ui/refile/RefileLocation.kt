package com.orgzly.android.ui.refile

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName

data class RefileLocation(
        val type: Type? = null,

        val id: Long? = null,

        val title: String? = null
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    enum class Type {
        @SerializedName("home")
        HOME,
        @SerializedName("book")
        BOOK,
        @SerializedName("note")
        NOTE
    }

    companion object {
        fun forHome(): RefileLocation {
            return RefileLocation(Type.HOME)
        }

        fun forBook(id: Long, title: String): RefileLocation {
            return RefileLocation(Type.BOOK, id, title)
        }

        fun forNote(id: Long, title: String): RefileLocation {
            return RefileLocation(Type.NOTE, id, title)
        }

        fun fromJson(json: String?): RefileLocation? {
            return if (json != null && json.isNotEmpty()) {
                try {
                    Gson().fromJson(json, RefileLocation::class.java)
                } catch (e: JsonSyntaxException) {
                    null
                }
            } else {
                null
            }
        }
    }
}