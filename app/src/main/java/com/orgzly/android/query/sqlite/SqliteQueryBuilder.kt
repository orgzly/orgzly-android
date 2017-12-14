package com.orgzly.android.query.sqlite

import android.content.Context
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.provider.models.DbNote
import com.orgzly.android.provider.views.DbNoteView
import com.orgzly.android.query.*
import com.orgzly.org.datetime.OrgInterval
import java.util.*


class SqliteQueryBuilder(val context: Context): SqlQueryBuilder {
    private var where: String = ""
    private val arguments: MutableList<String> = ArrayList()

    private var order: String = ""

    private var hasScheduledCondition = false
    private var hasDeadlineCondition = false

    override fun getSelection() = where
    override fun getSelectionArgs(): List<String> = arguments
    override fun getOrderBy(): String = order

    override fun build(query: Query) {
        hasScheduledCondition = false
        hasDeadlineCondition = false

        where = toString(query.condition, true)

        order = buildOrderBy(query.sortOrders)
    }

    private fun buildOrderBy(sortOrders: List<SortOrder>): String {
        val o = ArrayList<String>()

        if (sortOrders.isEmpty()) { // Use default sort order
            o.add(DbNoteView.BOOK_NAME)

            /* Priority or default priority. */
            o.add("COALESCE(" + DbNoteView.PRIORITY + ", '" + AppPreferences.defaultPriority(context) + "')")
            o.add(DbNoteView.PRIORITY + " IS NULL")

            if (hasScheduledCondition) {
                o.add(DbNoteView.SCHEDULED_TIME_TIMESTAMP + " IS NULL")
                o.add(DbNoteView.SCHEDULED_TIME_START_OF_DAY)
                o.add(DbNoteView.SCHEDULED_TIME_HOUR + " IS NULL")
                o.add(DbNoteView.SCHEDULED_TIME_TIMESTAMP)
            }

            if (hasDeadlineCondition) {
                o.add(DbNoteView.DEADLINE_TIME_TIMESTAMP + " IS NULL")
                o.add(DbNoteView.DEADLINE_TIME_START_OF_DAY)
                o.add(DbNoteView.DEADLINE_TIME_HOUR + " IS NULL")
                o.add(DbNoteView.DEADLINE_TIME_TIMESTAMP)
            }

        } else {
            sortOrders.forEach { order ->
                when (order) {
                    is SortOrder.ByBook ->
                        o.add(DbNoteView.BOOK_NAME + if (order.desc) " DESC" else "")

                    is SortOrder.ByScheduled -> {
                        o.add(DbNoteView.SCHEDULED_TIME_TIMESTAMP + " IS NULL")

                        if (order.desc) {
                            o.add(DbNoteView.SCHEDULED_TIME_START_OF_DAY + " DESC")
                            o.add(DbNoteView.SCHEDULED_TIME_HOUR + " IS NOT NULL")
                            o.add(DbNoteView.SCHEDULED_TIME_TIMESTAMP + " DESC")

                        } else {
                            o.add(DbNoteView.SCHEDULED_TIME_START_OF_DAY)
                            o.add(DbNoteView.SCHEDULED_TIME_HOUR + " IS NULL")
                            o.add(DbNoteView.SCHEDULED_TIME_TIMESTAMP)
                        }
                    }

                    is SortOrder.ByDeadline -> {
                        o.add(DbNoteView.DEADLINE_TIME_TIMESTAMP + " IS NULL")

                        if (order.desc) {
                            o.add(DbNoteView.DEADLINE_TIME_START_OF_DAY + " DESC")
                            o.add(DbNoteView.DEADLINE_TIME_HOUR + " IS NOT NULL")
                            o.add(DbNoteView.DEADLINE_TIME_TIMESTAMP + " DESC")

                        } else {
                            o.add(DbNoteView.DEADLINE_TIME_START_OF_DAY)
                            o.add(DbNoteView.DEADLINE_TIME_HOUR + " IS NULL")
                            o.add(DbNoteView.DEADLINE_TIME_TIMESTAMP)
                        }
                    }

                    is SortOrder.ByPriority -> {
                        o.add("COALESCE(" + DbNoteView.PRIORITY + ", '" + AppPreferences.defaultPriority(context) + "')" + if (order.desc) " DESC" else "")
                        o.add(DbNoteView.PRIORITY + if (order.desc) " IS NOT NULL" else " IS NULL")
                    }
                }
            }
        }

        /* Always sort by position last. */
        o.add(DbNoteView.LFT)

        return o.joinToString(", ")
    }

