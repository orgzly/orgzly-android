package com.orgzly.android.ui.savedsearches

import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.ui.CommonViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.orgzly.android.ui.AppBar

class SavedSearchesViewModel(dataRepository: DataRepository) : CommonViewModel() {
    enum class ViewState {
        LOADING,
        LOADED,
        EMPTY
    }

    val viewState = MutableLiveData<ViewState>(ViewState.LOADING)

    val data: LiveData<List<SavedSearch>> by lazy {
        Transformations.map(dataRepository.getSavedSearchesLiveData()) { searches ->
            viewState.value = if (searches.isNotEmpty()) {
                ViewState.LOADED
            } else {
                ViewState.EMPTY
            }

            searches
        }
    }

    companion object {
        const val APP_BAR_DEFAULT_MODE = 0
        const val APP_BAR_SELECTION_MODE = 1
    }

    val appBar = AppBar(mapOf(
        APP_BAR_DEFAULT_MODE to null,
        APP_BAR_SELECTION_MODE to APP_BAR_DEFAULT_MODE))
}