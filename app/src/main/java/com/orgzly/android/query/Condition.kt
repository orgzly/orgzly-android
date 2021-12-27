package com.orgzly.android.query

/**
 * Consider "Not" instead of per-condition flag.
 * Parsing dotted queries could be tricky (".i.done") though.
 */
sealed class Condition {
    data class InBook @JvmOverloads constructor(val name: String, val not: Boolean = false) : Condition()

    data class HasState(val state: String, val not: Boolean = false) : Condition()
    data class HasStateType(val type: StateType, val not: Boolean = false) : Condition()

    data class HasPriority(val priority: String, val not: Boolean = false) : Condition()
    data class HasSetPriority(val priority: String, val not: Boolean = false) : Condition()

    data class HasTag(val tag: String, val not: Boolean = false) : Condition()
    data class HasOwnTag(val tag: String, val not: Boolean = false) : Condition()

    data class Event(val interval: QueryInterval, val relation: Relation) : Condition()
    data class Scheduled(val interval: QueryInterval, val relation: Relation) : Condition()
    data class Deadline(val interval: QueryInterval, val relation: Relation) : Condition()
    data class Closed(val interval: QueryInterval, val relation: Relation) : Condition()
    data class Created(val interval: QueryInterval, val relation: Relation) : Condition()

    data class HasText(val text: String, val isQuoted: Boolean) : Condition()

    data class And(val operands: List<Condition>) : Condition()
    data class Or(val operands: List<Condition>) : Condition()
}
