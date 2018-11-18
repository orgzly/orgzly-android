package com.orgzly.android.espresso

import android.os.Environment
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.EspressoUtils.onSnackbar
import com.orgzly.android.ui.MainActivity
import com.orgzly.android.util.MiscUtils
import junit.framework.Assert.fail
import org.hamcrest.Matchers.startsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(value = Parameterized::class)
class ExternalLinksTest(private val param: Parameter) : OrgzlyTest() {

    data class Parameter(val link: String, val check: () -> Any)

    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(MainActivity::class.java, true, false)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): Collection<Parameter> {
            val cacheDir = App.getAppContext().cacheDir
            val storageDir = Environment.getExternalStorageDirectory()
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            File(storageDir, "orgzly-tests").let { dir ->
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        fail("Failed to create $dir")
                    }
                }

                MiscUtils.writeStringToFile("Lorem ipsum", File(dir, "document.txt"))

                javaClass.classLoader.getResourceAsStream("assets/images/logo.png").use { stream ->
                    MiscUtils.writeStreamToFile(stream, File(dir, "logo.png"))
                }
            }

            return listOf(
                    Parameter("file:./non-existing-file") {
                        onSnackbar().check(ViewAssertions.matches(
                                withText("File $storageDir/non-existing-file does not exist")))
                    },

                    Parameter("file:$cacheDir") {
                        onSnackbar().check(matches(withText(startsWith(
                                "Failed to open file: Failed to find configured root"))))
                    },

                    Parameter("file:${downloadsDir.absolutePath}") {
                        onSnackbar().check(matches(withText(startsWith(
                                "No application found to open this file"))))
                    }
            )
        }
    }

    @Test
    fun testLink() {
        shelfTestUtils.setupBook("book", "* Note\n${param.link}")

        activityRule.launchActivity(null)

        // Open book
        EspressoUtils.onListItem(0).perform(ViewActions.click())

        // Click on link
        EspressoUtils.onListItem(0).onChildView(ViewMatchers.withId(R.id.item_head_content))
                .perform(EspressoUtils.clickClickableSpan(param.link))

        param.check()
    }

}