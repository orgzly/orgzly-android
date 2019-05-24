package com.orgzly.android.di.module

import com.orgzly.android.NewNoteBroadcastReceiver
import com.orgzly.android.TimeChangeBroadcastReceiver
import com.orgzly.android.usecase.UseCaseService
import com.orgzly.android.reminders.ReminderService
import com.orgzly.android.sync.SyncService
import com.orgzly.android.ui.BookChooserActivity
import com.orgzly.android.ui.TemplateChooserActivity
import com.orgzly.android.ui.books.BooksFragment
import com.orgzly.android.ui.notes.book.BookPrefaceFragment
import com.orgzly.android.ui.savedsearch.SavedSearchFragment
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.main.SyncFragment
import com.orgzly.android.ui.note.NoteFragment
import com.orgzly.android.ui.notes.book.BookFragment
import com.orgzly.android.ui.notes.query.agenda.AgendaFragment
import com.orgzly.android.ui.notes.query.search.SearchFragment
import com.orgzly.android.ui.notifications.SyncStatusBroadcastReceiver
import com.orgzly.android.ui.refile.RefileFragment
import com.orgzly.android.ui.repo.BrowserActivity
import com.orgzly.android.ui.repos.ReposActivity
import com.orgzly.android.ui.repo.DirectoryRepoActivity
import com.orgzly.android.ui.repo.DropboxRepoActivity
import com.orgzly.android.ui.repo.git.GitRepoActivity
import com.orgzly.android.ui.settings.SettingsActivity
import com.orgzly.android.ui.share.ShareActivity
import com.orgzly.android.ui.savedsearches.SavedSearchesFragment
import com.orgzly.android.widgets.ListWidgetSelectionActivity
import com.orgzly.android.widgets.ListWidgetProvider
import com.orgzly.android.widgets.ListWidgetService
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class AndroidModule {

    // Service

    @ContributesAndroidInjector
    internal abstract fun contributeSyncService(): SyncService

    @ContributesAndroidInjector
    internal abstract fun contributeReminderService(): ReminderService

    @ContributesAndroidInjector
    internal abstract fun contributeActionService(): UseCaseService

    // Class only available on M or later
    // https://github.com/google/dagger/issues/1064
    // @ContributesAndroidInjector
    // internal abstract fun contributeChooserShareTargetService(): ChooserShareTargetService

    @ContributesAndroidInjector
    internal abstract fun contributeListWidgetService(): ListWidgetService

    // BroadcastReceiver

    @ContributesAndroidInjector
    internal abstract fun contributeListWidgetProvider(): ListWidgetProvider

    @ContributesAndroidInjector
    internal abstract fun contributeSyncStatusBroadcastReceiver(): SyncStatusBroadcastReceiver

    @ContributesAndroidInjector
    internal abstract fun contributeNewNoteBroadcastReceiver(): NewNoteBroadcastReceiver

    @ContributesAndroidInjector
    internal abstract fun contributeTimeChangeBroadcastReceiver(): TimeChangeBroadcastReceiver

    // Activity

    @ContributesAndroidInjector
    internal abstract fun contributeSettingsActivity(): SettingsActivity

    @ContributesAndroidInjector
    internal abstract fun contributeShareActivity(): ShareActivity

    @ContributesAndroidInjector
    internal abstract fun contributeReposActivity(): ReposActivity

    @ContributesAndroidInjector
    internal abstract  fun contributeGitRepoActivity(): GitRepoActivity

    @ContributesAndroidInjector
    internal abstract fun contributeDropboxRepoActivity(): DropboxRepoActivity

    @ContributesAndroidInjector
    internal abstract fun contributeDirectoryRepoActivity(): DirectoryRepoActivity

    @ContributesAndroidInjector
    internal abstract fun contributeBrowserActivity(): BrowserActivity

    @ContributesAndroidInjector
    internal abstract fun contributeBookChooserActivity(): BookChooserActivity

    @ContributesAndroidInjector
    internal abstract fun contributeTemplateChooserActivity(): TemplateChooserActivity

    @ContributesAndroidInjector
    internal abstract fun contributeListWidgetSelectionActivity(): ListWidgetSelectionActivity

    @ContributesAndroidInjector
    internal abstract fun contributeMainActivity(): MainActivity

    // Fragment

    @ContributesAndroidInjector
    internal abstract fun contributeBooksFragment(): BooksFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBookFragment(): BookFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSearchFragment(): SearchFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAgendaFragment(): AgendaFragment

    @ContributesAndroidInjector
    internal abstract fun contributeNoteFragment(): NoteFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBookPrefaceFragment(): BookPrefaceFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSavedSearchesFragment(): SavedSearchesFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSavedSearchFragment(): SavedSearchFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSyncFragment(): SyncFragment

    @ContributesAndroidInjector
    internal abstract fun contributeRefileDialogFragment(): RefileFragment
}