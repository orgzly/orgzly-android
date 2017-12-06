package com.orgzly.android.query

class QueryTokenizer(val str: String, private val groupOpen: String, private val groupClose: String) {
    private var tokenizer = QuotedStringTokenizer(normalizeQuery(str), " ", false, true)

    fun hasMoreTokens(): Boolean = tokenizer.hasMoreTokens()
    fun nextToken(): String = tokenizer.nextToken()

    // Space-separate group open/close characters.
    // FIXME: Support groups in QuotedStringTokenizer and remove this hack
    private fun normalizeQuery(str: String): String =
            getTokens(getTokens(" $str ", groupOpen).joinToString(" $groupOpen "), groupClose).joinToString(" $groupClose ")

    private fun getTokens(str: String, delim: String): List<String> {
        val tokens = mutableListOf<String>()

        val tokenizer = QuotedStringTokenizer(str, delim, false, true)

        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken())
        }

        return tokens.toList()
    }
}