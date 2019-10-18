package com.orgzly.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.orgzly.R;
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.data.DbRepoBookRepository;
import com.orgzly.android.db.OrgzlyDatabase;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.prefs.AppPreferencesValues;
import com.orgzly.android.repos.RepoFactory;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.datetime.OrgDateTime;

import org.junit.After;
import org.junit.Before;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Calendar;

import androidx.core.content.pm.PackageInfoCompat;
import androidx.test.platform.app.InstrumentationRegistry;

/**
 * Sets up the environment for tests, such as shelf, preferences and contexts.
 *
 * Inherited by all tests.
 */
public class OrgzlyTest {
    protected Context context;

    protected TestUtils testUtils;

    private AppPreferencesValues prefValues;

    protected DbRepoBookRepository dbRepoBookRepository;

    private UserTimeFormatter userTimeFormatter;

    protected LocalStorage localStorage;

    protected DataRepository dataRepository;

    private OrgzlyDatabase database;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        database = OrgzlyDatabase.forFile(context, OrgzlyDatabase.NAME_FOR_TESTS);

        dbRepoBookRepository = new DbRepoBookRepository(database);

        localStorage = new LocalStorage(context);

        RepoFactory repoFactory = new RepoFactory(context, dbRepoBookRepository);

        dataRepository = new DataRepository(
                context, database, repoFactory, context.getResources(), localStorage);

        testUtils = new TestUtils(dataRepository, dbRepoBookRepository);

        userTimeFormatter = new UserTimeFormatter(context);

        // localStorage.cleanup();

        setupPreferences();

        dataRepository.clearDatabase();
    }

    @After
    public void tearDown() throws Exception {
        restorePreferences();

        database.close();
    }

    private void setupPreferences() {
        /* Save preferences' values so they can be restored later. */
        prefValues = AppPreferences.getAllValues(context);

        /* Set all preferences to their default values. */
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> AppPreferences.setToDefaults(context));

        /* Modify preferences for tests. */
        setPreferencesForTests();
    }

    /**
     * Change some preferences for tests.
     */
    private void setPreferencesForTests() {
        /* Last used version. */
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int versionCode = (int) PackageInfoCompat.getLongVersionCode(info);
            AppPreferences.lastUsedVersionCode(context, versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        /* Manual notebook already loaded. */
        AppPreferences.isGettingStartedNotebookLoaded(context, true);

        /* Default states. */
        AppPreferences.states(context, "TODO NEXT | DONE");

        /* Display *all* notebook info. */
        AppPreferences.displayedBookDetails(
                context,
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

    protected String userDateTime(String s) {
        return userTimeFormatter.formatAll(OrgDateTime.parse(s));
    }

    protected String defaultDialogUserDate() {
        OrgDateTime time = new OrgDateTime(true);

        /* Default time is now + 1h.
         * TODO: We shouldn't be able to do this - make OrgDateTime immutable.
         */
        Calendar cal = time.getCalendar();
        cal.add(Calendar.HOUR_OF_DAY, 1);

        return userTimeFormatter.formatDate(time);
    }

    protected String currentUserDate() {
        OrgDateTime time = new OrgDateTime(true);
        return userTimeFormatter.formatDate(time);
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
