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

    enum class LoadState {
        IN_PROGRESS,
        DONE,
        NO_RESULTS
    }

    val dataLoadState = MutableLiveData<LoadState>(LoadState.IN_PROGRESS)

    fun setLoadState(state: LoadState) {
        dataLoadState.value = state
    }

    data class Params(val query: String?, val defaultPriority: String)

    private val notesParams = MutableLiveData<Params>()

    private val notesLiveData: LiveData<List<NoteView>>

    init {
        notesLiveData = Transformations.switchMap(notesParams) { params ->
            if (params.query != null) {
                dataRepository.selectNotesFromQueryLiveData(params.query)
            } else {
                MutableLiveData<List<NoteView>>()
            }
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

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        private val TAG = QueryViewModel::class.java.name
    }
}