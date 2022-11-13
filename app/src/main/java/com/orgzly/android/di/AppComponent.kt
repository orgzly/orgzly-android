package com.orgzly.android.di

import com.orgzly.android.NewNoteBroadcastReceiver
import com.orgzly.android.NotificationBroadcastReceiver
import com.orgzly.android.SharingShortcutsManager
import com.orgzly.android.TimeChangeBroadcastReceiver
import com.orgzly.android.di.module.ApplicationModule
import com.orgzly.android.di.module.DataModule
import com.orgzly.android.di.module.DatabaseModule
import com.orgzly.android.reminders.NoteReminders
import com.orgzly.android.reminders.RemindersBroadcastReceiver
import com.orgzly.android.sync.SyncWorker
import com.orgzly.android.ui.BookChooserActivity
import com.orgzly.android.ui.logs.AppLogsActivity
import com.orgzly.android.ui.TemplateChooserActivity
import com.orgzly.android.ui.books.BooksFragment
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.sync.SyncFragment
import com.orgzly.android.ui.note.NoteFragment
import com.orgzly.android.ui.notes.NotesFragment
import com.orgzly.android.ui.notes.book.BookFragment
import com.orgzly.android.ui.notes.book.BookPrefaceFragment
import com.orgzly.android.ui.notes.query.agenda.AgendaFragment
import com.orgzly.android.ui.notes.query.search.SearchFragment
import com.orgzly.android.ui.refile.RefileFragment
import com.orgzly.android.ui.repo.BrowserActivity
import com.orgzly.android.ui.repo.directory.DirectoryRepoActivity
import com.orgzly.android.ui.repo.dropbox.DropboxRepoActivity
import com.orgzly.android.ui.repo.git.GitRepoActivity
import com.orgzly.android.ui.repo.webdav.WebdavRepoActivity
import com.orgzly.android.ui.repos.ReposActivity
import com.orgzly.android.ui.savedsearch.SavedSearchFragment
import com.orgzly.android.ui.savedsearches.SavedSearchesFragment
import com.orgzly.android.ui.settings.SettingsActivity
import com.orgzly.android.ui.share.ShareActivity
import com.orgzly.android.usecase.UseCaseRunner
import com.orgzly.android.usecase.UseCaseWorker
import com.orgzly.android.widgets.ListWidgetProvider
import com.orgzly.android.widgets.ListWidgetSelectionActivity
import com.orgzly.android.widgets.ListWidgetService
import dagger.Component
import javax.inject.Singleton


@Singleton
@Component(modules = [
    ApplicationModule::class,
    DatabaseModule::class,
    DataModule::class
])
interface AppComponent {
    fun inject(arg: MainActivity)
    fun inject(arg: ReposActivity)
    fun inject(arg: DropboxRepoActivity)
    fun inject(arg: DirectoryRepoActivity)
    fun inject(arg: WebdavRepoActivity)
    fun inject(arg: GitRepoActivity)
    fun inject(arg: BrowserActivity)
    fun inject(arg: SettingsActivity)
    fun inject(arg: ShareActivity)
    fun inject(arg: BookChooserActivity)
    fun inject(arg: TemplateChooserActivity)
    fun inject(arg: ListWidgetSelectionActivity)
    fun inject(arg: AppLogsActivity)

    fun inject(arg: BooksFragment)
    fun inject(arg: NotesFragment)
    fun inject(arg: BookFragment)
    fun inject(arg: BookPrefaceFragment)
    fun inject(arg: SearchFragment)
    fun inject(arg: AgendaFragment)
    fun inject(arg: NoteFragment)
    fun inject(arg: SavedSearchesFragment)
    fun inject(arg: SavedSearchFragment)
    fun inject(arg: RefileFragment)
    fun inject(arg: SyncFragment)

    fun inject(arg: SyncWorker)
    fun inject(arg: NoteReminders)
    fun inject(arg: UseCaseRunner.Factory)
    fun inject(arg: UseCaseWorker)
    fun inject(arg: ListWidgetService)
    fun inject(arg: ListWidgetProvider)
    fun inject(arg: NewNoteBroadcastReceiver)
    fun inject(arg: TimeChangeBroadcastReceiver)
    fun inject(arg: RemindersBroadcastReceiver)
    fun inject(arg: NotificationBroadcastReceiver)
    fun inject(arg: SharingShortcutsManager)
}