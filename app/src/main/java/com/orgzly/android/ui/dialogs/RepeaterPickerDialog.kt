package com.orgzly.android.ui.dialogs

import android.content.Context
import com.orgzly.R
import com.orgzly.org.datetime.OrgInterval
import com.orgzly.org.datetime.OrgRepeater

class RepeaterPickerDialog(context: Context, initialValue: String, val onSet: (OrgRepeater) -> Unit) :
        PeriodWithTypePickerDialog(
                context,
                R.string.repeater_dialog_title,
                R.string.repeater_description,
                R.array.repeater_types,
                R.array.repeater_types_description,
                initialValue) {

    init {
        setup()
    }

    override fun set(typeIndex: Int, interval: OrgInterval) {
        val type = when (typeIndex) {
            0 -> OrgRepeater.Type.CUMULATE
            1 -> OrgRepeater.Type.CATCH_UP
            2 -> OrgRepeater.Type.RESTART
            else -> throw IllegalArgumentException("Unexpected type spinner position ($typeIndex)")
        }

        onSet(OrgRepeater(type, interval.value, interval.unit))
    }

    override fun parseValue(value: String): Pair<Int, OrgInterval> {
        val repeater = OrgRepeater.parse(value)

        return Pair(repeater.type.ordinal, OrgInterval(repeater.value, repeater.unit))
    }
}