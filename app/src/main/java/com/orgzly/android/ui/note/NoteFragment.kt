package com.orgzly.android.ui.note

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.BookUtils
import com.orgzly.android.data.DataRepository
import com.orgzly.android.data.mappers.OrgMapper
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.*
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.book.BookFragment
import com.orgzly.android.ui.share.ShareActivity
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.ui.views.TextViewWithMarkup
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.MiscUtils
import com.orgzly.android.util.SpaceTokenizer
import com.orgzly.android.util.UserTimeFormatter
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange
import com.orgzly.org.parser.OrgParserWriter
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_note.*
import java.util.*
import javax.inject.Inject

/**
 * Note editor.
 */
class NoteFragment : DaggerFragment(), View.OnClickListener, TimestampDialogFragment.OnDateTimeSetListener, DrawerItem {

    @Inject
    internal lateinit var dataRepository: DataRepository

    private var listener: Listener? = null

    private lateinit var viewModel: NoteViewModel

    /* Arguments. */
    private var mIsNew: Boolean = false

    private var mBookId: Long = 0

    /* Could be null if new note is being created. */
    var noteId: Long = 0

    private var place: Place? = null /* Relative location, used for new notes. */

    // Initial title and content, used for when sharing to Orgzly
    private var mInitialTitle: String? = null
    private var mInitialContent: String? = null

    private var notePayload: NotePayload? = null

    private var book: BookView? = null

    private lateinit var scrollView: ScrollView

    private lateinit var title: EditText

    private lateinit var locationView: TextView
    private lateinit var locationButtonView: TextView

    private lateinit var tagsContainer: View
    private lateinit var tagsView: MultiAutoCompleteTextView

    private lateinit var stateContainer: View
    private lateinit var state: TextView

    private lateinit var priorityContainer: View
    private lateinit var priority: TextView

    private lateinit var scheduledTimeContainer: View
    private lateinit var scheduledButton: TextView

    private lateinit var deadlineTimeContainer: View
    private lateinit var deadlineButton: TextView

    private lateinit var closedTimeContainer: ViewGroup
    private lateinit var closedText: TextView

    private lateinit var propertiesContainer: LinearLayout

    private lateinit var editSwitch: ToggleButton
    private lateinit var bodyEdit: EditText
    private lateinit var bodyView: TextViewWithMarkup

    /** Used to switch to note-does-not-exist view, if the note has been deleted.  */
    private lateinit var mViewFlipper: ViewFlipper

    private lateinit var mUserTimeFormatter: UserTimeFormatter

    private var dialog: AlertDialog? = null

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    override fun onAttach(context: Context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, activity)
        super.onAttach(context)

        listener = activity as Listener

        parseArguments()

