package com.orgzly.android.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.orgzly.android.App
import com.orgzly.android.usecase.NoteUpdateClockingState
import com.orgzly.android.usecase.UseCase
import com.orgzly.android.usecase.UseCaseRunner

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

    fun run(action: UseCase?) {
        App.EXECUTORS.diskIO().execute {
            try {
                UseCaseRunner.run(action!!)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun clockingUpdateRequest(noteIds: Set<Long>, type: Int) {
        run(NoteUpdateClockingState(noteIds, type))
    }

    data class FragmentState(
            val tag: String,
            val title: CharSequence?,
            val subTitle: CharSequence?,
            val selectionCount: Int)
}