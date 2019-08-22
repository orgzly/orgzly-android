package com.orgzly.android.ui.notes.query

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.orgzly.BuildConfig
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.util.LogUtils


class QueryViewModel(private val dataRepository: DataRepository) : CommonViewModel() {

    enum class ViewState {
        LOADING,
        LOADED,
        EMPTY
    }

    val viewState = MutableLiveData(ViewState.LOADING)

    data class Params(val query: String?, val defaultPriority: String)

    private val notesParams = MutableLiveData<Params>()

    private val notesLiveData = Transformations.switchMap(notesParams) { params ->
        if (params.query != null) {
            Transformations.map(dataRepository.selectNotesFromQueryLiveData(params.query)) {
                viewState.value = if (it.isNotEmpty()) {
                    ViewState.LOADED
                } else {
                    ViewState.EMPTY
                }

                it
            }
        } else {
            MutableLiveData<List<NoteView>>()
        }
    }

    /* Triggers querying only if parameters changed. */
    fun refresh(query: String?, defaultPriority: String) {
        Params(query, defaultPriority).let {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, it)
            notesParams.value = it
        }
    }

    fun notes(): LiveData<List<NoteView>> {
        return notesLiveData
    }

    companion object {
        private val TAG = QueryViewModel::class.java.name
    }
}