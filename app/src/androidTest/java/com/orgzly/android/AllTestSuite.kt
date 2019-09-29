package com.orgzly.android

import com.orgzly.android.espresso.*
import com.orgzly.android.misc.*
import com.orgzly.android.query.*
import com.orgzly.android.reminders.*
import com.orgzly.android.repos.*
import com.orgzly.android.ui.*
import com.orgzly.android.uiautomator.ListWidgetTest
import com.orgzly.android.usecase.*
import com.orgzly.android.util.*
import org.junit.Ignore
import org.junit.runner.RunWith
import org.junit.runners.Suite

@Ignore("Test suite")
@RunWith(Suite::class)
@Suite.SuiteClasses(
        ActionModeTest::class,
        AgendaFragmentTest::class,
        BookChooserActivityTest::class,
        BookPrefaceTest::class,
        BooksSortOrderTest::class,
        BooksTest::class,
        BookTest::class,
        CreatedAtPropertyTest::class,
        ExternalLinksTest::class,
        InternalLinksTest::class,
        MiscTest::class,
        NewNoteTest::class,
        NoteEventsTest::class,
        NoteFragmentTest::class,
        QueryFragmentTest::class,
        ReposActivityTest::class,
        SavedSearchesFragmentTest::class,
        SettingsChangeTest::class,
        SettingsFragmentTest::class,
        ShareActivityTest::class,
        SyncingTest::class,

        BookNameTest::class,
        BookParsingTest::class,
        CreatedAtPropertyTest::class,
        DataTest::class,
        SettingsTest::class,
        StateChangeTest::class,
        StructureTest::class,
        UriTest::class,

        QueryTest::class,
        QueryTokenizerTest::class,
        QueryUtilsTest::class,

        ReminderServiceTest::class,

        DataRepositoryTest::class,
        DirectoryRepoTest::class,
        DropboxRepoTest::class,
        LocalDbRepoTest::class,
        RepoFactoryTest::class,
        SyncTest::class,

        ImageLoaderTest::class,

        ListWidgetTest::class,

        NoteUpdateDeadlineTimeTest::class,
        NoteUpdateScheduledTimeTest::class,

        AgendaUtilsTest::class,
        EncodingDetectTest::class,
        MiscUtilsTest::class,
        OrgFormatterLinkTest::class,
        OrgFormatterMiscTest::class,
        OrgFormatterSpeedTest::class,
        OrgFormatterStyleTextTest::class,
        UriUtilsTest::class)
class AllTestSuite