package com.orgzly.android.ui

import android.os.Bundle
import androidx.annotation.ColorInt
import android.view.View
import com.orgzly.R
import com.orgzly.android.ui.util.styledAttributes
import java.util.*

class Selection {
    private val idSet = TreeSet<Long>()

    /** Map from adapter ID to real note ID, used for agenda. */
    private var idMap: HashMap<Long, Long>? = null

    fun getIds(): Set<Long> {
        return (idMap?.let { map -> idSet.mapNotNull { map[it] } } ?: idSet).toSet()
    }

    fun getOnly(): Long? {
        return if (idSet.isNotEmpty()) {
            getIds().first()
        } else {
            null
        }
    }

    val count: Int
        get() = idSet.size


    operator fun contains(id: Long): Boolean {
        return idSet.contains(id)
    }

    fun toggle(id: Long) {
        if (contains(id)) {
            idSet.remove(id)
        } else {
            idSet.add(id)
        }
    }

    // TODO: Choose mode when creating selection
    fun toggleSingleSelect(id: Long) {
        if (contains(id)) {
            idSet.clear()
        } else {
            idSet.clear()
            idSet.add(id)
        }
    }

    fun clear() {
        idSet.clear()
    }

    fun setMap(map: HashMap<Long, Long>) {
        idMap = map
    }

    fun removeNonExistent(dataIds: Set<Long>) {
        idSet.removeAll { it !in dataIds }
    }

    /**
     * Save selected items.
     * Restored with [Selection.restoreIds].
     */
    fun saveIds(bundle: Bundle) {
        if (count > 0) {
            val idsArray = LongArray(idSet.size)
            var i = 0
            for (id in idSet) {
                idsArray[i++] = id
            }
            bundle.putLongArray(SAVED_BUNDLE_KEY, idsArray)
        } else {
            bundle.remove(SAVED_BUNDLE_KEY)
        }
    }

    /**
     * Restore selected items.
     * Saved with [Selection.saveIds].
     */
    fun restoreIds(bundle: Bundle?) {
        idSet.clear()

        if (bundle != null && bundle.containsKey(SAVED_BUNDLE_KEY)) {
            val ids = bundle.getLongArray(SAVED_BUNDLE_KEY)
            if (ids != null) {
                for (id in ids) {
                    this.idSet.add(id)
                }
            }
        }
    }

    @ColorInt
    private var selectionBgColor = 0

    fun setIsSelectedBackground(view: View, id: Long) {
        if (selectionBgColor == 0) {
            selectionBgColor = view.context.styledAttributes(R.styleable.ColorScheme) { typedArray ->
                typedArray.getColor(R.styleable.ColorScheme_item_selected_bg_color, 0)
            }
        }

        if (contains(id)) {
            view.setBackgroundColor(selectionBgColor)
        } else {
            view.setBackgroundResource(0)
        }
    }

    companion object {
        private const val SAVED_BUNDLE_KEY = "list_of_selected_ids"
    }
}
