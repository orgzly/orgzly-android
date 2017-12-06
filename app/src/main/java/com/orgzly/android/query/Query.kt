package com.orgzly.android.query

data class Query @JvmOverloads constructor(
        val condition: Condition,
        val sortOrders: List<SortOrder> = listOf(),
        val instructions: Set<Instruction> = setOf())