    private fun joinConditions(members: List<Condition>, operator: String, isOuter: Boolean): String =
            if (isOuter) {
                members.joinToString(separator = " $operator ") {
                    toString(it)
                }
            } else {
                members.joinToString(prefix = "(", separator = " $operator ", postfix = ")") {
                    toString(it)
                }
            }

    private fun toString(expr: Condition, isOuter: Boolean = false): String {
        fun not(not: Boolean, selection: String): String = if (not) "NOT($selection)" else selection

        return when (expr) {
            is Condition.InBook -> {
                arguments.add(expr.name)
                not(expr.not, "${DbNoteView.BOOK_NAME} = ?")
            }

            is Condition.HasState -> {
                arguments.add(expr.state.toUpperCase())
                not(expr.not, "COALESCE(${DbNote.STATE}, '') = ?")
            }

            is Condition.HasStateType -> {
                when (expr.type) {
                    StateType.TODO -> {
                        val states = AppPreferences.todoKeywordsSet(context)
                        arguments.addAll(states)
                        not(expr.not, "COALESCE(${DbNote.STATE}, '') IN (" + Collections.nCopies(states.size, "?").joinToString() + ")")
                    }
                    StateType.DONE -> {
                        val states = AppPreferences.doneKeywordsSet(context)
                        arguments.addAll(states)
                        not(expr.not, "COALESCE(${DbNote.STATE}, '') IN (" + Collections.nCopies(states.size, "?").joinToString() + ")")

                    }
                    StateType.NONE -> not(expr.not, "COALESCE(" + DbNote.STATE + ", '') = ''")
                }
            }

            is Condition.HasPriority -> {
                arguments.add(AppPreferences.defaultPriority(context))
                arguments.add(expr.priority)
                not(expr.not, "LOWER(COALESCE(NULLIF(${DbNote.PRIORITY}, ''), ?)) = ?")
            }

            is Condition.HasSetPriority -> {
                arguments.add(expr.priority)
                not(expr.not, "LOWER(COALESCE(${DbNote.PRIORITY}, '')) = ?")
            }

            is Condition.HasTag -> {
                repeat(2) { arguments.add("%${expr.tag}%") }
                not(expr.not, "(COALESCE(${DbNote.TAGS}, '') LIKE ? OR COALESCE(${DbNoteView.INHERITED_TAGS}, '') LIKE ?)")
            }

            is Condition.HasOwnTag -> {
                arguments.add("%${expr.tag}%")
                "${DbNote.TAGS} LIKE ?"
            }

            is Condition.Scheduled -> {
                hasScheduledCondition = true
                toInterval(DbNoteView.SCHEDULED_TIME_TIMESTAMP, expr.interval, expr.relation)
            }

            is Condition.Deadline -> {
                hasDeadlineCondition = true
                toInterval(DbNoteView.DEADLINE_TIME_TIMESTAMP, expr.interval, expr.relation)
            }

            is Condition.HasText -> {
                repeat(3) { arguments.add("%${expr.text}%") }
                "(${DbNote.TITLE} LIKE ? OR ${DbNote.CONTENT} LIKE ? OR ${DbNote.TAGS} LIKE ?)"
            }

            is Condition.Or -> joinConditions(expr.operands, "OR", isOuter)
            is Condition.And -> joinConditions(expr.operands, "AND", isOuter)
        }
    }

    private fun toInterval(column: String, interval: QueryInterval, relation: Relation): String {
        if (interval.none) {
            return "$column IS NULL"
        }

        val field = when (interval.unit) {
            OrgInterval.Unit.HOUR  -> Calendar.HOUR_OF_DAY
            OrgInterval.Unit.DAY   -> Calendar.DAY_OF_MONTH
            OrgInterval.Unit.WEEK  -> Calendar.WEEK_OF_YEAR
            OrgInterval.Unit.MONTH -> Calendar.MONTH
            OrgInterval.Unit.YEAR  -> Calendar.YEAR

            null -> throw IllegalArgumentException("Interval unit not set")
        }

        val timeFromNow = TimeUtils.timeFromNow(field, interval.value)
        val timeFromNowPlusOne = TimeUtils.timeFromNow(field, interval.value + 1)

        val cond = when (relation) {
            Relation.EQ -> "$timeFromNow <= $column AND $column < $timeFromNowPlusOne"
            Relation.NE -> "$column < $timeFromNow AND $timeFromNowPlusOne <= $column"
            Relation.LT -> "$column < $timeFromNow"
            Relation.LE -> "$column < $timeFromNowPlusOne"
            Relation.GT -> "$timeFromNowPlusOne <= $column"
            Relation.GE -> "$timeFromNow <= $column"
        }
        
        return "($column != 0 AND $cond)"
    }
}
