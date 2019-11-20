package com.orgzly.android.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import com.orgzly.R
import com.orgzly.databinding.DialogRepeaterBinding
import com.orgzly.org.datetime.OrgInterval
import com.orgzly.org.datetime.OrgRepeater


/**
 * A dialog that prompts the user for the repeater.
 */
class RepeaterPickerDialog(
        context: Context,
        private val callback: OnRepeaterSetListener,
        initialValue: String
) : AlertDialog(context) {

    private var binding: DialogRepeaterBinding =
            DialogRepeaterBinding.inflate(LayoutInflater.from(context))


    init {
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.ok)) { _, _ ->
            val repeater = repeaterFromViews()
            callback.onRepeaterSet(repeater)
        }

        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel)) { _, _ ->
            cancel()
        }

        val types = context.resources.getStringArray(R.array.repeater_types)
        val units = context.resources.getStringArray(R.array.time_units)

        binding.typePicker.apply {
            minValue = 0
            maxValue = types.size - 1
            displayedValues = types
            setOnValueChangedListener { _, _, newVal ->
                updateTypeDescription(newVal)
            }
        }

        binding.valuePicker.apply {
            minValue = 1
            maxValue = 100
            wrapSelectorWheel = false
        }

        binding.unitPicker.apply {
            minValue = 0
            maxValue = units.size - 1
            displayedValues = units
            wrapSelectorWheel = false
        }

        setView(binding.root)

        setViewsFromString(initialValue)

        setTitle(R.string.repeater_dialog_title)
    }


    private fun updateTypeDescription(index: Int) {
        binding.typeDescription.text =
                context.resources.getStringArray(R.array.repeater_types_desc)[index]
    }

    private fun setViewsFromString(repeaterValue: String) {
        val repeater = OrgRepeater.parse(repeaterValue)

        binding.typePicker.value = repeater.type.ordinal

        binding.valuePicker.let { valuePicker ->
            // Increase the maximum if needed
            if (valuePicker.maxValue < repeater.value) {
                valuePicker.maxValue = repeater.value
                /*
                 * Has to be called after setting minimum and maximum values,
                 * per http://stackoverflow.com/a/21065844.
                 */
                valuePicker.wrapSelectorWheel = false
            }

            valuePicker.value = repeater.value
        }

        binding.unitPicker.value = repeater.unit.ordinal

        updateTypeDescription(binding.typePicker.value)
    }

    private fun repeaterFromViews(): OrgRepeater {
        val type = binding.typePicker.value.let {
            when (it) {
                0 -> OrgRepeater.Type.CUMULATE
                1 -> OrgRepeater.Type.CATCH_UP
                2 -> OrgRepeater.Type.RESTART
                else -> throw IllegalArgumentException("Unexpected type spinner position ($it)")
            }
        }

        val value = binding.valuePicker.value

        val unit = binding.unitPicker.value.let {
            when (it) {
                0 -> OrgInterval.Unit.HOUR
                1 -> OrgInterval.Unit.DAY
                2 -> OrgInterval.Unit.WEEK
                3 -> OrgInterval.Unit.MONTH
                4 -> OrgInterval.Unit.YEAR
                else -> throw IllegalArgumentException("Unexpected unit spinner position ($it)")
            }
        }

        return OrgRepeater(type, value, unit)
    }

    override fun onSaveInstanceState(): Bundle {
        return super.onSaveInstanceState().apply {
            putInt(TYPE, binding.typePicker.value)
            putInt(VALUE, binding.valuePicker.value)
            putInt(UNIT, binding.unitPicker.value)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        binding.typePicker.value = savedInstanceState.getInt(TYPE)
        binding.valuePicker.value = savedInstanceState.getInt(VALUE)
        binding.unitPicker.value = savedInstanceState.getInt(UNIT)
    }

    /**
     * The callback interface used to indicate the user is done setting the repeater.
     */
    interface OnRepeaterSetListener {
        fun onRepeaterSet(repeater: OrgRepeater)
    }

    companion object {
        private const val TYPE = "type"
        private const val UNIT = "unit"
        private const val VALUE = "value"
    }
}