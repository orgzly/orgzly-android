package com.orgzly.android.ui.dialogs

import android.content.Context
import com.orgzly.R
import com.orgzly.org.datetime.OrgDelay
import com.orgzly.org.datetime.OrgInterval

class WarningPeriodPickerDialog(context: Context, initialValue: String, val onSet: (OrgDelay) -> Unit) :
        PeriodWithTypePickerDialog(
                context,
                R.string.warning_period_dialog_title,
                R.string.warning_period_description,
                R.array.warning_period_types,
                0,
                initialValue) {

    init {
        setup()
    }

    override fun set(typeIndex: Int, interval: OrgInterval) {
        val type = when (typeIndex) {
            0 -> OrgDelay.Type.ALL
            else -> throw IllegalArgumentException("Unexpected type spinner position ($typeIndex)")
        }

        onSet(OrgDelay(type, interval.value, interval.unit))
    }

    override fun parseValue(value: String): Pair<Int, OrgInterval> {
        val delay = OrgDelay.parse(value)

        return Pair(delay.type.ordinal, OrgInterval(delay.value, delay.unit))
    }
}