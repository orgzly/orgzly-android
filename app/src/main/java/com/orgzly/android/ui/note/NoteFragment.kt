package com.orgzly.android.ui.note

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.BookUtils
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.db.entity.Note
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.*
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.book.BookFragment
import com.orgzly.android.ui.share.ShareActivity
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.SpaceTokenizer
import com.orgzly.android.util.UserTimeFormatter
import com.orgzly.databinding.FragmentNoteBinding
import com.orgzly.org.OrgProperties
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange
import dagger.android.support.DaggerFragment
import java.util.*
import javax.inject.Inject

/**
 * Note editor.
 */
class NoteFragment : DaggerFragment(), View.OnClickListener, TimestampDialogFragment.OnDateTimeSetListener, DrawerItem {

    private lateinit var binding: FragmentNoteBinding

    /** Could be 0 if new note is being created. */
    var noteId: Long = 0

    /** Relative location, used for new notes. */
    private var place: Place? = null

    /** Initial title, used for when sharing. */
    private var initialTitle: String? = null

    /** Initial content, used for when sharing. */
    private var initialContent: String? = null

    @Inject
    internal lateinit var dataRepository: DataRepository

    private var listener: Listener? = null

    private lateinit var viewModel: NoteViewModel

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
            /* Book ID must exist. */
            if (!containsKey(ARG_BOOK_ID)) {
                throw IllegalArgumentException(NoteFragment::class.java.simpleName + " requires " + ARG_BOOK_ID + " argument passed")
            }

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

            initialTitle = getString(ARG_TITLE)
            initialContent = getString(ARG_CONTENT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onCreate(savedInstanceState)

        sharedMainActivityViewModel = activity?.let {
            ViewModelProviders.of(it).get(SharedMainActivityViewModel::class.java)
        } ?: throw IllegalStateException("No Activity")

        val factory = NoteViewModelFactory.getInstance(
                dataRepository,
                arguments?.getLong(ARG_BOOK_ID) ?: 0,
                noteId,
                place,
                initialTitle,
                initialContent)

        viewModel = ViewModelProviders.of(this, factory).get(NoteViewModel::class.java)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState)

        binding = FragmentNoteBinding.inflate(inflater, container, false)

        /*
         * Not working when done in XML.
         * We want imeOptions="actionDone", so we can't use textMultiLine.
         */
        binding.fragmentNoteTitle.apply {
            setHorizontallyScrolling(false)

            maxLines = Integer.MAX_VALUE

            // Keyboard's action button pressed
            setOnEditorActionListener { _, _, _ ->
                save()
                true
            }
        }

        binding.fragmentNoteBreadcrumbsText.movementMethod = LinkMovementMethod.getInstance()

        if (activity is ShareActivity) {
            binding.fragmentNoteBreadcrumbs.visibility = View.GONE

            binding.fragmentNoteLocationButton.let {
                it.visibility = View.VISIBLE
                it.setOnClickListener(this)
            }
        } else {
            binding.fragmentNoteBreadcrumbs.visibility = View.VISIBLE
            binding.fragmentNoteLocationButton.visibility = View.GONE
        }


