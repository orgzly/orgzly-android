package com.orgzly.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import com.orgzly.R;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.prefs.AppPreferencesValues;
import com.orgzly.android.provider.clients.DbClient;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.repos.RepoFactory;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.datetime.OrgDateTime;
import org.junit.After;
import org.junit.Before;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

/**
 * Sets up the environment for tests, such as shelf, preferences and contexts.
 *
 * Inherited by all tests.
 */
public class OrgzlyTest {
    protected Context context;
    protected Shelf shelf;
    protected ShelfTestUtils shelfTestUtils;

    private AppPreferencesValues prefValues;

    private UserTimeFormatter userTimeFormatter;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();

        shelf = new Shelf(context);
        shelfTestUtils = new ShelfTestUtils(context, shelf);

         userTimeFormatter = new UserTimeFormatter(context);

        // new LocalFileStorage(context).cleanup();

        /* Request content provider to close the current database
         * and open a new one with a different name.
         */
        DbClient.toTest(context);

        /* Recreate all tables. */
        DbClient.recreateTables(context);

        /*
         * Using Handler due to:
         *   android.view.InflateException: Binary XML file line #26: Error inflating class java.lang.reflect.Constructor
         *   at android.preference.GenericInflater.createItem(GenericInflater.java:397)
         * on HTC One V API 15.
         */
//        new Handler(context.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
                setupPreferences();
//            }
//        });
    }

    @After
    public void tearDown() throws Exception {
//        new Handler(InstrumentationRegistry.getTargetContext().getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
                restorePreferences();
//            }
//        });
    }

    private void setupPreferences() {
        /* Save preferences' values so they can be restored later. */
        prefValues = AppPreferences.getAllValues(context);

        /* Set all preferences to their default values. */
        AppPreferences.setToDefaults(context);

        /* Modify preferences for tests. */
        setPreferencesForTests();
    }

    /**
     * Change some preferences for tests.
     */
    private void setPreferencesForTests() {
        /* Last used version. */
        try {
            int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            AppPreferences.lastUsedVersionCode(context, versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        /* Manual notebook already loaded. */
        AppPreferences.isGettingStartedNotebookLoaded(context, true);

        /* Click to open notes. */
        AppPreferences.isReverseNoteClickAction(context, false);

        /* Display *all* notebook info. */
        AppPreferences.displayedBookDetails(context,
                Arrays.asList(context.getResources().getStringArray(R.array.displayed_book_details_values)));

        /* Display first few lines of preface. */
        AppPreferences.prefaceDisplay(context, context.getString(R.string.pref_value_preface_in_book_few_lines));

        /* Display inherited tags in search results. */
        AppPreferences.inheritedTagsInSearchResults(context, true);
    }

    /**
     * Restore preferences.
     */
    private void restorePreferences() {
        AppPreferences.setAllFromValues(context, prefValues);
    }

    protected static Repo randomDropboxRepo(Context context) {
        String uuid = UUID.randomUUID().toString();
        return RepoFactory.getFromUri(context, "dropbox:/orgzly/tests/" + uuid);
    }

    /**
     * Local-dependent date and time strings displayed to user.
     */
    protected String userDateTime(String s) {
        return userTimeFormatter.formatAll(OrgDateTime.parse(s));
    }
    protected String userDate() {
        return userTimeFormatter.formatDate(new OrgDateTime(true));
    }

    protected int getActivityResultCode(Activity activity) throws NoSuchFieldException, IllegalAccessException {
        // see http://stackoverflow.com/a/33805663
        Field f = Activity.class.getDeclaredField("mResultCode");
        f.setAccessible(true);
        return f.getInt(activity);
    }

    protected Intent getActivityResultData(Activity activity) throws NoSuchFieldException, IllegalAccessException {
        // see http://stackoverflow.com/a/33805663
        Field f = Activity.class.getDeclaredField("mResultData");
        f.setAccessible(true);
        return (Intent) f.get(activity);
    }
}
