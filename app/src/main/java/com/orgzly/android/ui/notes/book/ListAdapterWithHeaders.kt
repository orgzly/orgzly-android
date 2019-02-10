package com.orgzly.android.ui.notes.book

import androidx.recyclerview.widget.*
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils

abstract class ListAdapterWithHeaders<T, VH : RecyclerView.ViewHolder>(
        private val diffCallback: DiffUtil.ItemCallback<T>,
        private val headers: Int = 1
) : RecyclerView.Adapter<VH>() {

    private val differ by lazy {
        AsyncListDiffer<T>(
                ListUpdateWithHeadersCallback(this),
                AsyncDifferConfig.Builder<T>(diffCallback).build())
    }

    fun submitList(list: List<T>?) {
        differ.submitList(list)
    }

    fun getItem(position: Int): T {
        return differ.currentList[position - headers]
    }

    override fun getItemCount(): Int {
        return differ.currentList.size + headers
    }

    fun getDataItemCount(): Int {
        return differ.currentList.size
    }

    inner class ListUpdateWithHeadersCallback(
            private val adapter: RecyclerView.Adapter<*>
    ) : ListUpdateCallback {

        override fun onInserted(position: Int, count: Int) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, position, count)
            adapter.notifyItemRangeInserted(position + headers, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, position, count)
            adapter.notifyItemRangeRemoved(position + headers, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, fromPosition, toPosition)
            adapter.notifyItemMoved(fromPosition + headers, toPosition + headers)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, position, count)
            adapter.notifyItemRangeChanged(position + headers, count, payload)
        }
    }

    companion object {
        private val TAG = ListAdapterWithHeaders::class.java.name
    }
}