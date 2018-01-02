package com.orgzly.android.query

import org.intellij.lang.annotations.Language
import java.util.*
import java.util.regex.Matcher

abstract class QueryParser {
    data class ConditionMatch(
            @Language("RegExp") val regex: String,
            val rule: (matcher: Matcher) -> Condition?)

    data class SortOrderMatch(
            @Language("RegExp") val regex: String,
            val rule: (matcher: Matcher) -> SortOrder?)

    data class OptionMatch(
            @Language("RegExp") val regex: String,
            val rule: (matcher: Matcher, options: Options) -> Options?)

    protected abstract val groupOpen: String
    protected abstract val groupClose: String

    protected abstract val operatorsAnd: List<String>
    protected abstract val operatorsOr: List<String>

    protected abstract val conditions: List<ConditionMatch>
    protected abstract val sortOrders: List<SortOrderMatch>
    protected abstract val supportedOptions: List<OptionMatch>

    private val orders: MutableList<SortOrder> = mutableListOf()
    private var options: Options = Options()

    private lateinit var tokenizer: QueryTokenizer


    fun parse(str: String): Query {
        orders.clear()

        tokenizer = QueryTokenizer(str, groupOpen, groupClose)

        return Query(parseExpression(), orders, options)
    }

    private fun parseExpression(vararg initialExpr: Condition): Condition {
        var members = ArrayList<Condition>()
        var operator = Operator.AND // Default
        var lastTokenWasCondition = false

        fun addCondition(condition: Condition) {
            if (lastTokenWasCondition && operator === Operator.OR) { // OR then implicit AND
                val exp = members.removeAt(members.size - 1)
                members.add(parseExpression(exp, condition))
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
                    members.add(parseExpression())
                    lastTokenWasCondition = false
                }

                groupClose -> {
                    break@tokens
                }

                in operatorsAnd -> {
                    if (members.size > 0) {
                        if (operator === Operator.OR) {
                            val expr = members.removeAt(members.size - 1)
                            members.add(parseExpression(expr))
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
                            val e = def.rule(matcher)
                            if (e != null) {
                                addCondition(e)
                                continue@tokens
                            }
                        }
                    }

                    // Check if token is a sort order.
                    for (def in sortOrders) {
                        val matcher = def.regex.toPattern().matcher(token)
                        if (matcher.find()) {
                            val e = def.rule(matcher)
                            if (e != null) {
                                orders.add(e)
                                continue@tokens
                            }
                        }
                    }

                    // Check if token is an instruction.
                    for (def in supportedOptions) {
                        val matcher = def.regex.toPattern().matcher(token)
                        if (matcher.find()) {
                            val e = def.rule(matcher, options)
                            if (e != null) {
                                options = e
                                continue@tokens
                            }
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

    protected fun unQuote(token: String): String = QuotedStringTokenizer.unquote(token)

    /**
     * AND has precedence over OR
     */
    enum class Operator { AND, OR }
}