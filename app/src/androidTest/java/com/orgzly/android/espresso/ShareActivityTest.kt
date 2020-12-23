package com.orgzly.android.espresso

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.EspressoUtils.*
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.share.ShareActivity
import org.hamcrest.Matchers.startsWith
import org.junit.Assert.assertTrue
import org.junit.Test


class ShareActivityTest : OrgzlyTest() {
    private fun startActivityWithIntent(
            action: String? = null,
            type: String? = null,
            extraText: String? = null,
            extraStreamUri: String? = null,
            queryString: String? = null): ActivityScenario<ShareActivity> {

        val intent = Intent(context, ShareActivity::class.java)

        if (action != null) {
            intent.action = action
        }

        if (type != null) {
            intent.type = type
        }

        if (extraText != null) {
            intent.putExtra(Intent.EXTRA_TEXT, extraText)
        }

        if (extraStreamUri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(extraStreamUri))
        }

        if (queryString != null) {
            intent.putExtra(AppIntent.EXTRA_QUERY_STRING, queryString)
        }

        return ActivityScenario.launch(intent)
    }

    @Test
    fun testDefaultBookRemainsSetAfterRotation() {
        val scenario = startActivityWithIntent(
                action = Intent.ACTION_SEND,
                type = "text/plain",
                extraText = "This is some shared text")

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(withId(R.id.fragment_note_location_button))
                .check(matches(withText(context.getString(R.string.default_share_notebook))))

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        onView(withId(R.id.fragment_note_location_button))
                .check(matches(withText(context.getString(R.string.default_share_notebook))))
    }

    @Test
    fun testBookRemainsSetAfterRotation() {
        testUtils.setupBook("book-one", "")
        testUtils.setupBook("book-two", "")
        testUtils.setupBook("book-three", "")

        val scenario = startActivityWithIntent(
                action = Intent.ACTION_SEND,
                type = "text/plain",
                extraText = "This is some shared text")

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(withId(R.id.fragment_note_location_button)).perform(scrollTo(), click())
        onView(withText("book-two")).perform(click())
        onView(withId(R.id.fragment_note_location_button)).check(matches(withText("book-two")))

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        onView(withId(R.id.fragment_note_location_button)).check(matches(withText("book-two")))
    }

    @Test
    fun testDefaultBookName() {
        startActivityWithIntent(
                action = Intent.ACTION_SEND,
                type = "text/plain",
                extraText = "This is some shared text")

        onView(withId(R.id.fragment_note_location_button))
                .check(matches(withText(context.getString(R.string.default_share_notebook))))
    }

    @Test
    fun testTextSimple() {
        startActivityWithIntent(
                action = Intent.ACTION_SEND,
                type = "text/plain",
                extraText = "This is some shared text")

        onView(withId(R.id.done)).perform(click())
    }

    @Test
    fun testSaveAfterRotation() {
        val scenario = startActivityWithIntent(
                action = Intent.ACTION_SEND,
                type = "text/plain",
                extraText = "This is some shared text")

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(withId(R.id.done)).perform(click())
    }

    @Test
    fun testTextEmpty() {
        startActivityWithIntent(action = Intent.ACTION_SEND, type = "text/plain", extraText = "")
        onView(withId(R.id.done)).perform(click())
    }

    @Test
    fun testTextNull() {
        startActivityWithIntent(action = Intent.ACTION_SEND, type = "text/plain")
        onView(withId(R.id.done)).perform(click())
    }

    @Test
    fun testImage() {
        startActivityWithIntent(
                action = Intent.ACTION_SEND,
                type = "image/png",
                extraStreamUri = "content://uri")

        onView(withId(R.id.fragment_note_title)).check(matches(withText("content://uri")))
        onView(withId(R.id.body_edit)).check(matches(withText("Cannot find image using this URI.")))

        onView(withId(R.id.done)).perform(click())
    }

    @Test
    fun testFileCopy() {
        AppPreferences.attachMethod(context, ShareActivity.ATTACH_METHOD_COPY_DIR);
        startActivityWithIntent(
                action = Intent.ACTION_SEND,
                type = "application/pdf",
                extraStreamUri = "content://uri")

        onView(withId(R.id.fragment_note_title)).check(matches(withText("content://uri")))
        onView(withId(R.id.body_edit)).check(matches(withText("content://uri\n\nCannot determine fileName to this content.")))

        onView(withId(R.id.done)).perform(click())
    }

    @Test
    fun testNoMatchingType() {
        startActivityWithIntent(action = Intent.ACTION_SEND, type = "application/octet-stream")

        onView(withId(R.id.fragment_note_title)).check(matches(withText("")))
        onSnackbar().check(matches(withText(context.getString(R.string.share_type_not_supported, "application/octet-stream"))))
    }

    @Test
    fun testNoActionSend() {
        startActivityWithIntent()

        onView(withId(R.id.fragment_note_title)).check(matches(withText("")))
    }

    @Test
    fun testSettingScheduledTimeRemainsSetAfterRotation() {
        val scenario = startActivityWithIntent(
                action = Intent.ACTION_SEND,
                type = "text/plain",
                extraText = "This is some shared text")

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click())
        onView(withText(R.string.set)).perform(click())
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(startsWith(defaultDialogUserDate()))))

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(startsWith(defaultDialogUserDate()))))
    }

    @Test
    fun testNoteInsertedLast() {
        testUtils.setupBook("book-one", "* Note 1\n** Note 2")

        startActivityWithIntent(
                action = Intent.ACTION_SEND,
                type = "text/plain",
                extraText = "Note 3")

        onView(withId(R.id.done)).perform(click())

        val (_, lft, rgt) = dataRepository.getLastNote("Note 1")!!.position
        val (_, lft1, rgt1) = dataRepository.getLastNote("Note 2")!!.position
        val (_, lft2, rgt2) = dataRepository.getLastNote("Note 3")!!.position

        assertTrue(lft < lft1)
        assertTrue(lft1 < rgt1)
        assertTrue(rgt1 < rgt)
        assertTrue(rgt < lft2)
        assertTrue(lft2 < rgt2)
    }

    @Test
    fun testPresetBookFromSearchQuery() {
        testUtils.setupBook("foo", "doesn't matter")

        startActivityWithIntent(
                action = Intent.ACTION_SEND,
                type = "text/plain",
                extraText = "This is some shared text",
                queryString = "b.foo")

        onView(withId(R.id.fragment_note_location_button)).check(matches(withText("foo")))
    }
}
