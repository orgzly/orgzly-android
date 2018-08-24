package com.orgzly.android.espresso

import android.app.Activity
import androidx.test.rule.ActivityTestRule

class EspressoActivityTestRule<T : Activity>(
        activityClass: Class<T>,
        initialTouchMode: Boolean,
        launchActivity: Boolean
) : ActivityTestRule<T>(activityClass, initialTouchMode, launchActivity)