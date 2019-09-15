package com.orgzly.android.query

sealed class SortOrder {
    abstract val desc: Boolean

    data class Book(override val desc: Boolean = false) : SortOrder()
    data class Title(override val desc: Boolean = false) : SortOrder()
    data class Scheduled(override val desc: Boolean = false) : SortOrder()
    data class Deadline(override val desc: Boolean = false) : SortOrder()
    data class Event(override val desc: Boolean = false) : SortOrder()
    data class Closed(override val desc: Boolean = false) : SortOrder()
    data class Created(override val desc: Boolean = false) : SortOrder()
    data class Priority(override val desc: Boolean = false) : SortOrder()
    data class State(override val desc: Boolean = false) : SortOrder()
    data class Position(override val desc: Boolean = false) : SortOrder()
}