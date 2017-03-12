package com.orgzly.android.ui;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Broadcasts;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;

/**
 * Ash nazg durbatulûk, ash nazg gimbatul,
 * ash nazg thrakatulûk agh burzum-ishi krimpatul.
 *
 * ("Extended by all activities.")
 */
public class CommonActivity extends AppCompatActivity {
    private static final String TAG = CommonActivity.class.getName();

    protected Snackbar snackbar;

    private void dismissSnackbar() {
        if (snackbar != null) {
            snackbar.dismiss();
            snackbar = null;
        }
    }

    public void showSimpleSnackbarLong(int resId) {
        showSimpleSnackbarLong(getString(resId));
    }

    public void showSimpleSnackbarLong(String message) {
        View view = findViewById(R.id.main_content);
        if (view != null) {
            showSnackbar(Snackbar.make(view, message, Snackbar.LENGTH_LONG));
        }
    }

    public void showSnackbar(Snackbar s) {
        dismissSnackbar();

        /* Close drawer before displaying snackbar. */
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        snackbar = s;

        /* Set background color from attribute. */
        int bgColor = getSnackbarBackgroundColor();
        snackbar.getView().setBackgroundColor(bgColor);

        snackbar.show();
    }

    private int getSnackbarBackgroundColor() {
        TypedArray arr = obtainStyledAttributes(R.styleable.ColorScheme);
        int color = arr.getColor(R.styleable.ColorScheme_snackbar_bg_color, 0);
        arr.recycle();
        return color;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean consumed = super.dispatchTouchEvent(ev);

        if (ev.getAction() == MotionEvent.ACTION_UP) {
            dismissSnackbar();
        }

        return consumed;
    }

    /**
     * Set theme and styles.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        setupTheme();

        setupLayoutDirection();

        super.onCreate(savedInstanceState);

    }

    private void setupLayoutDirection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            String setting = AppPreferences.layoutDirection(this);

            int layoutDirection = View.LAYOUT_DIRECTION_LOCALE;

            if (getString(R.string.pref_value_layout_direction_ltr).equals(setting)) {
                layoutDirection = View.LAYOUT_DIRECTION_LTR;

            } else if (getString(R.string.pref_value_layout_direction_rtl).equals(setting)) {
                layoutDirection = View.LAYOUT_DIRECTION_RTL;
            }

            getWindow().getDecorView().setLayoutDirection(layoutDirection);
        }
    }

    private void setupTheme() {
        /*
         * Set theme - color scheme.
         */
        String colorScheme = AppPreferences.colorScheme(this);

        if (getString(R.string.pref_value_color_scheme_dark).equals(colorScheme)) {
            setTheme(R.style.AppDarkTheme_Dark);

        } else if (getString(R.string.pref_value_color_scheme_black).equals(colorScheme)) {
            setTheme(R.style.AppDarkTheme_Black);

        } else {
            setTheme(R.style.AppLightTheme_Light);
        }

        /*
         * Apply font style based on preferences.
         */
        String fontSizePref = AppPreferences.fontSize(this);

        if (getString(R.string.pref_value_font_size_large).equals(fontSizePref)) {
            getTheme().applyStyle(R.style.FontSize_Large, true);
        } else if (getString(R.string.pref_value_font_size_small).equals(fontSizePref)) {
            getTheme().applyStyle(R.style.FontSize_Small, true);
        }
    }

    protected Runnable actionAfterPermissionGrant;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case AppPermissions.FOR_BOOK_EXPORT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (actionAfterPermissionGrant != null) {
                        actionAfterPermissionGrant.run();
                        actionAfterPermissionGrant = null;
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "");
    }

    public void popBackStackAndCloseKeyboard() {
        getSupportFragmentManager().popBackStack();
        ActivityUtils.closeSoftKeyboard(this);
    }
}
