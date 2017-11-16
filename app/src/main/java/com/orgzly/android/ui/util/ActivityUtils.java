package com.orgzly.android.ui.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.ui.MainActivity;
import com.orgzly.android.ui.fragments.BookPrefaceFragment;
import com.orgzly.android.ui.fragments.BookFragment;
import com.orgzly.android.ui.fragments.BooksFragment;
import com.orgzly.android.ui.fragments.FilterFragment;
import com.orgzly.android.ui.fragments.FiltersFragment;
import com.orgzly.android.ui.fragments.NoteFragment;
import com.orgzly.android.ui.fragments.QueryFragment;
import com.orgzly.android.ui.fragments.ReposFragment;
import com.orgzly.android.ui.fragments.SettingsFragment;
import com.orgzly.android.util.LogUtils;

/**
 *
 */
public class ActivityUtils {
    private static final String TAG = ActivityUtils.class.getName();

    public static void closeSoftKeyboard(final Activity activity) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Hiding keyboard in activity " + activity);

        View view = activity.getCurrentFocus();

        /* If no view currently has focus, create a new one to grab a window token from it. */
        if (view == null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "No view in focus, using activity");
            view = new View(activity);
        }

        final View finalView = view;

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(finalView.getWindowToken(), 0);
//            }
//        }, 100);
    }

    public static void openSoftKeyboard(final Activity activity, final View view) {
        if (view.requestFocus()) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Showing keyboard for view " + view + " in activity " + activity);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 100);

        } else {
            Log.w(TAG, "Can't open keyboard because view " + view +
                       " failed to get focus in activity " + activity);
        }
    }

    /**
     * Color the action bar depending on which fragment is displayed.
     */
    public static void setColorsForFragment(final AppCompatActivity activity, String fragmentTag) {
        FragmentResources resources = new FragmentResources(activity, fragmentTag);

        /* Color status bar. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(resources.statusColor);
            // getWindow().setNavigationBarColor(color);
        }

        /* Color action bar. */
        activity.getSupportActionBar()
                .setBackgroundDrawable(new ColorDrawable(resources.actionColor));
    }

    public static class FragmentResources {
        public int statusColor;
        public int actionColor;
        public Drawable fabDrawable;

        public FragmentResources(Context context, String fragmentTag) {
            int statusBarAttr;
            int actionBarAttr;
            int fabAttr;

            if (SettingsFragment.FRAGMENT_TAG.equals(fragmentTag)) {
                statusBarAttr = R.attr.status_bar_in_settings;
                actionBarAttr = R.attr.action_bar_in_settings;
                fabAttr = 0;

            } else if (ReposFragment.FRAGMENT_TAG.equals(fragmentTag)) {
                statusBarAttr = R.attr.status_bar_in_settings;
                actionBarAttr = R.attr.action_bar_in_settings;
                fabAttr = 0;

            } else if (FiltersFragment.FRAGMENT_TAG.equals(fragmentTag)) {
                statusBarAttr = R.attr.status_bar_in_query;
                actionBarAttr = R.attr.action_bar_in_query;
                fabAttr = R.attr.oic_new_item;

            } else if (FilterFragment.FRAGMENT_TAG.equals(fragmentTag)) {
                statusBarAttr = R.attr.status_bar_in_query;
                actionBarAttr = R.attr.action_bar_in_query;
                fabAttr = 0;

            } else if (QueryFragment.FRAGMENT_TAG.equals(fragmentTag)) {
                statusBarAttr = R.attr.status_bar_in_query;
                actionBarAttr = R.attr.action_bar_in_query;
                fabAttr = 0;

            } else if (BooksFragment.FRAGMENT_TAG.equals(fragmentTag)) {
                statusBarAttr = R.attr.status_bar_default;
                actionBarAttr = R.attr.action_bar_default;
                fabAttr = R.attr.oic_new_item;

            } else if (BookFragment.FRAGMENT_TAG.equals(fragmentTag)) {
                statusBarAttr = R.attr.status_bar_default;
                actionBarAttr = R.attr.action_bar_default;
                fabAttr = R.attr.oic_new_item;

            } else if (BookPrefaceFragment.FRAGMENT_TAG.equals(fragmentTag)) {
                statusBarAttr = R.attr.status_bar_default;
                actionBarAttr = R.attr.action_bar_default;
                fabAttr = 0;

            } else if (NoteFragment.FRAGMENT_TAG.equals(fragmentTag)) {
                statusBarAttr = R.attr.status_bar_default;
                actionBarAttr = R.attr.action_bar_default;
                fabAttr = 0;

            } else {
                statusBarAttr = R.attr.status_bar_default;
                actionBarAttr = R.attr.action_bar_default;
                fabAttr = 0;
            }

            TypedArray typedArray = context.obtainStyledAttributes(new int[] {
                    statusBarAttr,
                    actionBarAttr,
                    fabAttr
            });

            statusColor = typedArray.getColor(0, 0);
            actionColor = typedArray.getColor(1, 0);
            fabDrawable = typedArray.getDrawable(2);

            typedArray.recycle();
        }
    }

    /**
     * Open "App info" settings, where permissions can be granted.
     */
    public static void openAppInfoSettings(Activity activity) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);

        intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));

        activity.startActivity(intent);
    }

    public static PendingIntent mainActivityPendingIntent(Context context, long bookId, long noteId) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId, noteId);
        Intent intent = Intent.makeRestartActivityTask(new ComponentName(context, MainActivity.class));

        intent.putExtra(MainActivity.EXTRA_BOOK_ID, bookId);
        intent.putExtra(MainActivity.EXTRA_NOTE_ID, noteId);

        return PendingIntent.getActivity(
                context,
                Long.valueOf(noteId).intValue(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
