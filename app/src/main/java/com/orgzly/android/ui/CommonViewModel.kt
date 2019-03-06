package com.orgzly.android.ui

import androidx.lifecycle.ViewModel

open class CommonViewModel : ViewModel() {
    val errorEvent: SingleLiveEvent<Throwable> = SingleLiveEvent()

    fun catchAndPostError(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            errorEvent.postValue(e)
        }
    }

    override fun onCleared() {
    }
}