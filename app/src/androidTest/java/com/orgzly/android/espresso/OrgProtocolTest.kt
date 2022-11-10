package com.orgzly.android.espresso

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.util.EspressoUtils.onSnackbar
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test

class OrgProtocolTest : OrgzlyTest() {
    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        testUtils.setupBook(
            "book-a",
            """
                * Note [b-1]
                :PROPERTIES:
                :CUSTOM_ID: DIFFERENT case CUSTOM id
                :END:
                * Note [b-2]
                :PROPERTIES:
                :END:
                * Note [b-3]
                :PROPERTIES:
                :CUSTOM_ID: Link to note in a different book
                :END:

                """.trimIndent()
        )

        testUtils.setupBook(
            "book-b",
            """
                * Note [b-4]
                :PROPERTIES:
                :ID: BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F
                :END:

                """.trimIndent()
        )
    }

    @Test
    fun testOrgProtocolOpensNote() {
        val url = "org-protocol://org-id-goto?id=BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F"

        launchActivity(url)

        onView(allOf(withId(R.id.breadcrumbs_text), isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText("book-b")))

        onView(allOf(withId(R.id.title_view), isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText("Note [b-4]")))
    }

    @Test
    fun testOrgProtocolBadLink1() {
        val url = "org-protocol://org-id-goto://BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F"

        launchActivity(url)

        onSnackbar().check(ViewAssertions.matches(ViewMatchers.withText("Missing “id” param in $url")))
    }

    @Test
    fun testOrgProtocolBadLink2() {
        val url = "org-protocol://some-other-protocol?x=1&y=2"

        launchActivity(url)

        onSnackbar().check(ViewAssertions.matches(ViewMatchers.withText("Unsupported handler in $url")))
    }

    private fun launchActivity(uriString: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(uriString)
        }

        ActivityScenario.launch<Activity>(intent)
    }
}