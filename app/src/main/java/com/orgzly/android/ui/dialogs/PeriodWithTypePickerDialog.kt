package com.orgzly.android.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.orgzly.R
import com.orgzly.databinding.DialogPeriodWithTypeBinding
import com.orgzly.org.datetime.OrgInterval


/**
 * A dialog that prompts the user for the repeater, delay or warning period.
 */
abstract class PeriodWithTypePickerDialog(
        context: Context,
        @StringRes private val titleId: Int,
        @StringRes private val descriptionId: Int,
        @ArrayRes private val typesId: Int,
        @ArrayRes private val typesDescriptionsId: Int,
        private val initialValue: String
) : AlertDialog(context) {

    abstract fun set(typeIndex: Int, interval: OrgInterval)

    // Returns type index and OrgInterval
    abstract fun parseValue(value: String): Pair<Int, OrgInterval>

    private var binding: DialogPeriodWithTypeBinding =
            DialogPeriodWithTypeBinding.inflate(LayoutInflater.from(context))


    fun setup() {
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.ok)) { _, _ ->
            val typeIndex = binding.typePicker.value

            val interval = getInterval(
                    binding.valuePicker.value,
                    binding.unitPicker.value)

            set(typeIndex, interval)
        }

        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel)) { _, _ ->
            cancel()
        }


        val types = context.resources.getStringArray(typesId)
        binding.typePicker.apply {
            minValue = 0
            maxValue = types.size - 1
            displayedValues = types
            setOnValueChangedListener { _, _, newVal ->
                setTypeDescription(newVal)
            }
        }

        binding.valuePicker.apply {
            minValue = 1
            maxValue = 100
            wrapSelectorWheel = false
        }

        val units = context.resources.getStringArray(R.array.time_units)
        binding.unitPicker.apply {
            minValue = 0
            maxValue = units.size - 1
            displayedValues = units
            wrapSelectorWheel = false
        }

        setView(binding.root)

        setTitle(titleId)

        setDescription()

        setPickerValues(initialValue)

        setTypeDescription(binding.typePicker.value)
    }

    private fun setDescription() {
        binding.dialogDescription.text = context.getString(descriptionId)
    }

    private fun setPickerValues(value: String) {
        val pair = parseValue(value)

        val typeIndex = pair.first
        val interval = pair.second

        binding.typePicker.value = typeIndex

        binding.valuePicker.let { valuePicker ->
            // Increase the maximum if needed
            if (valuePicker.maxValue < interval.value) {
                valuePicker.maxValue = interval.value
                /*
                 * Has to be called after setting minimum and maximum values,
                 * per http://stackoverflow.com/a/21065844.
                 */
                valuePicker.wrapSelectorWheel = false
            }

            valuePicker.value = interval.value
        }

        binding.unitPicker.value = interval.unit.ordinal
    }

    private fun setTypeDescription(index: Int) {
        if (typesDescriptionsId == 0) {
            binding.typeDescription.visibility = View.GONE
        } else {
            binding.typeDescription.text =
                    context.resources.getStringArray(typesDescriptionsId)[index]
            binding.typeDescription.visibility = View.VISIBLE
        }
    }

    private fun getInterval(value: Int, unitIndex: Int): OrgInterval {
        val unit = unitIndex.let {
            when (it) {
                0 -> OrgInterval.Unit.HOUR
                1 -> OrgInterval.Unit.DAY
                2 -> OrgInterval.Unit.WEEK
                3 -> OrgInterval.Unit.MONTH
                4 -> OrgInterval.Unit.YEAR
                else -> throw IllegalArgumentException("Unexpected unit spinner position ($it)")
            }
        }

        return OrgInterval(value, unit)
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

    companion object {
        private const val TYPE = "type"
        private const val UNIT = "unit"
        private const val VALUE = "value"
    }
}