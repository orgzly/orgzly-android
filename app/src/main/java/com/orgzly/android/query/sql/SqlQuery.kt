package com.orgzly.android.query.sql

data class SqlQuery(val selection: String, val selectionArgs: List<String>, val orderBy: String)