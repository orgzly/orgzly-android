package com.orgzly.android.query

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
class QueryTokenizerTest(private val param: Parameter) {

    data class Parameter(val query: String, val tokens: List<String>)

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): Collection<Parameter> {
            return listOf(
                    Parameter("(a)", listOf("(", "a", ")")),
                    Parameter("((a))", listOf("(", "(", "a", ")", ")")),
                    Parameter("((a) b)", listOf("(", "(", "a", ")", "b", ")")),
                    Parameter("""((a) "c \"  d")""", listOf("(", "(", "a", ")", """"c \"  d"""", ")")),
                    Parameter("""((a."b c"))""", listOf("(", "(", """a."b c"""", ")", ")"))
            )
        }
    }

    @Test
    fun normalizeQuery() {
        val tokenizer = QueryTokenizer(param.query, "(", ")")
        assertThat(tokenizer.tokens, `is`(param.tokens))
    }
}