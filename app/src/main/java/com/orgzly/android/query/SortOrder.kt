package com.orgzly.android.query

sealed class SortOrder {
    abstract val desc: Boolean

    data class ByBook(override val desc: Boolean = false) : SortOrder()
    data class ByScheduled(override val desc: Boolean = false) : SortOrder()
    data class ByDeadline(override val desc: Boolean = false) : SortOrder()
    data class ByPriority(override val desc: Boolean = false) : SortOrder()
}