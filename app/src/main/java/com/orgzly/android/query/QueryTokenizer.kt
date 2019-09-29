package com.orgzly.android.query

class QueryTokenizer(val str: String, private val groupOpen: String, private val groupClose: String) {
    val tokens = tokanize(str)

    var nextToken = 0

    fun hasMoreTokens(): Boolean = nextToken < tokens.size

    fun nextToken() = tokens[nextToken++]

    private fun tokanize(str: String): List<String> {
        return tokenRegex.findAll(str).map { it.value }.toList()
    }

    companion object {
        val TAG: String = QueryTokenizer::class.java.name

        private const val char = """[^")(\s]"""
        private const val doubleQuoted = """"[^"\\]*(?:\\.[^"\\]*)*""""
        private const val doubleQuotedWithPrefix = """$char*$doubleQuoted"""
        private const val groupOpener = """\("""
        private const val groupCloser  = """\)"""
        private const val rest = """$char+"""

        private val tokenRegex =
                listOf(doubleQuotedWithPrefix, groupOpener, groupCloser, rest)
                        .joinToString("|").toRegex()


        fun unquote(s: String): String {
            if (s.length < 2) {
                return s
            }

            val first = s[0]
            val last = s[s.length - 1]
            if (first != last || first != '"') {
                return s
            }
            val b = StringBuilder(s.length - 2)
            var quote = false
            for (i in 1 until s.length - 1) {
                val c = s[i]

                if (c == '\\' && !quote) {
                    quote = true
                    continue
                }
                quote = false
                b.append(c)
            }

            return b.toString()
        }

        fun quote(s: String, delim: String): String {
            if (s.isEmpty()) {
                return "\"\""
            }

            for (element in s) {
                if (element == '"' || element == '\\' || delim.indexOf(element) >= 0) {
                    return quoteUnconditionally(s)
                }
            }

            return s
        }

        fun quoteUnconditionally(s: String): String {
            val builder = StringBuilder(s.length + 8)
            builder.append('"')
            for (element in s) {
                if (element == '"') {
                    builder.append('\\')
                }
                builder.append(element)
                continue
            }
            builder.append('"')
            return builder.toString()
        }
    }
}