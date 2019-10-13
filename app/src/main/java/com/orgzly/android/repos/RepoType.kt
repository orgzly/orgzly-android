package com.orgzly.android.repos

import java.lang.IllegalArgumentException

enum class RepoType(val id: Int) {
    MOCK(1),
    DROPBOX(2),
    DIRECTORY(3),
    DOCUMENT(4),
    WEBDAV(5),
    GIT(6);

    companion object {
        @JvmStatic
        fun fromId(type: Int): RepoType {
            return when (type) {
                1 -> MOCK
                2 -> DROPBOX
                3 -> DIRECTORY
                4 -> DOCUMENT
                5 -> WEBDAV
                6 -> GIT

                else -> throw IllegalArgumentException("Unknown repo type id $type")
            }
        }
    }
}