package com.orgzly.android.query.user

import com.orgzly.android.query.*

open class DottedQueryParser : QueryParser() {

    override val groupOpen   = "("
    override val groupClose  = ")"

    override val logicalAnd = listOf("and", "AND")
    override val logicalOr = listOf("or", "OR")

    override val conditions = listOf(
            ConditionMatch("""^(\.)?b\.(.+)""") { match ->
                Condition.InBook(unQuote(match.groupValues[2]), match.groupValues[1].isNotEmpty())
            },

            ConditionMatch("""^(\.)?i\.(.+)""") { match ->
                Condition.HasState(unQuote(match.groupValues[2]), match.groupValues[1].isNotEmpty())
            },

            ConditionMatch("""^(\.)?it\.(todo|done|none)""") { match ->
                val stateType = StateType.valueOf(match.groupValues[2].toUpperCase())
                Condition.HasStateType(stateType, match.groupValues[1].isNotEmpty())
            },

            ConditionMatch("""^(\.)?p\.([a-zA-Z])""") { match ->
                Condition.HasPriority(match.groupValues[2], match.groupValues[1].isNotEmpty())
            },

            ConditionMatch("""^(\.)?ps\.([a-zA-Z])""") { match ->
                Condition.HasSetPriority(match.groupValues[2], match.groupValues[1].isNotEmpty())
            },

            ConditionMatch("""^(\.)?t\.(.+)""") { match ->
                Condition.HasTag(unQuote(match.groupValues[2]), match.groupValues[1].isNotEmpty())
            },

            ConditionMatch("""^(\.)?tn\.(.+)""") { match ->
                Condition.HasOwnTag(unQuote(match.groupValues[2]), match.groupValues[1].isNotEmpty())
            },

            ConditionMatch("""^(e|s|d|c|cr)(?:\.(eq|ne|lt|le|gt|ge))?\.(.+)""") { match ->
                val timeTypeMatch = match.groupValues[1]
                val relationMatch = match.groupValues[2]
                val intervalMatch = match.groupValues[3]

                val relation = when (relationMatch) {
                    "eq" -> Relation.EQ
                    "ne" -> Relation.NE
                    "lt" -> Relation.LT
                    "le" -> Relation.LE
                    "gt" -> Relation.GT
                    "ge" -> Relation.GE
                    else ->
                        // Default if there is no relation
                        when (timeTypeMatch) {
                            "c", "e" -> Relation.EQ
                            else -> Relation.LE
                        }
                }

                val interval = QueryInterval.parse(unQuote(intervalMatch))

                if (interval != null) {
                    when (timeTypeMatch) {
                        "e"  -> Condition.Event(interval, relation)
                        "d"  -> Condition.Deadline(interval, relation)
                        "c"  -> Condition.Closed(interval, relation)
                        "cr" -> Condition.Created(interval, relation)
                        else -> Condition.Scheduled(interval, relation)
                    }
                } else {
                    null // Ignore this match
                }
            }
    )

    override val sortOrders = listOf(
            SortOrderMatch("""^(\.)?o\.(?:notebook|book|b)$""") { match ->
                SortOrder.Book(match.groupValues[1].isNotEmpty())
            },
            SortOrderMatch("""^(\.)?o\.(?:title|t)$""") { match ->
                SortOrder.Title(match.groupValues[1].isNotEmpty())
            },
            SortOrderMatch("""^(\.)?o\.(?:scheduled|sched|s)$""") { match ->
                SortOrder.Scheduled(match.groupValues[1].isNotEmpty())
            },
            SortOrderMatch("""^(\.)?o\.(?:deadline|dead|d)$""") { match ->
                SortOrder.Deadline(match.groupValues[1].isNotEmpty())
            },
            SortOrderMatch("""^(\.)?o\.(?:event|e)$""") { match ->
                SortOrder.Event(match.groupValues[1].isNotEmpty())
            },
            SortOrderMatch("""^(\.)?o\.(?:closed|close|c)$""") { match ->
                SortOrder.Closed(match.groupValues[1].isNotEmpty())
            },
            SortOrderMatch("""^(\.)?o\.(?:created|cr)$""") { match ->
                SortOrder.Created(match.groupValues[1].isNotEmpty())
            },
            SortOrderMatch("""^(\.)?o\.(?:priority|prio|pri|p)$""") { match ->
                SortOrder.Priority(match.groupValues[1].isNotEmpty())
            },
            SortOrderMatch("""^(\.)?o\.(?:state|st)$""") { match ->
                SortOrder.State(match.groupValues[1].isNotEmpty())
            },
            SortOrderMatch("""^(\.)?o\.(?:position|pos)$""") { match ->
                SortOrder.Position(match.groupValues[1].isNotEmpty())
            }
    )

    override val supportedOptions = listOf(
            OptionMatch("""^ad\.(\d+)$""") { match, options ->
                val days = match.groupValues[1].toInt()
                if (days > 0) options.copy(agendaDays = days) else null
            }
    )
}
