package com.orgzly.android.query

import org.intellij.lang.annotations.Language
import java.util.*
import java.util.regex.Matcher

abstract class QueryParser {
    data class ConditionMatch(@Language("RegExp") val regex: String, val rule: (matcher: Matcher) -> Condition?)
    data class SortOrderMatch(@Language("RegExp") val regex: String, val rule: (matcher: Matcher) -> SortOrder?)

    abstract val groupOpen: String
    abstract val groupClose: String

    abstract val operatorsAnd: List<String>
    abstract val operatorsOr: List<String>

    abstract val conditions: List<ConditionMatch>
    abstract val sortOrders: List<SortOrderMatch>


    private val orders: MutableList<SortOrder> = mutableListOf()

    private lateinit var tokenizer: QueryTokenizer


    fun parse(str: String): Query {
        orders.clear()

        tokenizer = QueryTokenizer(str, groupOpen, groupClose)

        return Query(conditions(), orders)
    }

    private fun conditions(vararg initialExpr: Condition): Condition {
        var members = ArrayList<Condition>()
        var operator = Operator.AND // Default
        var lastTokenWasCondition = false

        fun addCondition(condition: Condition) {
            if (lastTokenWasCondition && operator === Operator.OR) { // OR then implicit AND
                val exp = members.removeAt(members.size - 1)
                members.add(conditions(exp, condition))
            } else {
                members.add(condition)
            }

            lastTokenWasCondition = true
        }

        if (initialExpr.isNotEmpty()) {
            members.addAll(Arrays.asList(*initialExpr))
        }

        tokens@ while (tokenizer.hasMoreTokens()) {
            val token = tokenizer.nextToken()

            // System.out.println("Next token: $token, Current members: $members")

            when (token) {
                groupOpen -> {
                    members.add(conditions())
                    lastTokenWasCondition = false
                }

                groupClose -> {
                    break@tokens
                }

                in operatorsAnd -> {
                    if (members.size > 0) {
                        if (operator === Operator.OR) {
                            val expr = members.removeAt(members.size - 1)
                            members.add(conditions(expr))
                        }
                        lastTokenWasCondition = false

                    } // else: Operator with no expression before it
                }

                in operatorsOr -> {
                    if (members.size > 0) {
                        if (operator === Operator.AND) {
                            if (members.size > 1) { // exp exp OR ... -> Or(And(exp exp), ...)
                                val prev = Condition.And(members)
                                members = ArrayList()
                                members.add(prev)
                            }
                        }
                        operator = Operator.OR
                        lastTokenWasCondition = false

                    } // else: Operator with no expression before it
                }

                else -> {
                    // Check if token is a condition.
                    for (def in conditions) {
                        val matcher = def.regex.toPattern().matcher(token)
                        if (matcher.find()) {
                            def.rule(matcher)?.let {
                                addCondition(it)
                            }
                            continue@tokens
                        }
                    }

                    // Check if token is a sort order.
                    for (def in sortOrders) {
                        val matcher = def.regex.toPattern().matcher(token)
                        if (matcher.find()) {
                            def.rule(matcher)?.let {
                                addOrder(it)
                            }
                            continue@tokens
                        }
                    }

                    // If nothing matches use token as plain text.
                    addCondition(Condition.HasText(unQuote(token)))
                }
            }
        }

        return when (operator) {
            Operator.AND -> Condition.And(members)
            Operator.OR -> Condition.Or(members)
        }
    }

    private fun addOrder(order: SortOrder) = orders.add(order)

    protected fun unQuote(token: String): String = QuotedStringTokenizer.unquote(token)

    /**
     * AND has precedence over OR
     *
     * Reasoning (examples)...
     *
     * Languages: && over ||
     * Orgzly: First "or" implementation: AND over OR
     * Orgzly: Search proposal: AND over OR
     * Google: OR over AND
     * Bing: AND over OR
     */
    enum class Operator {
        AND,
        OR
    }
}