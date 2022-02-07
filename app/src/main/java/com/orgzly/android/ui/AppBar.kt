package com.orgzly.android.ui

class AppBar {
    sealed class State(val count: Int) {
        data class Default(val n: Int = 0): State(n)
        data class MainSelection(val n: Int): State(n)
        data class NextSelection(val n: Int): State(n)
    }

    val state: SingleLiveEvent<State> = SingleLiveEvent()

    fun toState(count: Int) {
        if (count == 0) {
            state.postValue(State.Default())
        } else {
            when (state.value) { // Current state
                is State.MainSelection ->
                    // Maintain state, update count
                    state.postValue(State.MainSelection(count))
                is State.NextSelection ->
                    // Maintain state, update count
                    state.postValue(State.NextSelection(count))
                is State.Default ->
                    state.postValue(State.MainSelection(count))
                null ->
                    state.postValue(State.MainSelection(count))
            }
        }
    }

    fun toDefault() {
        this.state.postValue(State.Default())
    }

    fun toMainSelection() {
        this.state.postValue(State.MainSelection(noOfSelected()))
    }
    fun toNextSelection() {
        this.state.postValue(State.NextSelection(noOfSelected()))
    }

    private fun noOfSelected(): Int {
        return state.value?.count ?: 0
    }

    fun handleOnBackPressed() {
        when (state.value) {
            is State.MainSelection ->
                toDefault()
            is State.NextSelection ->
                toMainSelection()
            else -> {
                // Do nothing
            }
        }
    }
}