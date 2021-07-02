package com.orgzly.android.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.savedsearches.SavedSearchesViewModel
import com.orgzly.android.ui.savedsearches.SavedSearchesViewModelFactory
import com.orgzly.android.util.LogUtils
import javax.inject.Inject

/**
 * Widget selection.
 */
class ListWidgetSelectionActivity : AppCompatActivity(), OnViewHolderClickListener<SavedSearch> {

    @Inject
    lateinit var dataRepository: DataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

        WidgetStyle.updateActivity(this)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list_widget_selection)

        val viewFlipper = findViewById<ViewFlipper>(R.id.activity_list_widget_selection_view_flipper)

        val viewAdapter = ListWidgetSelectionAdapter(this)
        viewAdapter.setHasStableIds(true)

        findViewById<RecyclerView>(R.id.activity_list_widget_selection_recycler_view).let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = viewAdapter
        }

        val factory = SavedSearchesViewModelFactory.getInstance(dataRepository)
        val model = ViewModelProvider(this, factory).get(SavedSearchesViewModel::class.java)

        model.viewState.observe(this, Observer {
            viewFlipper.displayedChild = when (it) {
                SavedSearchesViewModel.ViewState.EMPTY -> 1
                else -> 0
            }
        })

        model.savedSearches.observe(this, Observer { savedSearches ->
            viewAdapter.submitList(savedSearches)

            val ids = savedSearches.mapTo(hashSetOf()) { it.id }

            viewAdapter.getSelection().removeNonExistent(ids)
        })
    }

    override fun onClick(view: View, position: Int, item: SavedSearch) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "onBottomActionItemClicked", item)

        /* notify Widget */
        val intent = Intent(this, ListWidgetProvider::class.java)
        intent.action = AppIntent.ACTION_SET_LIST_WIDGET_SELECTION
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
        intent.putExtra(AppIntent.EXTRA_SAVED_SEARCH_ID, item.id)
        sendBroadcast(intent)

        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onLongClick(view: View, position: Int, item: SavedSearch) {
    }

    companion object {

        private val TAG = ListWidgetSelectionActivity::class.java.name
    }
}
