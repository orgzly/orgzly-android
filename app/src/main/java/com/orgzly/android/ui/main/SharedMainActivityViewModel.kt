package com.orgzly.android.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.orgzly.android.ui.SingleLiveEvent

class SharedMainActivityViewModel : ViewModel() {
    data class FragmentState(
        val tag: String,
        val title: CharSequence?,
        val subTitle: CharSequence?,
        val selectionCount: Int)

    val fragmentState: MutableLiveData<FragmentState> = MutableLiveData()

    fun setFragment(
        tag: String,
        title: CharSequence?,
        subTitle: CharSequence?,
        selectionCount: Int) {
        fragmentState.value = FragmentState(tag, title, subTitle, selectionCount)
    }

    val drawerLockState: MutableLiveData<Boolean> = MutableLiveData()

    fun lockDrawer() {
        drawerLockState.value = true
    }

    fun unlockDrawer() {
        drawerLockState.value = false
    }

    val snackbarWithReposLink: SingleLiveEvent<String> = SingleLiveEvent()

    fun showSnackbarWithReposLink(message: String) {
        snackbarWithReposLink.postValue(message)
    }
}