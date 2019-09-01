package com.orgzly.android.espresso

import android.app.Activity
import androidx.test.rule.ActivityTestRule

class EspressoActivityTestRule<T : Activity> @JvmOverloads constructor(
        activityClass: Class<T>,
        initialTouchMode: Boolean = true,
        launchActivity: Boolean = false
) : ActivityTestRule<T>(activityClass, initialTouchMode, launchActivity)