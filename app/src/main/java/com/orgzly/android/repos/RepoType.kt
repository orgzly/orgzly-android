package com.orgzly.android.repos

import java.lang.IllegalArgumentException

enum class RepoType(val id: Int) {
    MOCK(1),
    DROPBOX(2),
    GOOGLE_DRIVE(3),
    DIRECTORY(4),
    DOCUMENT(5),
    WEBDAV(6),
    GIT(7);

    companion object {
        @JvmStatic
        fun fromId(type: Int): RepoType {
            return when (type) {
                1 -> MOCK
                2 -> DROPBOX
                3 -> GOOGLE_DRIVE
                4 -> DIRECTORY
                5 -> DOCUMENT
                6 -> WEBDAV
                7 -> GIT

                else -> throw IllegalArgumentException("Unknown repo type id $type")
            }
        }
    }
}
