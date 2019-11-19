package com.orgzly.android.ui.dialogs

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.Window

import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders

import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.UserTimeFormatter
import com.orgzly.databinding.DialogTimestampBinding
import com.orgzly.databinding.DialogTimestampTitleBinding
import com.orgzly.org.datetime.OrgDateTime

import java.util.Calendar
import java.util.TreeSet

class TimestampDialogFragment : DialogFragment(), View.OnClickListener {


    /** Use by caller to know what's the timestamp for (scheduled, deadline, etc.).  */
    private var mId: Int = 0

    private var mNoteIds = TreeSet<Long>()

    private var mActivityListener: OnDateTimeSetListener? = null
    private var mContext: Context? = null

    private lateinit var userTimeFormatter: UserTimeFormatter

    private lateinit var binding: DialogTimestampBinding
    private lateinit var titleBinding: DialogTimestampTitleBinding

    private lateinit var viewModel: TimestampDialogViewModel

    // Currently opened picker. Keep the reference to avoid leak.
    private var pickerDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userTimeFormatter = UserTimeFormatter(context)


        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
    }

    /**
     * Create a new instance of a dialog.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)


        /* This makes sure that the fragment has implemented
         * the callback interface. If not, it throws an exception
         */
        check(parentFragment is OnDateTimeSetListener) { "Fragment " + parentFragment + " must implement " + OnDateTimeSetListener::class.java }

        mActivityListener = parentFragment as OnDateTimeSetListener?

        mContext = activity


        val args = requireArguments()

        mId = args.getInt(ARG_DIALOG_ID)
        mNoteIds.addAll(args.getLongArray(ARG_NOTE_IDS)?.toList() ?: emptyList())

        // Ignored, replaced by Org timestamp
        // val title = args.getInt(ARG_TITLE)

        val dateTimeString = args.getString(ARG_TIME)


        LayoutInflater.from(activity).let { inflater ->
            binding = DialogTimestampBinding.inflate(inflater)
            titleBinding = DialogTimestampTitleBinding.inflate(inflater)
        }


        val factory = TimestampDialogViewModelFactory.getInstance(dateTimeString)
        viewModel = ViewModelProviders.of(this, factory).get(TimestampDialogViewModel::class.java)

        setupObservers()


        // Pickers

        binding.datePickerButton.setOnClickListener(this)

        binding.timePickerButton.setOnClickListener(this)
        binding.timeUsedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsTimeUsed(isChecked)
        }

        binding.endTimePickerButton.setOnClickListener(this)
        binding.endTimeUsedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsEndTimeUsed(isChecked)
        }

        binding.repeaterPickerButton.setOnClickListener(this)
        binding.repeaterUsedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsRepeaterUsed(isChecked)
        }

        binding.delayPickerButton.setOnClickListener(this)
        binding.delayUsedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsDelayUsed(isChecked)
        }

        // Shortcuts
        binding.todayButton.setOnClickListener(this)
        binding.tomorrowButton.setOnClickListener(this)
        binding.nextWeekButton.setOnClickListener(this)


        return AlertDialog.Builder(mContext)
                .setCustomTitle(titleBinding.root)
                .setView(binding.root)
                .setPositiveButton(R.string.set) { _, _ ->
                    val time = viewModel.getOrgDateTime()
                    mActivityListener?.onDateTimeSet(mId, mNoteIds, time)
                }
                .setNeutralButton(R.string.clear) { _, _ ->
                    mActivityListener?.onDateTimeSet(mId, mNoteIds, null)
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    mActivityListener?.onDateTimeAborted(mId, mNoteIds)
                }
                .show()
    }

    /**
     * Receives all dialog's clicks
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.date_picker_button -> {
                val yearMonthDay = viewModel.getYearMonthDay()

                val picker = DatePickerDialog(mContext!!, { _, year, monthOfYear, dayOfMonth ->
                    viewModel.set(year, monthOfYear, dayOfMonth)
                }, yearMonthDay.first, yearMonthDay.second, yearMonthDay.third)

                picker.setOnDismissListener {
                    pickerDialog = null
                }

                pickerDialog = picker.apply {
                    show()
                }
            }

            R.id.time_picker_button -> {
                val hourMinute = viewModel.getTimeHourMinute()

                val picker = TimePickerDialog(mContext, { _, hourOfDay, minute ->
                    viewModel.setTime(hourOfDay, minute)
                }, hourMinute.first, hourMinute.second, DateFormat.is24HourFormat(context))

                picker.setOnDismissListener {
                    pickerDialog = null
                }

                pickerDialog = picker.apply {
                    show()
                }
            }

            R.id.end_time_picker_button -> {
                val hourMinute = viewModel.getEndTimeHourMinute()

                val picker = TimePickerDialog(mContext, { _, hourOfDay, minute ->
                    viewModel.setEndTime(hourOfDay, minute)
                }, hourMinute.first, hourMinute.second, DateFormat.is24HourFormat(context))

                picker.setOnDismissListener {
                    pickerDialog = null
                }

                pickerDialog = picker.apply {
                    show()
                }
            }

            R.id.repeater_picker_button -> {
                val picker = RepeaterPickerDialog(mContext, { repeater ->
                    viewModel.set(repeater)
                }, viewModel.getRepeaterString())

                picker.setOnDismissListener {
                    pickerDialog = null
                }

                pickerDialog = picker.apply {
                    show()
                }
            }

            R.id.today_button -> {
                Calendar.getInstance().apply {
                    viewModel.set(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))
                }
            }

            R.id.tomorrow_button -> {
                Calendar.getInstance().apply {
                    add(Calendar.DATE, 1)
                }.apply {
                    viewModel.set(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))
                }
            }

            R.id.next_week_button -> {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    add(Calendar.DATE, 7)
                }.apply {
                    viewModel.set(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.dateTime.observe(requireActivity(), Observer { dateTime ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, dateTime)

            val orgDateTime = viewModel.getOrgDateTime(dateTime)

            titleBinding.timestamp.text = orgDateTime.toStringWithoutBrackets()

            binding.datePickerButton.text = userTimeFormatter.formatDate(dateTime)

            binding.timePickerButton.text = userTimeFormatter.formatTime(dateTime)
            binding.timeUsedCheckbox.isChecked = dateTime.isTimeUsed

            binding.endTimePickerButton.text = userTimeFormatter.formatEndTime(dateTime)
            binding.endTimeUsedCheckbox.isChecked = dateTime.isEndTimeUsed
            binding.endTimePickerButton.isEnabled = dateTime.isTimeUsed
            binding.endTimeUsedCheckbox.isEnabled = dateTime.isTimeUsed

            binding.repeaterPickerButton.text = userTimeFormatter.formatRepeater(dateTime)
            binding.repeaterUsedCheckbox.isChecked = dateTime.isRepeaterUsed

            binding.delayPickerButton.text = userTimeFormatter.formatDelay(dateTime)
            binding.delayUsedCheckbox.isChecked = dateTime.isDelayUsed
        })
    }

    override fun onPause() {
        super.onPause()

        pickerDialog?.dismiss()
        pickerDialog = null
    }

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    interface OnDateTimeSetListener {
        fun onDateTimeSet(id: Int, noteIds: TreeSet<Long>, time: OrgDateTime?)
        fun onDateTimeAborted(id: Int, noteIds: TreeSet<Long>)
    }

    companion object {
        val FRAGMENT_TAG: String = TimestampDialogFragment::class.java.name

        private val TAG = TimestampDialogFragment::class.java.name

        private const val ARG_DIALOG_ID = "id"
        private const val ARG_TITLE = "title"
        private const val ARG_NOTE_IDS = "note_ids"
        private const val ARG_TIME = "time"


        fun getInstance(id: Int, title: Int, noteId: Long, time: OrgDateTime?): TimestampDialogFragment {
            return getInstance(id, title, setOf(noteId), time)
        }

        fun getInstance(id: Int, title: Int, noteIds: Set<Long>, time: OrgDateTime?): TimestampDialogFragment {
            val fragment = TimestampDialogFragment()

            /* Set arguments for fragment. */
            val bundle = Bundle()

            bundle.putInt(ARG_DIALOG_ID, id)
            bundle.putInt(ARG_TITLE, title)
            bundle.putLongArray(ARG_NOTE_IDS, toArray(noteIds))

            if (time != null) {
                bundle.putString(ARG_TIME, time.toString())
            }

            fragment.arguments = bundle

            return fragment
        }

        private fun toArray(set: Set<Long>): LongArray {
            var i = 0
            val result = LongArray(set.size)

            for (e in set) {
                result[i++] = e
            }

            return result
        }
    }
}