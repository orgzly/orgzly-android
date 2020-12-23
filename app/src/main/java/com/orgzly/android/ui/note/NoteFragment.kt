package com.orgzly.android.ui.note

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.BookUtils
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.db.entity.Note
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.*
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.book.BookFragment
import com.orgzly.android.ui.share.ShareActivity
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.ui.util.removeBackgroundKeepPadding
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.SpaceTokenizer
import com.orgzly.android.util.UserTimeFormatter
import com.orgzly.databinding.FragmentNoteBinding
import com.orgzly.org.OrgProperties
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange
import java.util.*
import javax.inject.Inject

/**
 * Note editor.
 */
class NoteFragment : Fragment(), View.OnClickListener, TimestampDialogFragment.OnDateTimeSetListener, DrawerItem {

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
        super.onAttach(context)

        App.appComponent.inject(this)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, activity)

        listener = activity as Listener

        parseArguments()

        mUserTimeFormatter = UserTimeFormatter(context)
    }

    private fun parseArguments() {
        arguments?.apply {
            /* Book ID must exist. */
            require(containsKey(ARG_BOOK_ID)) {
                "${NoteFragment::class.java.simpleName} requires $ARG_BOOK_ID argument passed"
            }

            /* Note ID might or might not be passed - it depends if note is being edited or created. */
            if (containsKey(ARG_NOTE_ID)) {
                noteId = getLong(ARG_NOTE_ID)

                /* Note ID must be valid if it exists. */
                require(noteId > 0) {
                    "Note id is $noteId"
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
        super.onCreate(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        sharedMainActivityViewModel = ViewModelProviders.of(requireActivity())
                .get(SharedMainActivityViewModel::class.java)

        val factory = NoteViewModelFactory.getInstance(
                dataRepository,
                arguments?.getLong(ARG_BOOK_ID) ?: 0,
                noteId,
                place,
                initialTitle,
                initialContent)

        viewModel = ViewModelProviders.of(this, factory).get(NoteViewModel::class.java)

        setHasOptionsMenu(true)

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        })
    }

    private fun onBackPressed() {
        userCancel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        binding = FragmentNoteBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        /*
         * Not working when done in XML.
         * We want imeOptions="actionDone", so we can't use textMultiLine.
         */
        binding.fragmentNoteTitle.apply {
            setHorizontallyScrolling(false)

            maxLines = Integer.MAX_VALUE

            // Keyboard's action button pressed
            setOnEditorActionListener { _, _, _ ->
                userSave()
                true
            }
        }

        binding.fragmentNoteTitleView.apply {
            removeBackgroundKeepPadding()

            setOnFocusOrClickListener(View.OnClickListener {
                viewModel.editTitle()
            })
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

        binding.fragmentNoteTagsButton.setOnClickListener {
            binding.fragmentNoteTags.showDropDown()
        }

        setupTagsViewAdapter()

        binding.fragmentNotePriorityButton.setOnClickListener(this)
        binding.fragmentNotePriorityRemove.setOnClickListener(this)

        binding.fragmentNoteStateButton.setOnClickListener(this)
        binding.fragmentNoteStateRemove.setOnClickListener(this)

        binding.fragmentNoteScheduledButton.setOnClickListener(this)
        binding.fragmentNoteScheduledRemove.setOnClickListener(this)

        binding.fragmentNoteDeadlineButton.setOnClickListener(this)
        binding.fragmentNoteDeadlineRemove.setOnClickListener(this)

        binding.fragmentNoteClosedEditText.setOnClickListener(this)
        binding.fragmentNoteClosedRemove.setOnClickListener(this)

        binding.bodyView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                // Update bodyEdit text when checkboxes are clicked
                val text = binding.bodyView.getRawText()
                binding.bodyEdit.setText(text)
            }
        })

        binding.bodyView.apply {
            removeBackgroundKeepPadding()

            setOnFocusOrClickListener(View.OnClickListener {
                viewModel.editContent()
            })
        }

        if (activity != null && AppPreferences.isFontMonospaced(context)) {
            binding.bodyEdit.typeface = Typeface.MONOSPACE
            binding.bodyView.typeface = Typeface.MONOSPACE
        }

        setupObservers()

        /*
         * Metadata folding
         */

        binding.fragmentNoteMetadataHeader.setOnClickListener {
            val isFolded = binding.fragmentNoteMetadata.visibility != View.VISIBLE
            setMetadataFoldState(!isFolded)
            AppPreferences.noteMetadataFolded(context, !isFolded)
        }

        setMetadataFoldState(AppPreferences.noteMetadataFolded(context))

        /*
         * Content folding
         */

        binding.fragmentNoteContentHeader.setOnClickListener {
            isNoteContentFolded().not().let { isFolded ->
                if (isFolded && binding.bodyEdit.hasFocus()) {
                    ActivityUtils.closeSoftKeyboard(activity)
                }

                setContentFoldState(isFolded)
                AppPreferences.isNoteContentFolded(context, isFolded)
            }
        }

        setContentFoldState(AppPreferences.isNoteContentFolded(context))

    }

    private fun isNoteContentFolded(): Boolean {
        return binding.fragmentNoteContentViews.visibility != View.VISIBLE
    }

    private fun setContentFoldState(isFolded: Boolean) {
        binding.fragmentNoteContentViews.visibility = visibleOrGone(!isFolded)
        binding.fragmentNoteContentHeaderUpIcon.visibility = visibleOrGone(!isFolded)
        binding.fragmentNoteContentHeaderDownIcon.visibility = visibleOrGone(isFolded)
    }


    private fun setMetadataFoldState(isFolded: Boolean) {
        binding.fragmentNoteMetadata.visibility = visibleOrGone(!isFolded)
        binding.fragmentNoteMetadataHeaderUpIcon.visibility = visibleOrGone(!isFolded)
        binding.fragmentNoteMetadataHeaderDownIcon.visibility = visibleOrGone(isFolded)
    }

    private fun visibleOrGone(visible: Boolean) = if (visible) View.VISIBLE else View.GONE

    private fun setupObservers() {
        viewModel.noteCreatedEvent.observe(viewLifecycleOwner, Observer { note ->
            listener?.onNoteCreated(note)
        })

        viewModel.noteUpdatedEvent.observe(viewLifecycleOwner, Observer { note ->
            listener?.onNoteUpdated(note)
        })

        viewModel.noteDeletedEvent.observeSingle(viewLifecycleOwner, Observer { count ->
            (activity as? MainActivity)?.popBackStackAndCloseKeyboard()

            val message = if (count == 0) {
                resources.getString(R.string.no_notes_deleted)
            } else {
                resources.getQuantityString(R.plurals.notes_deleted, count, count)
            }

            showSnackbar(message)
        })

        viewModel.noteDeleteRequest.observeSingle(viewLifecycleOwner, Observer { count ->
            val question = resources.getQuantityString(
                    R.plurals.delete_note_or_notes_with_count_question, count, count)

            dialog = AlertDialog.Builder(context)
                    .setTitle(question)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.deleteNote()
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .show()

        })

        viewModel.bookChangeRequestEvent.observeSingle(viewLifecycleOwner, Observer { books ->
            if (books != null) {
                handleNoteBookChangeRequest(books)
            }
        })

        viewModel.viewEditMode.observe(viewLifecycleOwner, Observer { viewEditMode ->
            when (viewEditMode) {
                NoteViewModel.ViewEditMode.VIEW ->
                    toViewMode()

                NoteViewModel.ViewEditMode.EDIT -> {
                    toEditMode()
                }

                NoteViewModel.ViewEditMode.EDIT_TITLE_WITH_KEYBOARD -> {
                    toEditMode()
                    ActivityUtils.openSoftKeyboard(activity, binding.fragmentNoteTitle)
                }

                NoteViewModel.ViewEditMode.EDIT_CONTENT_WITH_KEYBOARD -> {
                    toEditMode()
                    ActivityUtils.openSoftKeyboard(activity, binding.bodyEdit)
                }

                null -> { }
            }

            // For updating view-edit switch
            activity?.invalidateOptionsMenu()

        })

        viewModel.errorEvent.observeSingle(viewLifecycleOwner, Observer { error ->
            showSnackbar((error.cause ?: error).localizedMessage)
        })

        viewModel.snackBarMessage.observeSingle(viewLifecycleOwner, Observer { resId ->
            showSnackbar(resId)
        })
    }

    private fun toEditMode() {
        binding.fragmentNoteTitleView.visibility = View.GONE
        binding.fragmentNoteTitle.visibility = View.VISIBLE

        binding.bodyView.visibility = View.GONE
        binding.bodyEdit.visibility = View.VISIBLE
    }

    private fun toViewMode() {
        ActivityUtils.closeSoftKeyboard(activity)

        binding.fragmentNoteTitle.visibility = View.GONE
        binding.fragmentNoteTitleView.setRawText(binding.fragmentNoteTitle.text.toString())
        binding.fragmentNoteTitleView.visibility = View.VISIBLE

        binding.bodyEdit.visibility = View.GONE

        binding.bodyView.setRawText(binding.bodyEdit.text.toString())

        ImageLoader.loadImages(binding.bodyView)

        binding.bodyView.visibility = View.VISIBLE
    }

    private fun updateViewsFromPayload() {
        val payload = viewModel.notePayload ?: return

        // State
        setStateView(payload.state)

        // Priority
        setPriorityView(payload.priority)

        // Title
        binding.fragmentNoteTitle.setText(payload.title)
        binding.fragmentNoteTitleView.setRawText(payload.title ?: "")

        // Tags
        if (!payload.tags.isEmpty()) {
            binding.fragmentNoteTags.setText(TextUtils.join(" ", payload.tags))
        } else {
            binding.fragmentNoteTags.text = null
        }

        // Times
        updateTimestampView(TimeType.SCHEDULED, OrgRange.parseOrNull(payload.scheduled))
        updateTimestampView(TimeType.DEADLINE, OrgRange.parseOrNull(payload.deadline))
        updateTimestampView(TimeType.CLOSED, OrgRange.parseOrNull(payload.closed))

        // Properties
        binding.fragmentNotePropertiesContainer.removeAllViews()
        for (property in payload.properties.all) {
            addPropertyToList(property.name, property.value)
        }
        addPropertyToList(null, null)

        // Content

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
        super.onActivityCreated(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        viewModel.noteDetailsDataEvent.observeSingle(viewLifecycleOwner, Observer { data ->
            data.book?.let {
                val bookTitle = BookUtils.getFragmentTitleForBook(it.book)

                val breadcrumbs = Breadcrumbs().apply {
                    // Notebook
                    add(bookTitle, 0, if (noteId == 0L) null else fun() {
                        userFollowBookBreadcrumb()
                    })

                    // Ancestors
                    data.ancestors.forEach { ancestor ->
                        add(ancestor.title, 0) {
                            userFollowNoteBreadcrumb(ancestor)
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
                    viewModel.editTitle(saveMode = false)
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

            setMetadataViewsVisibility()

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
        super.onResume()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
    }

    private fun announceChangesToActivity() {
//        sharedMainActivityViewModel.setFragment(
//                FRAGMENT_TAG,
//                viewModel.bookView.value?.book?.name,
//                BookUtils.getError(context, viewModel.bookView.value?.book),
//                0)

        sharedMainActivityViewModel.setFragment(FRAGMENT_TAG, null, null, 0)
    }

    override fun onPause() {
        super.onPause()

        dialog?.dismiss()
        dialog = null

        ActivityUtils.keepScreenOnClear(activity)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        binding.fragmentNoteViewFlipper.displayedChild = 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, outState)

        if (::binding.isInitialized) {
            updatePayloadFromViews()
        }

        viewModel.savePayloadToBundle(outState)
    }

    override fun onDetach() {
        super.onDetach()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        listener = null
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

            R.id.fragment_note_state_remove -> {
                setState(null)
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

            R.id.fragment_note_priority_remove -> {
                setPriorityView(null)
            }

            /* Setting scheduled time. */
            R.id.fragment_note_scheduled_button ->
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_scheduled_button,
                        TimeType.SCHEDULED,
                        emptySet(), // Unused
                        OrgRange.parseOrNull(viewModel.notePayload?.scheduled)?.startTime)

            R.id.fragment_note_scheduled_remove -> {
                updateTimestampView(TimeType.SCHEDULED, null)
                viewModel.updatePayloadScheduledTime(null)
            }

            /* Setting deadline time. */
            R.id.fragment_note_deadline_button ->
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_deadline_button,
                        TimeType.DEADLINE,
                        emptySet(), // Unused
                        OrgRange.parseOrNull(viewModel.notePayload?.deadline)?.startTime)

            R.id.fragment_note_deadline_remove -> {
                updateTimestampView(TimeType.DEADLINE, null)
                viewModel.updatePayloadDeadlineTime(null)
            }

            /* Setting closed time. */
            R.id.fragment_note_closed_edit_text ->
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_closed_edit_text,
                        TimeType.CLOSED,
                        emptySet(), // Unused
                        OrgRange.parseOrNull(viewModel.notePayload?.closed)?.startTime)

            R.id.fragment_note_closed_remove -> {
                updateTimestampView(TimeType.CLOSED, null)
                viewModel.updatePayloadClosedTime(null)
            }
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

    override fun onDateTimeSet(id: Int, noteIds: TreeSet<Long>, time: OrgDateTime?) {
        val range = if (time != null) OrgRange(time) else null

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

    override fun onDateTimeAborted(id: Int, noteIds: TreeSet<Long>) {

    }

    /*
     * Options Menu.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater)

        inflater.inflate(R.menu.note_actions, menu)

        ActivityUtils.keepScreenOnUpdateMenuItem(
                activity,
                menu,
                menu.findItem(R.id.keep_screen_on))

        // Remove search item
        menu.removeItem(R.id.activity_action_search)

        if (viewModel.notePayload == null) { // Displaying non-existent note.
            menu.removeItem(R.id.note_view_edit)
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

            menu.findItem(R.id.note_view_edit)
                    ?.actionView
                    ?.findViewById<Switch>(R.id.note_view_edit_switch)
                    ?.let { switch ->

                        switch.isChecked = viewModel.isInEditMode()

                        switch.setOnCheckedChangeListener { _, _ ->
                            viewModel.toggleViewEditMode()
                        }
                    }
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
                userSave()
                return true
            }

            R.id.keep_screen_on -> {
                dialog = ActivityUtils.keepScreenOnToggle(activity, item)
                return true
            }

            R.id.delete -> {
                userDelete()
                return true
            }

            R.id.metadata_show_all -> {
                item.isChecked = true
                AppPreferences.noteMetadataVisibility(context, "all")
                setMetadataViewsVisibility()
                return true
            }

            R.id.metadata_show_selected -> {
                item.isChecked = true
                AppPreferences.noteMetadataVisibility(context, "selected")
                setMetadataViewsVisibility()
                return true
            }

            R.id.metadata_always_show_set -> {
                item.isChecked = !item.isChecked
                AppPreferences.alwaysShowSetNoteMetadata(context, item.isChecked)
                setMetadataViewsVisibility()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setMetadataViewsVisibility() {
        setMetadataViewsVisibility(
                "tags",
                binding.fragmentNoteTagsContainer,
                !TextUtils.isEmpty(binding.fragmentNoteTags.text))

        setMetadataViewsVisibility(
                "state",
                binding.fragmentNoteStateContainer,
                !TextUtils.isEmpty(binding.fragmentNoteStateButton.text))

        setMetadataViewsVisibility(
                "priority",
                binding.fragmentNotePriorityContainer,
                !TextUtils.isEmpty(binding.fragmentNotePriorityButton.text))

        setMetadataViewsVisibility(
                "scheduled_time",
                binding.fragmentNoteScheduledTimeContainer,
                !TextUtils.isEmpty(binding.fragmentNoteScheduledButton.text))

        setMetadataViewsVisibility(
                "deadline_time",
                binding.fragmentNoteDeadlineTimeContainer,
                !TextUtils.isEmpty(binding.fragmentNoteDeadlineButton.text))

        setMetadataViewsVisibility(
                "properties",
                binding.fragmentNotePropertiesContainer,
                binding.fragmentNotePropertiesContainer.childCount > 1)
    }

    private fun setMetadataViewsVisibility(name: String?, container: View, isSet: Boolean) {
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

    private fun userSave() {
        ActivityUtils.closeSoftKeyboard(activity)

        updatePayloadFromViews()

        viewModel.saveNote()
    }

    private fun userCancel(): Boolean {
        ActivityUtils.closeSoftKeyboard(activity)

        updatePayloadFromViews()

        if (viewModel.isNoteModified()) {
            dialog = AlertDialog.Builder(context)
                    .setTitle(R.string.note_has_been_modified)
                    .setMessage(R.string.discard_or_save_changes)
                    .setPositiveButton(R.string.save) { _, _ ->
                        viewModel.saveNote {
                            listener?.onNoteCanceled()
                        }
                    }
                    .setNegativeButton(R.string.discard) { _, _ ->
                        listener?.onNoteCanceled()
                    }
                    .setNeutralButton(R.string.cancel, null)
                    .show()

            return true

        } else {
            listener?.onNoteCanceled()

            return false
        }
    }

    private fun userDelete() {
        viewModel.requestNoteDelete()
    }

    private fun userFollowBookBreadcrumb() {
        ActivityUtils.closeSoftKeyboard(activity)

        updatePayloadFromViews()

        if (viewModel.isNoteModified()) {
            dialog = AlertDialog.Builder(context)
                    .setTitle(R.string.note_has_been_modified)
                    .setMessage(R.string.discard_or_save_changes)
                    .setPositiveButton(R.string.save) { _, _ ->
                        viewModel.saveNote {
                            viewModel.followBookBreadcrumb()
                        }
                    }
                    .setNegativeButton(R.string.discard) { _, _ ->
                        viewModel.followBookBreadcrumb()
                    }
                    .setNeutralButton(R.string.cancel, null)
                    .show()
        } else {
            viewModel.followBookBreadcrumb()
        }
    }

    private fun userFollowNoteBreadcrumb(ancestor: Note) {
        ActivityUtils.closeSoftKeyboard(activity)

        updatePayloadFromViews()

        if (viewModel.isNoteModified()) {
            dialog = AlertDialog.Builder(context)
                    .setTitle(R.string.note_has_been_modified)
                    .setMessage(R.string.discard_or_save_changes)
                    .setPositiveButton(R.string.save) { _, _ ->
                        viewModel.saveNote {
                            viewModel.followNoteBreadcrumb(ancestor)
                        }
                    }
                    .setNegativeButton(R.string.discard) { _, _ ->
                        viewModel.followNoteBreadcrumb(ancestor)
                    }
                    .setNeutralButton(R.string.cancel, null)
                    .show()
        } else {
            viewModel.followNoteBreadcrumb(ancestor)
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

    private fun showSnackbar(message: String?) {
        CommonActivity.showSnackbar(context, message)
    }

    private fun showSnackbar(@StringRes resId: Int) {
        CommonActivity.showSnackbar(context, resId)
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
                initialContent: String? = null,
                attachmentUri: Uri? = null): NoteFragment? {

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
