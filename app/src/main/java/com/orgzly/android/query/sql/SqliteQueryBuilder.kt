package com.orgzly.android.query.sql

import android.content.Context
import android.database.DatabaseUtils
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.provider.models.DbNote
import com.orgzly.android.provider.views.DbNoteView
import com.orgzly.android.query.*
import com.orgzly.org.datetime.OrgInterval
import java.util.*


class SqliteQueryBuilder(val context: Context) {
    private var where: String = ""
    private val arguments: MutableList<String> = ArrayList()

    private var order: String = ""

    private var hasScheduledCondition = false
    private var hasDeadlineCondition = false
    private var hasCreatedCondition = false

    fun build(query: Query): SqlQuery {
        hasScheduledCondition = false
        hasDeadlineCondition = false
        hasCreatedCondition = false

        where = toString(query.condition)

        order = buildOrderBy(query.sortOrders)

        return SqlQuery(where, arguments, order)
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

            if (hasCreatedCondition) {
                o.add(DbNoteView.CREATED_AT + " DESC")
            }

        } else {
            sortOrders.forEach { order ->
                when (order) {
                    is SortOrder.Book ->
                        o.add(DbNoteView.BOOK_NAME + if (order.desc) " DESC" else "")

                    is SortOrder.Scheduled -> {
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

                    is SortOrder.Deadline -> {
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

                    is SortOrder.Created -> {
                        o.add(DbNoteView.CREATED_AT + " IS NULL")

                        if (order.desc) {
                            o.add(DbNoteView.CREATED_AT + " DESC")

                        } else {
                            o.add(DbNoteView.CREATED_AT)
                        }
                    }

                    is SortOrder.Closed -> {
                        o.add(DbNoteView.CLOSED_TIME_TIMESTAMP + " IS NULL")

                        if (order.desc) {
                            o.add(DbNoteView.CLOSED_TIME_START_OF_DAY + " DESC")
                            o.add(DbNoteView.CLOSED_TIME_HOUR + " IS NOT NULL")
                            o.add(DbNoteView.CLOSED_TIME_TIMESTAMP + " DESC")

                        } else {
                            o.add(DbNoteView.CLOSED_TIME_START_OF_DAY)
                            o.add(DbNoteView.CLOSED_TIME_HOUR + " IS NULL")
                            o.add(DbNoteView.CLOSED_TIME_TIMESTAMP)
                        }
                    }

                    is SortOrder.Priority -> {
                        o.add("COALESCE(" + DbNoteView.PRIORITY + ", '" + AppPreferences.defaultPriority(context) + "')" + if (order.desc) " DESC" else "")
                        o.add(DbNoteView.PRIORITY + if (order.desc) " IS NOT NULL" else " IS NULL")
                    }

                    is SortOrder.State -> {
                        val states = AppPreferences.todoKeywordsSet(context)
                                .union(AppPreferences.doneKeywordsSet(context))

                        if (states.isNotEmpty()) {
                            val statesInOrder = if (order.desc) states.reversed() else states

                            o.add(statesInOrder.foldIndexed("CASE ${DbNoteView.STATE}") { i, str, state ->
                                "$str WHEN ${DatabaseUtils.sqlEscapeString(state)} THEN $i"
                            } + " ELSE ${states.size} END")
                        }
                    }

                    is SortOrder.Position -> {
                        o.add(DbNoteView.LFT + if (order.desc) " DESC" else "")
                    }
                }
            }
        }

        /* Always sort by position last. */
        o.add(DbNoteView.LFT)

        return o.joinToString(", ")
    }

    private fun joinConditions(members: List<Condition>, operator: String): String {
        return members.joinToString(prefix = "(", separator = " $operator ", postfix = ")") {
            toString(it)
        }
    }

    private fun toString(expr: Condition?): String {
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

            is Condition.Created -> {
                hasCreatedCondition = true
                toInterval(DbNoteView.CREATED_AT, expr.interval, expr.relation)
            }

            is Condition.Closed -> {
                toInterval(DbNoteView.CLOSED_TIME_TIMESTAMP, expr.interval, expr.relation)
            }

            is Condition.HasText -> {
                repeat(3) { arguments.add("%${expr.text}%") }
                "(${DbNote.TITLE} LIKE ? OR ${DbNote.CONTENT} LIKE ? OR ${DbNote.TAGS} LIKE ?)"
            }

            is Condition.Or -> joinConditions(expr.operands, "OR")
            is Condition.And -> joinConditions(expr.operands, "AND")

            null -> "" // No conditions
        }
    }

    private fun toInterval(column: String, interval: QueryInterval, relation: Relation): String {
        if (interval.none) {
            return "$column IS NULL"
        }

        val (field, value) = getFieldAndValueFromInterval(interval)

        val timeFromNow = TimeUtils.timeFromNow(field, value)
        val timeFromNowPlusOne = TimeUtils.timeFromNow(field, value + 1)


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

    /*
     * TODO: Clean this up.
     * There's no need to depend on Org-supported units.
     * Remove OrgInterval dependency from QueryInterval.
     */
    private fun getFieldAndValueFromInterval(interval: QueryInterval): Pair<Int, Int> {
        return if (interval.now) {
            Pair(Calendar.MILLISECOND, 0)

        } else {
            val unit = when (interval.unit) {
                OrgInterval.Unit.HOUR -> Calendar.HOUR_OF_DAY
                OrgInterval.Unit.DAY -> Calendar.DAY_OF_MONTH
                OrgInterval.Unit.WEEK -> Calendar.WEEK_OF_YEAR
                OrgInterval.Unit.MONTH -> Calendar.MONTH
                OrgInterval.Unit.YEAR -> Calendar.YEAR

                null -> throw IllegalArgumentException("Interval unit not set")
            }

            val value = interval.value

            Pair(unit, value)
        }
    }
}
