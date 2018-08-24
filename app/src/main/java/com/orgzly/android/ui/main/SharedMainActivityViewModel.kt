package com.orgzly.android.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedMainActivityViewModel : ViewModel() {
    val drawerLockState: MutableLiveData<Boolean> = MutableLiveData()

    val fragmentState: MutableLiveData<FragmentState> = MutableLiveData()

    fun lockDrawer() {
        drawerLockState.value = true
    }

    fun unlockDrawer() {
        drawerLockState.value = false
    }

    fun setFragment(
            tag: String,
            title: CharSequence?,
            subTitle: CharSequence?,
            selectionCount: Int) {
        fragmentState.value = FragmentState(tag, title, subTitle, selectionCount)
    }

    data class FragmentState(
            val tag: String,
            val title: CharSequence?,
            val subTitle: CharSequence?,
            val selectionCount: Int)
}