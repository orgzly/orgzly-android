package com.orgzly.android.espresso

import android.os.Environment
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.EspressoUtils.*
import com.orgzly.android.ui.MainActivity
import org.hamcrest.Matchers.startsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExternalLinksTest : OrgzlyTest() {
    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(MainActivity::class.java, true, false)

    private lateinit var outsideConfiguredRootLink: String

    private lateinit var directoryLink: String

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        outsideConfiguredRootLink = "file:${context.cacheDir.absolutePath}"

        directoryLink = "file:${Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).absolutePath}"

        shelfTestUtils.setupBook(
                "book-a",
                """
                    * Note [a-1]
                    $outsideConfiguredRootLink

                    * Note [a-2]
                    $directoryLink
                """.trimIndent()
        )

        activityRule.launchActivity(null)

        // Open book
        onListItem(0).perform(click())
    }

    @Test
    fun testOutsideConfiguredRoot() {
        onListItem(0).onChildView(withId(R.id.item_head_content))
                .perform(clickClickableSpan(outsideConfiguredRootLink))

        onSnackbar().check(matches(withText(startsWith(
                "Failed to open file: Failed to find configured root"))))
    }

    @Test
    fun testDirectory() {
        onListItem(1).onChildView(withId(R.id.item_head_content))
                .perform(clickClickableSpan(directoryLink))

        onSnackbar().check(matches(withText("No application found to open this file")))
    }
}