        /* Hint causes minimum width - when tags' width is smaller then hint's, there is empty space. */
        binding.fragmentNoteTags.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.fragmentNoteTags.apply {
                    if (!TextUtils.isEmpty(text.toString())) {
                        hint = ""
                    } else {
                        setHint(R.string.fragment_note_tags_hint)
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        setupTagsViewAdapter()

        binding.fragmentNotePriorityButton.setOnClickListener(this)

        binding.fragmentNoteStateButton.setOnClickListener(this)

        binding.fragmentNoteScheduledButton.setOnClickListener(this)

        binding.fragmentNoteDeadlineButton.setOnClickListener(this)

        binding.fragmentNoteClosedEditText.setOnClickListener(this)

        binding.bodyView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                // Update bodyEdit text when checkboxes are clicked
                val text = binding.bodyView.getRawText()
                binding.bodyEdit.setText(text)
            }
        })

        if (activity != null && AppPreferences.isFontMonospaced(context)) {
            binding.bodyEdit.typeface = Typeface.MONOSPACE
            binding.bodyView.typeface = Typeface.MONOSPACE
        }

        setupObservers()

        return binding.root
    }


    private fun toEditMode() {
        binding.fragmentNoteModeText.setText(R.string.note_content_finish_editing)

        binding.bodyView.visibility = View.GONE
        binding.bodyEdit.visibility = View.VISIBLE
    }

    private fun toViewMode() {
        binding.fragmentNoteModeText.setText(R.string.note_content_start_editing)

        binding.bodyEdit.visibility = View.GONE

        binding.bodyView.setRawText(binding.bodyEdit.text.toString())

        ImageLoader.loadImages(binding.bodyView)

        binding.bodyView.visibility = View.VISIBLE

        binding.fragmentNoteContainer.requestFocus()

        ActivityUtils.closeSoftKeyboard(activity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, view, savedInstanceState)
        super.onViewCreated(view, savedInstanceState)

        /*
         * Metadata folding
         */

        binding.fragmentNoteMetadataHeader.setOnClickListener {
            val isFolded = binding.fragmentNoteMetadata.visibility != View.VISIBLE
            setMetadataFoldState(!isFolded)
            AppPreferences.noteMetadataFolded(context, !isFolded)
        }

        setMetadataFoldState(AppPreferences.noteMetadataFolded(context))

        binding.fragmentNoteModeToggleButton.setOnClickListener {
            viewModel.toggleViewEditMode()
        }
    }

    private fun setMetadataFoldState(isFolded: Boolean) {
        binding.fragmentNoteMetadata.visibility = visibleOrGone(!isFolded)
        binding.fragmentNoteMetadataHeaderUpIcon.visibility = visibleOrGone(!isFolded)
        binding.fragmentNoteMetadataHeaderDownIcon.visibility = visibleOrGone(isFolded)
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

        viewModel.viewEditMode.observe(viewLifecycleOwner, Observer { viewEditMode ->
            when (viewEditMode) {
                NoteViewModel.ViewEditMode.VIEW ->
                    toViewMode()

                NoteViewModel.ViewEditMode.EDIT ->
                    toEditMode()

                NoteViewModel.ViewEditMode.EDIT_WITH_KEYBOARD -> {
                    toEditMode()
                    ActivityUtils.openSoftKeyboard(activity, binding.bodyEdit, binding.fragmentNoteContainer)
                }

                null -> { }
            }
        })

        viewModel.noteCreatedEvent.observe(viewLifecycleOwner, Observer { note ->
            listener?.onNoteCreated(note)
        })

        viewModel.noteUpdatedEvent.observe(viewLifecycleOwner, Observer { note ->
            listener?.onNoteUpdated(note)
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

    private fun updateViewsFromPayload() {
        val payload = viewModel.notePayload ?: return

        setStateView(payload.state)

        setPriorityView(payload.priority)

        /* Title. */
        binding.fragmentNoteTitle.setText(payload.title)

        /* Tags. */
        if (!payload.tags.isEmpty()) {
            binding.fragmentNoteTags.setText(TextUtils.join(" ", payload.tags))
        } else {
            binding.fragmentNoteTags.text = null
        }

        /* Times. */
        updateTimestampView(TimeType.SCHEDULED, OrgRange.parseOrNull(payload.scheduled))
        updateTimestampView(TimeType.DEADLINE, OrgRange.parseOrNull(payload.deadline))
        updateTimestampView(TimeType.CLOSED, OrgRange.parseOrNull(payload.closed))

        /* Properties. */
        binding.fragmentNotePropertiesContainer.removeAllViews()
        for (property in payload.properties.all) {
            addPropertyToList(property.name, property.value)
        }
        addPropertyToList(null, null)

        /* Content. */

        binding.bodyEdit.setText(payload.content)

        binding.bodyView.setRawText(payload.content ?: "")

        ImageLoader.loadImages(binding.bodyView)
    }

    private fun addPropertyToList(propName: String?, propValue: String?) {
        View.inflate(activity, R.layout.fragment_note_property, binding.fragmentNotePropertiesContainer)

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
                binding.fragmentNotePropertiesContainer.removeView(propView)
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
        return binding.fragmentNotePropertiesContainer.childCount == 1
                && binding.fragmentNotePropertiesContainer.getChildAt(0) === view
    }

    private fun lastProperty(): ViewGroup {
        return binding.fragmentNotePropertiesContainer
                .getChildAt(binding.fragmentNotePropertiesContainer.childCount - 1) as ViewGroup
    }

    private fun updatePayloadFromViews() {
        val properties = OrgProperties()

        for (i in 0 until binding.fragmentNotePropertiesContainer.childCount) {
            val property = binding.fragmentNotePropertiesContainer.getChildAt(i)

            val name = (property.findViewById<View>(R.id.name) as TextView).text
            val value = (property.findViewById<View>(R.id.value) as TextView).text

            if (!TextUtils.isEmpty(name)) { // Ignore property with no name
                properties.put(name.toString(), value.toString())
            }
        }

        // Replace new lines with spaces, in case multi-line text has been pasted
        val title = binding.fragmentNoteTitle.text.toString().replace("\n".toRegex(), " ").trim { it <= ' ' }

        val content = binding.bodyEdit.text.toString()

        // TODO: Create a function (extension?) for this
        val state = if (TextUtils.isEmpty(binding.fragmentNoteStateButton.text))
            null
        else
            binding.fragmentNoteStateButton.text.toString()

        val priority = if (TextUtils.isEmpty(binding.fragmentNotePriorityButton.text))
            null
        else
            binding.fragmentNotePriorityButton.text.toString()

        val tags = binding.fragmentNoteTags.text.toString()
                .split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }

        viewModel.updatePayload(title, content, state, priority, tags, properties)

        // Planning times are updated in onDateTimeSet
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onActivityCreated(savedInstanceState)

        viewModel.noteDetailsDataEvent.observeSingle(viewLifecycleOwner, Observer { data ->
            data.book?.let {
                val bookTitle = BookUtils.getFragmentTitleForBook(it.book)

                val breadcrumbs = Breadcrumbs().apply {
                    // Notebook
                    add(bookTitle, 0, if (noteId == 0L) null else fun() {
                        ActivityUtils.closeSoftKeyboard(activity)
                        viewModel.onBreadcrumbsBook(data)
                    })

                    // Ancestors
                    data.ancestors.forEach { ancestor ->
                        add(ancestor.title, 0) {
                            ActivityUtils.closeSoftKeyboard(activity)
                            viewModel.onBreadcrumbsNote(it.book.id, ancestor)
                        }
                    }
                }

                binding.fragmentNoteBreadcrumbsText.text = breadcrumbs.toCharSequence()

                binding.fragmentNoteLocationButton.text = bookTitle
            }

            if (viewModel.isNew()) { // Create new note
                binding.fragmentNoteViewFlipper.displayedChild = 0

                /* Open keyboard for new notes, unless fragment was given
                 * some initial values (for example from ShareActivity).
                 */
                if (TextUtils.isEmpty(initialTitle) && TextUtils.isEmpty(initialContent)) {
                    ActivityUtils.openSoftKeyboardWithDelay(activity, binding.fragmentNoteTitle)
                }

            } else { // Open existing note
                if (viewModel.notePayload != null) {
                    binding.fragmentNoteViewFlipper.displayedChild = 0
                } else {
                    binding.fragmentNoteViewFlipper.displayedChild = 1
                }
            }

            // Load payload from saved Bundle if available
            if (savedInstanceState != null) {
                viewModel.restorePayloadFromBundle(savedInstanceState)
            }

            updateViewsFromPayload()

            /* Refresh action bar items (hide or display, depending on if book is loaded. */
            activity?.invalidateOptionsMenu()

            announceChangesToActivity()
        })

        viewModel.loadData()
    }

    /**
     * Set adapter for tags view for auto-complete.
     */
    private fun setupTagsViewAdapter() {
        viewModel.tags.observe(viewLifecycleOwner, Observer { tags ->
            context?.let {
                val adapter = ArrayAdapter(it, R.layout.dropdown_item, tags)
                binding.fragmentNoteTags.setAdapter(adapter)
                binding.fragmentNoteTags.setTokenizer(SpaceTokenizer())
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
                viewModel.bookView.value?.book?.name,
                BookUtils.getError(context, viewModel.bookView.value?.book),
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

        binding.fragmentNoteViewFlipper.displayedChild = 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, outState)
        super.onSaveInstanceState(outState)

        updatePayloadFromViews()

        viewModel.savePayloadToBundle(outState)
    }

    override fun onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onDetach()

        listener = null
    }

    private enum class TimeType {
        SCHEDULED, DEADLINE, CLOSED, CLOCKED
    }

    private fun updateTimestampView(timeType: TimeType, range: OrgRange?) {
        when (timeType) {
            TimeType.SCHEDULED -> if (range != null) {
                binding.fragmentNoteScheduledButton.text = mUserTimeFormatter.formatAll(range)
            } else {
                binding.fragmentNoteScheduledButton.text = null
            }

            TimeType.DEADLINE -> if (range != null) {
                binding.fragmentNoteDeadlineButton.text = mUserTimeFormatter.formatAll(range)
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "deadline button set to ${binding.fragmentNoteDeadlineButton.text}")
            } else {
                binding.fragmentNoteDeadlineButton.text = null
            }

            TimeType.CLOSED ->
                /*
                 * Do not display CLOSED button if it's not set.
                 * It will be updated on state change.
                 */
                if (range != null) {
                    binding.fragmentNoteClosedEditText.text = mUserTimeFormatter.formatAll(range)
                    binding.fragmentNoteClosedTimeContainer.visibility = View.VISIBLE
                } else {
                    binding.fragmentNoteClosedTimeContainer.visibility = View.GONE
                }

            else -> { }
        }
    }

    private fun handleNoteBookChangeRequest(books: List<BookView>) {
        val bookNames = books.map { it.book.name }.toTypedArray()

        val selected = getSelectedBook(books, viewModel.bookId)

        dialog = AlertDialog.Builder(context)
                .setTitle(R.string.notebook)
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

                val currentState = if (!TextUtils.isEmpty(binding.fragmentNoteStateButton.text)) {
                    states.indexOf(binding.fragmentNoteStateButton.text.toString())
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
                if (!TextUtils.isEmpty(binding.fragmentNotePriorityButton.text)) {
                    currentPriority = priorities.indexOf(binding.fragmentNotePriorityButton.text.toString())
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
                        OrgRange.parseOrNull(viewModel.notePayload?.scheduled)?.startTime)

            /* Setting deadline time. */
            R.id.fragment_note_deadline_button ->
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_deadline_button,
                        R.string.deadline,
                        0, // Unused
                        OrgRange.parseOrNull(viewModel.notePayload?.deadline)?.startTime)

            /* Setting closed time. */
            R.id.fragment_note_closed_edit_text ->
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_closed_edit_text,
                        R.string.closed,
                        0, // Unused
                        OrgRange.parseOrNull(viewModel.notePayload?.closed)?.startTime)
        }

        f?.show(childFragmentManager, TimestampDialogFragment.FRAGMENT_TAG)
    }

    private fun getSelectedBook(books: List<BookView>, id: Long?): Int {
        var selected = -1

        if (id != null) {
            for (i in books.indices) {
                if (books[i].book.id == id) {
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
        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(
                    TAG, id, R.id.fragment_note_deadline_button, noteIds, time, viewModel.notePayload)

        viewModel.notePayload?.let { payload ->
            val range = OrgRange(time)

            when (id) {
                R.id.fragment_note_scheduled_button -> {
                    updateTimestampView(TimeType.SCHEDULED, range)
                    viewModel.updatePayloadScheduledTime(range)

                }

                R.id.fragment_note_deadline_button -> {
                    updateTimestampView(TimeType.DEADLINE, range)
                    viewModel.updatePayloadDeadlineTime(range)
                }

                R.id.fragment_note_closed_edit_text -> {
                    updateTimestampView(TimeType.CLOSED, range)
                    viewModel.updatePayloadClosedTime(range)
                }
            }
        }
    }

    override fun onDateTimeCleared(id: Int, noteIds: TreeSet<Long>) {
        viewModel.notePayload?.let { payload ->
            when (id) {
                R.id.fragment_note_scheduled_button -> {
                    updateTimestampView(TimeType.SCHEDULED, null)
                    viewModel.updatePayloadScheduledTime(null)
                }

                R.id.fragment_note_deadline_button -> {
                    updateTimestampView(TimeType.DEADLINE, null)
                    viewModel.updatePayloadDeadlineTime(null)
                }

                R.id.fragment_note_closed_edit_text -> {
                    updateTimestampView(TimeType.CLOSED, null)
                    viewModel.updatePayloadClosedTime(null)
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

        if (viewModel.notePayload == null) { // Displaying non-existent note.
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
        if (viewModel.isNew()) {
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
                binding.fragmentNoteTagsContainer,
                !TextUtils.isEmpty(binding.fragmentNoteTags.text))

        setMetadataVisibility(
                "state",
                binding.fragmentNoteStateContainer,
                !TextUtils.isEmpty(binding.fragmentNoteStateButton.text))

        setMetadataVisibility(
                "priority",
                binding.fragmentNotePriorityContainer,
                !TextUtils.isEmpty(binding.fragmentNotePriorityButton.text))

        setMetadataVisibility(
                "scheduled_time",
                binding.fragmentNoteScheduledTimeContainer,
                !TextUtils.isEmpty(binding.fragmentNoteScheduledButton.text))

        setMetadataVisibility(
                "deadline_time",
                binding.fragmentNoteDeadlineTimeContainer,
                !TextUtils.isEmpty(binding.fragmentNoteDeadlineButton.text))

        setMetadataVisibility(
                "properties",
                binding.fragmentNotePropertiesContainer,
                binding.fragmentNotePropertiesContainer.childCount > 1)
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
                    viewModel.deleteNote()
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
    }

    private fun cancelWithConfirmation() {
        if (!isAskingForConfirmationForModifiedNote()) {
            cancel()
        }
    }

    /* It's possible that note does not exist
     * if it has been deleted and the user went back to it.
     */
    fun isAskingForConfirmationForModifiedNote(): Boolean {
        updatePayloadFromViews()

        return if (viewModel.isNoteModified()) {
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
        listener?.onNoteCanceled()
    }

    private fun save() {
        /* Make sure notebook is set. */
        if (viewModel.bookId == 0L) {
            (activity as? CommonActivity)?.showSnackbar(R.string.note_book_not_set)
            return
        }

        updatePayloadFromViews()

        if (isTitleValid()) {
            if (viewModel.isNew()) { // New note
                viewModel.createNote()

            } else { // Existing note
                if (viewModel.isNoteModified()) {
                    viewModel.updateNote(noteId)
                } else {
                    listener?.onNoteCanceled()
                }
            }
        }
    }

    private fun isTitleValid(): Boolean {
        val title = viewModel.notePayload?.title

        return if (TextUtils.isEmpty(title)) {
            CommonActivity.showSnackbar(context, getString(R.string.title_can_not_be_empty))
            false
        } else {
            true
        }
    }

    /**
     * Updates current book for this note. Only makes sense for new notes.
     * TODO: Should be setPosition and allow filing under specific note
     */
    private fun setBook(newBook: BookView) {
        viewModel.setBook(newBook)

        val title = BookUtils.getFragmentTitleForBook(viewModel.bookView.value?.book)
        binding.fragmentNoteBreadcrumbsText.text = title
        binding.fragmentNoteLocationButton.text = title

        arguments?.putLong(ARG_BOOK_ID, newBook.book.id)
    }

    /**
     * Update state, timestamps, last-repeat and logbook.
     */
    private fun setState(state: String?) {
        updatePayloadFromViews()

        viewModel.updatePayloadState(state)

        updateViewsFromPayload()
    }

    private fun setStateView(state: String?) {
        if (state == null || NoteStates.NO_STATE_KEYWORD == state) {
            this.binding.fragmentNoteStateButton.text = null
        } else {
            this.binding.fragmentNoteStateButton.text = state
        }
    }

    private fun setPriorityView(priority: String?) {
        this.binding.fragmentNotePriorityButton.text = priority
    }

    /**
     * Mark note's book in the drawer.
     */
    override fun getCurrentDrawerItemId(): String {
        return BookFragment.getDrawerItemId(viewModel.bookId)
    }

    interface Listener {
        fun onNoteCreated(note: Note)
        fun onNoteUpdated(note: Note)
        fun onNoteCanceled()
    }

    companion object {

        private val TAG = NoteFragment::class.java.name

        /** Name used for [android.app.FragmentManager].  */
        @JvmField
        val FRAGMENT_TAG: String = NoteFragment::class.java.name

        private const val ARG_BOOK_ID = "book_id"
        private const val ARG_NOTE_ID = "note_id"
        private const val ARG_PLACE = "place"
        private const val ARG_TITLE = "title"
        private const val ARG_CONTENT = "content"

        @JvmStatic
        @JvmOverloads
        fun forNewNote(
                notePlace: NotePlace,
                initialTitle: String? = null,
                initialContent: String? = null): NoteFragment? {

            return if (notePlace.bookId > 0) {
                getInstance(
                        notePlace.bookId,
                        notePlace.noteId,
                        notePlace.place,
                        initialTitle,
                        initialContent)
            } else {
                Log.e(TAG, "Invalid book id ${notePlace.bookId}")
                null
            }
        }

        @JvmStatic
        fun forExistingNote(bookId: Long, noteId: Long): NoteFragment? {
            return if (bookId > 0 && noteId > 0) {
                getInstance(bookId, noteId)
            } else {
                Log.e(TAG, "Invalid book id $bookId or note id $noteId")
                null
            }
        }

        @JvmStatic
        private fun getInstance(
                bookId: Long,
                noteId: Long,
                place: Place? = null,
                initialTitle: String? = null,
                initialContent: String? = null): NoteFragment {

            val fragment = NoteFragment()

            val args = Bundle()

            args.putLong(ARG_BOOK_ID, bookId)

            if (noteId > 0) {
                args.putLong(ARG_NOTE_ID, noteId)
            }

            if (place != null) {
                args.putString(ARG_PLACE, place.toString())
            }

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
