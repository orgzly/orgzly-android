package com.orgzly.android.ui

class AppBar(var modes: Map<Int, Int?>) {

    val mode: SingleLiveEvent<Int> = SingleLiveEvent()

    fun toModeFromSelectionCount(count: Int) {
        if (count == 0) {
            // No selection, default mode
            mode.postValue(0)
        } else {
            if (mode.value == 0) {
                // Selection, from default mode
                mode.postValue(1)
            } else {
                // Keep mode
                mode.postValue(mode.value)
            }
        }
    }

    fun toMode(id: Int) {
        this.mode.postValue(id)
    }

    fun handleOnBackPressed() {
        mode.value?.let { currentMode ->
            val previousMode = modes[currentMode]
            if (previousMode != null) {
                toMode(previousMode)
            }
        }
    }
}