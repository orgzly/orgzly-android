package com.orgzly.android.ui.notes

import android.content.Context
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils

class ItemGestureDetector(context: Context, private val listener: Listener) :
        RecyclerView.OnItemTouchListener {

    private val gestureDetector = GestureDetectorCompat(context, object: OnSwipeListener() {
        override fun onSwipe(direction: Direction, e1: MotionEvent, e2: MotionEvent): Boolean {
            if (direction == Direction.RIGHT) {
                listener.onSwipe(1, e1, e2)
                return true

            } else if (direction == Direction.LEFT) {
                listener.onSwipe(-1, e1, e2)
                return true
            }

            return false
        }
    })

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, e.action)
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        // if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, e.action)
        return gestureDetector.onTouchEvent(e)
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
    }

    interface Listener {
        fun onSwipe(direction: Int, e1: MotionEvent, e2: MotionEvent)
    }

    companion object {
        private val TAG = ItemGestureDetector::class.java.name
    }
}
