package com.orgzly.android.ui

import androidx.lifecycle.ViewModel

open class CommonViewModel : ViewModel() {
    val errorEvent: SingleLiveEvent<Throwable> = SingleLiveEvent()

    override fun onCleared() {
    }
}