        mUserTimeFormatter = UserTimeFormatter(context)
    }

    private fun parseArguments() {
        arguments?.apply {
            mIsNew = getBoolean(ARG_IS_NEW)

            /* Book ID must exist. */
            if (!containsKey(ARG_BOOK_ID)) {
                throw IllegalArgumentException(NoteFragment::class.java.simpleName + " requires " + ARG_BOOK_ID + " argument passed")
            }

            mBookId = getLong(ARG_BOOK_ID)

            //        /* Book ID must be valid. */
            //        if (mBookId <= 0) {
            //            throw new IllegalArgumentException("Passed argument book id is not valid (" + mBookId + ")");
            //        }

            /* Note ID might or might not be passed - it depends if note is being edited or created. */
            if (containsKey(ARG_NOTE_ID)) {
                noteId = getLong(ARG_NOTE_ID)

                /* Note ID must be valid if it exists. */
                if (noteId <= 0) {
                    throw IllegalArgumentException("Note id is $noteId")
                }
            }

            /* Location (used for new notes). */
            place = getString(ARG_PLACE)?.let {
                Place.valueOf(it)
            }

            mInitialTitle = getString(ARG_TITLE)
            mInitialContent = getString(ARG_CONTENT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onCreate(savedInstanceState)

        sharedMainActivityViewModel = activity?.let {
            ViewModelProviders.of(it).get(SharedMainActivityViewModel::class.java)
        } ?: throw IllegalStateException("No Activity")

        val factory = NoteViewModelFactory.getInstance(dataRepository, mBookId)

        viewModel = ViewModelProviders.of(this, factory).get(NoteViewModel::class.java)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState)

        val top = inflater.inflate(R.layout.fragment_note, container, false)

        scrollView = top.findViewById(R.id.fragment_note_container)

        title = top.findViewById(R.id.fragment_note_title)

        /*
         * Only works when set from code.
         * We want imeOptions="actionDone", so we can't use textMultiLine.
         */
        title.setHorizontallyScrolling(false)
        title.maxLines = 3

        /* Keyboard's action button pressed. */
        title.setOnEditorActionListener { _, _, _ ->
            save()
            true
        }

        locationView = top.findViewById(R.id.fragment_note_location)
        locationButtonView = top.findViewById(R.id.fragment_note_location_button)

        if (activity is ShareActivity) {
            locationView.visibility = View.GONE
            locationButtonView.visibility = View.VISIBLE
            locationButtonView.setOnClickListener(this)
        } else {
            locationView.visibility = View.VISIBLE
            locationButtonView.visibility = View.GONE
        }


        tagsContainer = top.findViewById(R.id.fragment_note_tags_container)

        tagsView = top.findViewById(R.id.fragment_note_tags)

        /* Hint causes minimum width - when tags' width is smaller then hint's, there is empty space. */
        tagsView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(tagsView.text.toString())) {
                    tagsView.hint = ""
                } else {
                    tagsView.setHint(R.string.fragment_note_tags_hint)
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        setupTagsViewAdapter()

        priorityContainer = top.findViewById(R.id.fragment_note_priority_container)
        priority = top.findViewById(R.id.fragment_note_priority_button)
        priority.setOnClickListener(this)

        stateContainer = top.findViewById(R.id.fragment_note_state_container)
        state = top.findViewById(R.id.fragment_note_state_button)
        state.setOnClickListener(this)

        scheduledTimeContainer = top.findViewById(R.id.fragment_note_scheduled_time_container)
        scheduledButton = top.findViewById(R.id.fragment_note_scheduled_button)
        scheduledButton.setOnClickListener(this)

        deadlineTimeContainer = top.findViewById(R.id.fragment_note_deadline_time_container)
        deadlineButton = top.findViewById(R.id.fragment_note_deadline_button)
        deadlineButton.setOnClickListener(this)

        closedTimeContainer = top.findViewById(R.id.fragment_note_closed_time_container)
        closedText = top.findViewById(R.id.fragment_note_closed_edit_text)
        closedText.setOnClickListener(this)

        propertiesContainer = top.findViewById(R.id.fragment_note_properties_container)

        bodyEdit = top.findViewById(R.id.body_edit)

        bodyView = top.findViewById(R.id.body_view)

        bodyView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                // Update bodyEdit text when checkboxes are clicked
                val text = bodyView.getRawText()
                bodyEdit.setText(text)
            }
        })

        if (activity != null && AppPreferences.isFontMonospaced(context)) {
            bodyEdit.typeface = Typeface.MONOSPACE
            bodyView.typeface = Typeface.MONOSPACE
        }

        editSwitch = top.findViewById(R.id.edit_content_toggle)

        editSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                toEditMode()
            } else {
                toViewMode()
            }
        }

        editSwitch.setOnClickListener {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, editSwitch.isChecked)

            if (editSwitch.isChecked) { // Clicked to edit content
                ActivityUtils.openSoftKeyboard(activity, bodyEdit, scrollView)
            }
        }

        mViewFlipper = top.findViewById(R.id.fragment_note_view_flipper)

        setupObservers()

        return top
    }

    private fun toEditMode() {
        bodyView.visibility = View.GONE
        bodyEdit.visibility = View.VISIBLE
    }

    private fun toViewMode() {
        bodyEdit.visibility = View.GONE

        bodyView.setRawText(bodyEdit.text)

        ImageLoader.loadImages(bodyView)

        bodyView.visibility = View.VISIBLE

        bodyView.requestFocus()

        ActivityUtils.closeSoftKeyboard(activity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, view, savedInstanceState)
        super.onViewCreated(view, savedInstanceState)

        /*
         * Metadata folding
         */

        fragment_note_metadata_header.setOnClickListener {
            val isFolded = fragment_note_metadata.visibility != View.VISIBLE
            setMetadataFoldState(!isFolded)
            AppPreferences.noteMetadataFolded(context, !isFolded)
        }

        setMetadataFoldState(AppPreferences.noteMetadataFolded(context))
    }

    private fun setMetadataFoldState(isFolded: Boolean) {
        fragment_note_metadata.visibility = visibleOrGone(!isFolded)
        fragment_note_metadata_header_up_icon.visibility = visibleOrGone(!isFolded)
        fragment_note_metadata_header_down_icon.visibility = visibleOrGone(isFolded)
    }

    private fun visibleOrGone(visible: Boolean) = if (visible) View.VISIBLE else View.GONE

    private fun setupObservers() {
        viewModel.noteDeletedEvent.observeSingle(viewLifecycleOwner, Observer { count ->
            handleNoteDeleted(count ?: 0)
        })

        viewModel.bookChangeRequestEvent.observeSingle(viewLifecycleOwner, Observer { books ->
            if (books != null) {
                handleNoteBookChangeRequest(books)
            }
        })

        viewModel.errorEvent.observeSingle(viewLifecycleOwner, Observer { error ->
            if (error != null) {
                CommonActivity.showSnackbar(context, (error.cause ?: error).localizedMessage)
            }
        })
    }

    private fun handleNoteDeleted(count: Int) {
        (activity as? CommonActivity)?.let {
            it.popBackStackAndCloseKeyboard()

            val message = if (count == 0) {
                resources.getString(R.string.no_notes_deleted)
            } else {
                resources.getQuantityString(R.plurals.notes_deleted, count, count)
            }

            it.showSnackbar(message)
        }
    }

    private fun savePayloadToBundle(outState: Bundle) {
        notePayload?.let {
            outState.putParcelable("payload", it)
        }
    }

    private fun restorePayloadFromBundle(savedInstanceState: Bundle) {
        notePayload = savedInstanceState.getParcelable("payload") as? NotePayload
    }

    private fun updateViewsFromPayload(payload: NotePayload) {
        setStateView(payload.state)

        setPriorityView(payload.priority)

        /* Title. */
        title.setText(payload.title)

        /* Tags. */
        if (!payload.tags.isEmpty()) {
            tagsView.setText(TextUtils.join(" ", payload.tags))
        } else {
            tagsView.text = null
        }

        /* Times. */
        updateTimestampView(TimeType.SCHEDULED, OrgRange.parseOrNull(payload.scheduled))
        updateTimestampView(TimeType.DEADLINE, OrgRange.parseOrNull(payload.deadline))
        updateTimestampView(TimeType.CLOSED, OrgRange.parseOrNull(payload.closed))

        /* Properties. */
        propertiesContainer.removeAllViews()
        if (!payload.properties.isEmpty()) {
            val properties = OrgMapper.toOrgProperties(payload.properties)
            for (name in properties.keys) {
                val value = properties[name]
                addPropertyToList(name, value)
            }
        }
        addPropertyToList(null, null)

        /* Content. */

        bodyEdit.setText(payload.content)

        bodyView.setRawText(payload.content ?: "")

        ImageLoader.loadImages(bodyView)
    }

    private fun addPropertyToList(propName: String?, propValue: String?) {
        View.inflate(activity, R.layout.fragment_note_property, propertiesContainer)

        val propView = lastProperty()

        val name = propView.findViewById<EditText>(R.id.name)
        val value = propView.findViewById<EditText>(R.id.value)
        val delete = propView.findViewById<View>(R.id.delete)

        if (propName != null && propValue != null) { // Existing property
            name.setText(propName)
            value.setText(propValue)
        }

        delete.setOnClickListener {
            if (isOnlyProperty(propView) || isLastProperty(propView)) {
                name.text = null
                value.text = null
            } else {
                propertiesContainer.removeView(propView)
            }
        }

        /*
         * Add new empty property if last one is being edited.
         */
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (isLastProperty(propView) && !TextUtils.isEmpty(s)) {
                    addPropertyToList(null, null)
                }
            }
        }

        name.addTextChangedListener(textWatcher)
        value.addTextChangedListener(textWatcher)
    }

    private fun isLastProperty(view: View): Boolean {
        return lastProperty() === view
    }

    private fun isOnlyProperty(view: View): Boolean {
        return propertiesContainer.childCount == 1 && propertiesContainer.getChildAt(0) === view
    }

    private fun lastProperty(): ViewGroup {
        return propertiesContainer.getChildAt(propertiesContainer.childCount - 1) as ViewGroup
    }

    private fun updatePayloadFromViews() {
        val properties = LinkedHashMap<String, String>()

        for (i in 0 until propertiesContainer.childCount) {
            val property = propertiesContainer.getChildAt(i)

            val name = (property.findViewById<View>(R.id.name) as TextView).text
            val value = (property.findViewById<View>(R.id.value) as TextView).text

            if (!TextUtils.isEmpty(name)) { // Ignore property with no name
                properties[name.toString()] = value.toString()
            }
        }

        notePayload?.let { payload ->
            val tags = Arrays.asList(*tagsView.text.toString().split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())

            notePayload = payload.copy(
                    // Replace new lines with spaces, in case multi-line text has been pasted
                    title = title.text.toString().replace("\n".toRegex(), " ").trim { it <= ' ' },
                    content = bodyEdit.text.toString(),
                    state = if (TextUtils.isEmpty(state.text)) null else state.text.toString(),
                    priority = if (TextUtils.isEmpty(priority.text)) null else priority.text.toString(),
                    scheduled = payload.scheduled,
                    deadline = payload.deadline,
                    closed = payload.closed,
                    tags = tags,
                    properties = properties
            )
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onActivityCreated(savedInstanceState)

        viewModel.noteDetailsDataEvent.observeSingle(viewLifecycleOwner, Observer { data ->
            book = data.book

            book?.let {
                locationView.text = BookUtils.getFragmentTitleForBook(it.book)
                locationButtonView.text = BookUtils.getFragmentTitleForBook(it.book)
            }

            if (mIsNew) { /* Creating new note. */
                notePayload = NoteBuilder.newPayload(context!!, mInitialTitle ?: "", mInitialContent)

                mViewFlipper.displayedChild = 0

                editSwitch.isChecked = true

                /* Open keyboard for new notes, unless fragment was given
                 * some initial values (for example from ShareActivity).
                 */
                if (TextUtils.isEmpty(mInitialTitle) && TextUtils.isEmpty(mInitialContent)) {
                    ActivityUtils.openSoftKeyboardWithDelay(activity, title)
                }

            } else { /* Get existing note from database. */
                notePayload = dataRepository.getNotePayload(noteId)

                if (notePayload != null) {
                    // If there is no content, start in edit mode
                    if (notePayload?.content == null) {
                        editSwitch.isChecked = true
                    }

                    mViewFlipper.displayedChild = 0

                } else {
                    mViewFlipper.displayedChild = 1
                }
            }

            /* Get current values from saved Bundle and populate all views. */
            if (savedInstanceState != null) {
                restorePayloadFromBundle(savedInstanceState)
            }

            /* Update views from note. */
            notePayload?.let { payload ->
                updateViewsFromPayload(payload)
            }

            /* Store the hash value of original note. */
            arguments?.apply {
                notePayload?.let { payload ->
                    if (!containsKey(ARG_ORIGINAL_NOTE_HASH)) {
                        putLong(ARG_ORIGINAL_NOTE_HASH, notePayloadHash(payload))
                    }
                }
            }

            /* Refresh action bar items (hide or display, depending on if book is loaded. */
            activity?.invalidateOptionsMenu()

            announceChangesToActivity()
        })

        viewModel.loadData(mBookId, noteId)

    }

    /**
     * Set adapter for tags view for auto-complete.
     */
    private fun setupTagsViewAdapter() {
        viewModel.tags.observe(viewLifecycleOwner, Observer { tags ->
            context?.let {
                val adapter = ArrayAdapter(it, R.layout.dropdown_item, tags)
                tagsView.setAdapter(adapter)
                tagsView.setTokenizer(SpaceTokenizer())
            }
        })
    }

    override fun onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onResume()

        setMetadataVisibility()
    }

    private fun announceChangesToActivity() {
        sharedMainActivityViewModel.setFragment(
                FRAGMENT_TAG,
                BookUtils.getFragmentTitleForBook(book?.book),
                BookUtils.getFragmentSubtitleForBook(context, book?.book),
                0)
    }

    override fun onPause() {
        super.onPause()

        if (dialog != null) {
            dialog?.dismiss()
            dialog = null
        }
    }

    override fun onDestroyView() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onDestroyView()

        mViewFlipper.displayedChild = 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, outState)
        super.onSaveInstanceState(outState)

        updatePayloadFromViews()

        savePayloadToBundle(outState)
    }

    override fun onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onDetach()

        listener = null
    }

    /**
     * Hash used to detect note modifications.
     * TODO: Avoid generating org
     */
    private fun notePayloadHash(payload: NotePayload): Long {
        val head = OrgMapper.toOrgHead(payload)

        val parserWriter = OrgParserWriter()
        val str = parserWriter.whiteSpacedHead(head, 1, false)

        return MiscUtils.sha1(str)
    }

    private enum class TimeType {
        SCHEDULED, DEADLINE, CLOSED, CLOCKED
    }

    private fun updateTimestampView(timeType: TimeType, range: OrgRange?) {
        when (timeType) {
            TimeType.SCHEDULED -> if (range != null) {
                scheduledButton.text = mUserTimeFormatter.formatAll(range)
            } else {
                scheduledButton.text = null
            }

            TimeType.DEADLINE -> if (range != null) {
                deadlineButton.text = mUserTimeFormatter.formatAll(range)
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "deadline button set to ${deadlineButton.text}")
            } else {
                deadlineButton.text = null
            }

            TimeType.CLOSED ->
                /*
                 * Do not display CLOSED button if it's not set.
                 * It will be updated on state change.
                 */
                if (range != null) {
                    closedText.text = mUserTimeFormatter.formatAll(range)
                    closedTimeContainer.visibility = View.VISIBLE
                } else {
                    closedTimeContainer.visibility = View.GONE
                }

            else -> { }
        }
    }

    private fun handleNoteBookChangeRequest(books: List<BookView>) {
        val bookNames = books.map { it.book.name }.toTypedArray()

        val selected = getSelectedBook(books, mBookId)

        dialog = AlertDialog.Builder(context)
                .setTitle(R.string.state)
                .setSingleChoiceItems(bookNames, selected) { dialog, which ->
                    val book = books[which]

                    setBook(book)

                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    override fun onClick(view: View) {
        var f: DialogFragment? = null

        when (view.id) {
            R.id.fragment_note_location_button -> {
                viewModel.requestNoteBookChange()
            }

            R.id.fragment_note_state_button -> {
                val states = NoteStates.fromPreferences(context!!)

                val keywords = states.array

                val currentState = if (!TextUtils.isEmpty(state.text)) {
                    states.indexOf(state.text.toString())
                } else {
                    -1
                }

                dialog = AlertDialog.Builder(context)
                        .setTitle(R.string.state)
                        .setSingleChoiceItems(keywords, currentState) { dialog, which ->
                            // On state change - update state and timestamps
                            setState(states[which])
                            dialog.dismiss()
                        }
                        .setNeutralButton(R.string.clear) { _, _ ->
                            // On state change - update state and timestamps
                            setState(null)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
            }

            R.id.fragment_note_priority_button -> {
                val priorities = NotePriorities.fromPreferences(context!!)

                val keywords = priorities.array

                var currentPriority = -1
                if (!TextUtils.isEmpty(priority.text)) {
                    currentPriority = priorities.indexOf(priority.text.toString())
                }

                dialog = AlertDialog.Builder(context)
                        .setTitle(R.string.priority)
                        .setSingleChoiceItems(keywords, currentPriority) { dialog, which ->
                            setPriorityView(priorities[which])
                            dialog.dismiss()
                        }
                        .setNeutralButton(R.string.clear) { _, _ -> setPriorityView(null) }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
            }

            /* Setting scheduled time. */
            R.id.fragment_note_scheduled_button ->
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_scheduled_button,
                        R.string.schedule,
                        0, // Unused
                        OrgRange.parseOrNull(notePayload?.scheduled)?.startTime)

            /* Setting deadline time. */
            R.id.fragment_note_deadline_button ->
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_deadline_button,
                        R.string.deadline,
                        0, // Unused
                        OrgRange.parseOrNull(notePayload?.deadline)?.startTime)

            /* Setting closed time. */
            R.id.fragment_note_closed_edit_text ->
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_closed_edit_text,
                        R.string.closed,
                        0, // Unused
                        OrgRange.parseOrNull(notePayload?.closed)?.startTime)
        }

        f?.show(childFragmentManager, TimestampDialogFragment.FRAGMENT_TAG)
    }

    private fun getSelectedBook(books: List<BookView>, bookId: Long?): Int {
        var selected = -1

        if (bookId != null) {
            for (i in books.indices) {
                if (books[i].book.id == bookId) {
                    selected = i
                    break
                }
            }

        } else {
            val defaultBookName = AppPreferences.shareNotebook(context)
            for (i in books.indices) {
                if (defaultBookName == books[i].book.name) {
                    selected = i
                    break
                }
            }
        }

        return selected
    }

    override fun onDateTimeSet(id: Int, noteIds: TreeSet<Long>, time: OrgDateTime) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id, R.id.fragment_note_deadline_button, noteIds, time, notePayload)

        notePayload?.let { payload ->
            val range = OrgRange(time)

            when (id) {
                R.id.fragment_note_scheduled_button -> {
                    updateTimestampView(TimeType.SCHEDULED, range)
                    notePayload = payload.copy(scheduled = range.toString())
                }

                R.id.fragment_note_deadline_button -> {
                    updateTimestampView(TimeType.DEADLINE, range)
                    notePayload = payload.copy(deadline = range.toString())
                }

                R.id.fragment_note_closed_edit_text -> {
                    updateTimestampView(TimeType.CLOSED, range)
                    notePayload = payload.copy(closed = range.toString())
                }
            }
        }
    }

    override fun onDateTimeCleared(id: Int, noteIds: TreeSet<Long>) {
        notePayload?.let { payload ->
            when (id) {
                R.id.fragment_note_scheduled_button -> {
                    updateTimestampView(TimeType.SCHEDULED, null)
                    notePayload = payload.copy(scheduled = null)
                }

                R.id.fragment_note_deadline_button -> {
                    updateTimestampView(TimeType.DEADLINE, null)
                    notePayload = payload.copy(deadline = null)
                }

                R.id.fragment_note_closed_edit_text -> {
                    updateTimestampView(TimeType.CLOSED, null)
                    notePayload = payload.copy(closed = null)
                }
            }
        }
    }

    override/* TimestampDialog */ fun onDateTimeAborted(id: Int, noteIds: TreeSet<Long>) {}

    /*
     * Options Menu.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater)

        inflater.inflate(R.menu.note_actions, menu)

        /* Remove search item. */
        menu.removeItem(R.id.activity_action_search)

        if (notePayload == null) { // Displaying non-existent note.
            menu.removeItem(R.id.close)
            menu.removeItem(R.id.done)
            menu.removeItem(R.id.metadata)
            menu.removeItem(R.id.delete)

        } else {
            when (AppPreferences.noteMetadataVisibility(context)) {
                "selected" -> menu.findItem(R.id.metadata_show_selected).isChecked = true
                else -> menu.findItem(R.id.metadata_show_all).isChecked = true
            }

            menu.findItem(R.id.metadata_always_show_set).isChecked =
                    AppPreferences.alwaysShowSetNoteMetadata(context)
        }

        /* Newly created note cannot be deleted. */
        if (mIsNew) {
            menu.removeItem(R.id.delete)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item)

        when (item.itemId) {
            R.id.done -> {
                save()
                return true
            }

            R.id.close -> {
                cancelWithConfirmation()
                return true
            }

            R.id.metadata_show_all -> {
                item.isChecked = true
                AppPreferences.noteMetadataVisibility(context, "all")
                setMetadataVisibility()
                return true
            }

            R.id.metadata_show_selected -> {
                item.isChecked = true
                AppPreferences.noteMetadataVisibility(context, "selected")
                setMetadataVisibility()
                return true
            }

            R.id.metadata_always_show_set -> {
                item.isChecked = !item.isChecked
                AppPreferences.alwaysShowSetNoteMetadata(context, item.isChecked)
                setMetadataVisibility()
                return true
            }

            R.id.delete -> {
                delete()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setMetadataVisibility() {
        setMetadataVisibility(
                "tags",
                tagsContainer,
                !TextUtils.isEmpty(tagsView.text))

        setMetadataVisibility(
                "state",
                stateContainer,
                !TextUtils.isEmpty(state.text))

        setMetadataVisibility(
                "priority",
                priorityContainer,
                !TextUtils.isEmpty(priority.text))

        setMetadataVisibility(
                "scheduled_time",
                scheduledTimeContainer,
                !TextUtils.isEmpty(scheduledButton.text))

        setMetadataVisibility(
                "deadline_time",
                deadlineTimeContainer,
                !TextUtils.isEmpty(deadlineButton.text))

        setMetadataVisibility(
                "properties",
                propertiesContainer,
                propertiesContainer.childCount > 1)
    }

    private fun setMetadataVisibility(name: String?, container: View, isSet: Boolean) {
        val context = context

        if (context != null) {
            val visibility = AppPreferences.noteMetadataVisibility(context)
            val selectedMetadata = AppPreferences.selectedNoteMetadata(context)
            val alwaysShowSet = AppPreferences.alwaysShowSetNoteMetadata(context)

            val isVisible = ("all" == visibility
                    || "none" == visibility // Not used anymore
                    || "selected" == visibility && name != null && selectedMetadata.contains(name)
                    || alwaysShowSet && isSet)

            container.visibility = if (isVisible) View.VISIBLE else View.GONE
        }
    }

    private fun delete() {
        dialog = AlertDialog.Builder(context)
                .setTitle(R.string.delete_note)
                .setMessage(R.string.delete_note_and_all_subnotes)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deleteNote(mBookId, noteId)
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
    }

    fun cancelWithConfirmation() {
        if (!isAskingForConfirmationForModifiedNote()) {
            cancel()
        }
    }

    /* It's possible that note does not exist
     * if it has been deleted and the user went back to it.
     */
    fun isAskingForConfirmationForModifiedNote(): Boolean {
        updatePayloadFromViews()

        val payload = notePayload

        return if (payload != null && isNoteModified(payload)) {
            dialog = AlertDialog.Builder(context)
                    .setTitle(R.string.note_has_been_modified)
                    .setMessage(R.string.discard_or_save_changes)
                    .setPositiveButton(R.string.save) { _, _ -> save() }
                    .setNegativeButton(R.string.discard) { _, _ -> cancel() }
                    .setNeutralButton(R.string.cancel, null)
                    .show()
            true

        } else {
            false
        }
    }

    private fun cancel() {
        listener?.onNoteCancelRequest()
    }

    private fun save() {
        /* Make sure notebook is set. */
        if (mBookId == 0L) {
            (activity as? CommonActivity)?.showSnackbar(R.string.note_book_not_set)
            return
        }

        updatePayloadFromViews()

        if (isTitleValid()) {
            val payload = notePayload

            if (mIsNew) { // New note
                val notePlace = if (place != Place.UNSPECIFIED)
                    NotePlace(mBookId, noteId, place)
                else
                    NotePlace(mBookId)

                if (payload != null) {
                    listener?.onNoteCreateRequest(payload, notePlace)
                }

            } else { // Existing note
                if (payload != null && isNoteModified(payload)) {
                    listener?.onNoteUpdateRequest(payload, noteId)
                } else {
                    listener?.onNoteCancelRequest()
                }
            }
        }
    }

    private fun isTitleValid(): Boolean {
        return if (TextUtils.isEmpty(notePayload?.title)) {
            CommonActivity.showSnackbar(context, getString(R.string.title_can_not_be_empty))
            false
        } else {
            true
        }
    }

    private fun isNoteModified(payload: NotePayload): Boolean {
        val currentHash = notePayloadHash(payload)
        val originalHash = arguments?.getLong(ARG_ORIGINAL_NOTE_HASH)

        return currentHash != originalHash
    }

    /**
     * Updates current book for this note. Only makes sense for new notes.
     * TODO: Should be setPosition and allow filing under specific note
     */
    private fun setBook(newBook: BookView) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, book)

        book = newBook
        mBookId = newBook.book.id

        val title = BookUtils.getFragmentTitleForBook(book?.book)
        locationView.text = title
        locationButtonView.text = title

        arguments?.putLong(ARG_BOOK_ID, newBook.book.id)
    }

    /**
     * Update state, timestamps, last-repeat and logbook.
     */
    private fun setState(state: String?) {
        updatePayloadFromViews()

        notePayload?.let { payload ->
            notePayload = NoteBuilder.withState(context!!, payload, state).also {
                updateViewsFromPayload(it)
            }
        }
    }

    private fun setStateView(state: String?) {
        if (state == null || NoteStates.NO_STATE_KEYWORD == state) {
            this.state.text = null
        } else {
            this.state.text = state
        }
    }

    private fun setPriorityView(priority: String?) {
        this.priority.text = priority
    }

    /**
     * Mark note's book in the drawer.
     */
    override fun getCurrentDrawerItemId(): String {
        return BookFragment.getDrawerItemId(mBookId)
    }

    interface Listener {
        fun onNoteCreateRequest(notePayload: NotePayload, notePlace: NotePlace)
        fun onNoteUpdateRequest(notePayload: NotePayload, noteId: Long)
        fun onNoteCancelRequest()
    }

    companion object {

        private val TAG = NoteFragment::class.java.name

        /** Name used for [android.app.FragmentManager].  */
        @JvmField
        val FRAGMENT_TAG: String = NoteFragment::class.java.name

        private const val ARG_ORIGINAL_NOTE_HASH = "original_note_hash"

        private const val ARG_IS_NEW = "is_new"
        private const val ARG_BOOK_ID = "book_id"
        private const val ARG_NOTE_ID = "note_id"
        private const val ARG_PLACE = "place"
        private const val ARG_TITLE = "title"
        private const val ARG_CONTENT = "content"

        @JvmStatic
        fun forSharedNote(bookId: Long, title: String?, content: String?): NoteFragment {
            return getInstance(true, bookId, 0, Place.UNSPECIFIED, title, content)
        }

        @JvmStatic
        fun forBook(isNew: Boolean, bookId: Long, noteId: Long, place: Place): NoteFragment {
            return getInstance(isNew, bookId, noteId, place, null, null)
        }

        private fun getInstance(
                isNew: Boolean,
                bookId: Long,
                noteId: Long,
                place: Place,
                initialTitle: String?,
                initialContent: String?): NoteFragment {

            val fragment = NoteFragment()

            val args = Bundle()

            args.putBoolean(ARG_IS_NEW, isNew)

            args.putLong(ARG_BOOK_ID, bookId)

            if (noteId > 0) {
                args.putLong(ARG_NOTE_ID, noteId)
            }

            args.putString(ARG_PLACE, place.toString())

            if (initialTitle != null) {
                args.putString(ARG_TITLE, initialTitle)
            }

            if (initialContent != null) {
                args.putString(ARG_CONTENT, initialContent)
            }

            fragment.arguments = args

            return fragment
        }
    }
}
