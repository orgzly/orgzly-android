package com.orgzly.android.query.dotted

import android.content.Context
import com.orgzly.android.query.*
import com.orgzly.android.query.QuotedStringTokenizer

open class DottedQueryBuilder(val context: Context) : UserQueryBuilder {
    private var string: String = ""

    override fun getString(): String = string

    override fun build(query: Query) {
        string = toString(query.condition, true)

        if (query.sortOrders.isNotEmpty()) {
            string += " " + toString(query.sortOrders)
        }
    }

    private fun dot(order: SortOrder) = if (order.desc) "." else ""

    private fun toString(orders: List<SortOrder>): String =
            orders.joinToString(" ") { order ->
                when (order) {
                    is SortOrder.ByBook      -> dot(order) + "o.b"
                    is SortOrder.ByScheduled -> dot(order) + "o.s"
                    is SortOrder.ByDeadline  -> dot(order) + "o.d"
                    is SortOrder.ByPriority  -> dot(order) + "o.p"
                }
            }

    private fun toString(expr: Condition, isOuter: Boolean = false): String {
        fun dot(not: Boolean): String = if (not) "." else ""

        return when (expr) {
            is Condition.InBook -> "${dot(expr.not)}b.${quote(expr.name)}"

            is Condition.HasState -> "${dot(expr.not)}i.${expr.state}"

            is Condition.HasStateType -> {
                when (expr.type) {
                    StateType.DONE -> "${dot(expr.not)}it.done"
                    StateType.TODO -> "${dot(expr.not)}it.todo"
                    StateType.NONE -> "${dot(expr.not)}it.none"
                }
            }

            is Condition.HasPriority -> "${dot(expr.not)}p.${expr.priority}"
            is Condition.HasSetPriority -> "${dot(expr.not)}ps.${expr.priority}"

            is Condition.HasTag -> "${dot(expr.not)}t.${expr.tag}"
            is Condition.HasOwnTag -> "tn.${expr.tag}"

            is Condition.ScheduledInInterval -> "s.${expr.interval}"
            is Condition.DeadlineInInterval -> "d.${expr.interval}"

            is Condition.HasText -> expr.text

            is Condition.Or ->
                expr.operands.joinToString(prefix = if (isOuter) "" else "(", separator = " or ", postfix = if (isOuter) "" else ")") {
                    toString(it)
                }

            is Condition.And ->
                expr.operands.joinToString(separator = " ") {
                    toString(it)
                }
        }
    }

    private fun quote(s: String) = QuotedStringTokenizer.quote(s, " ")
}