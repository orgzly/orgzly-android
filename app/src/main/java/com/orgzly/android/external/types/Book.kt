package com.orgzly.android.external.types

import com.orgzly.android.db.entity.BookView

data class Book(val id: Long, val title: String) {
    companion object {
        fun from(view: BookView) =
                Book(view.book.id, view.book.name)
    }
}