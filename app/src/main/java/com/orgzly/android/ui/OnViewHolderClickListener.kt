package com.orgzly.android.ui

import android.view.View

interface OnViewHolderClickListener<T> {
    fun onClick(view: View, position: Int, item: T)
    fun onLongClick(view: View, position: Int, item: T)
}