package com.orgzly.android.espresso

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.EspressoUtils.*
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.settings.SettingsActivity
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.io.File

@Ignore("Not a test")
class ScreenshotsTakingNotATest : OrgzlyTest() {

    companion object {
        private const val SCREENSHOTS_DIRECTORY = "/sdcard/Download/screenshots"

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            File(SCREENSHOTS_DIRECTORY).run {
                deleteRecursively()
                mkdirs()
            }
        }
    }

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        dataRepository.importGettingStartedBook()
    }

    @Test
    fun main() {
        ActivityScenario.launch(MainActivity::class.java)

        takeScreenshot("books.png")

        onView(withId(R.id.drawer_layout)).perform(open())

        takeScreenshot("navigation-drawer.png")

        onView(allOf(isDescendantOfA(withId(R.id.drawer_navigation_view)), withText(R.string.getting_started_notebook_name)))
            .perform(click())

        // Open quick-menu
        // Not working
        // onNoteInBook(4).perform(swipeRight())

        takeScreenshot("book.png")

        onNoteInBook(11).perform(click())

        takeScreenshot("note.png")

        onView(withId(R.id.drawer_layout)).perform(open())
        onView(withText(R.string.searches)).perform(click())

        takeScreenshot("saved-searches.png")

        onView(withId(R.id.drawer_layout)).perform(open())

        onView(allOf(isDescendantOfA(withId(R.id.drawer_navigation_view)), withText(R.string.agenda)))
            .perform(click())

        takeScreenshot("agenda.png")
    }

    @Test
    fun mainDark() {
        AppPreferences.colorScheme(context, context.getString(R.string.pref_value_color_scheme_dark));
        ActivityScenario.launch(MainActivity::class.java)

        onBook(0).perform(click())
        onView(withId(R.id.drawer_layout)).perform(open())

        takeScreenshot("dark-navigation-drawer.png")
    }

    @Test
    fun settings() {
        ActivityScenario.launch(SettingsActivity::class.java)

        clickSetting("", R.string.sync)
        clickSetting("", R.string.repositories)

        takeScreenshot("repositories.png")
    }

    private fun takeScreenshot(name: String) {
        val screenshotFile = File(SCREENSHOTS_DIRECTORY, name)

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).run {
            if (!takeScreenshot(screenshotFile, 1.0f, 100)) {
                throw Exception("Failed to create screenshot $name")
            }
        }
    }
}
