package com.orgzly.android.ui.util

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.annotation.StyleableRes

fun <R> Context.styledAttributes(@StyleableRes attrs: IntArray, f: (typedArray: TypedArray) -> R): R {
    val typedArray = obtainStyledAttributes(attrs)
    try {
        return f(typedArray)
    } finally {
        typedArray.recycle()
    }
}

fun <R> Context.styledAttributes(set: AttributeSet, @StyleableRes attrs: IntArray, f: (typedArray: TypedArray) -> R): R {
    val typedArray = obtainStyledAttributes(set, attrs)
    try {
        return f(typedArray)
    } finally {
        typedArray.recycle()
    }
}
