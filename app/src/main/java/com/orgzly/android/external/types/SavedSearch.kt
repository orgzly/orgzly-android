package com.orgzly.android.external.types

data class SavedSearch(val id: Long, val name: String, val position: Int, val query: String) {
    companion object {
        fun from(search: com.orgzly.android.db.entity.SavedSearch) =
                SavedSearch(search.id, search.name, search.position, search.query)
    }
}