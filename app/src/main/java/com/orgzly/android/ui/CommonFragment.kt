package com.orgzly.android.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.sync.SyncState
import com.orgzly.android.sync.SyncState.Type.*
import com.orgzly.android.util.LogUtils
import kotlinx.coroutines.launch

/*
 * Used by all MainActivity's fragments
 */
open class CommonFragment : Fragment() {
    private lateinit var syncProgressViewModel: SyncProgressViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        syncProgressViewModel = ViewModelProvider(this)[SyncProgressViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncProgressViewModel.syncState.collect { state ->
                    if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "MutableSharedFlow", state)

                    if (state != null) {
                        updateSyncProgressIndicator(view, state)
                    }
                }
            }
        }
    }

    private fun updateSyncProgressIndicator(view: View, state: SyncState) {
        val progressIndicator = view.findViewById<LinearProgressIndicator>(R.id.sync_toolbar_progress)

        when (state.type) {
            CANCELING,
            STARTING,
            COLLECTING_BOOKS,
            BOOKS_COLLECTED -> {
                progressIndicator.isIndeterminate = true
                progressIndicator.visibility = View.VISIBLE
            }

            BOOK_STARTED,
            BOOK_ENDED -> {
                progressIndicator.isIndeterminate = false
                progressIndicator.max = state.total
                progressIndicator.progress = state.current
                progressIndicator.visibility = View.VISIBLE
            }

            AUTO_SYNC_NOT_STARTED,
            FINISHED,
            CANCELED,
            FAILED_NO_REPOS,
            FAILED_NO_CONNECTION,
            FAILED_NO_STORAGE_PERMISSION,
            FAILED_NO_BOOKS_FOUND,
            FAILED_EXCEPTION ->
                progressIndicator.visibility = View.GONE
        }
    }

    companion object {
        private val TAG = CommonFragment::class.java.name
    }
}