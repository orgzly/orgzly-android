package com.orgzly.android.ui

import androidx.lifecycle.ViewModel

open class CommonViewModel : ViewModel() {
    val snackBarMessage: SingleLiveEvent<Int> = SingleLiveEvent()

    val errorEvent: SingleLiveEvent<Throwable> = SingleLiveEvent()

    fun catchAndPostError(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            e.printStackTrace()
            errorEvent.postValue(e)
        }
    }

    override fun onCleared() {
    }
}