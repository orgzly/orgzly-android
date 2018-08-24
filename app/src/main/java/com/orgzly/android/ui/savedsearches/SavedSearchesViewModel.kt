package com.orgzly.android.ui.savedsearches

import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.ui.CommonViewModel
import androidx.lifecycle.LiveData

class SavedSearchesViewModel(dataRepository: DataRepository) : CommonViewModel() {
    val savedSearches: LiveData<List<SavedSearch>> by lazy {
        dataRepository.getSavedSearchesLiveData()
    }
}