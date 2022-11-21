package com.orgzly.android.ui.note

import android.content.Context
import android.content.Intent
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
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.BookUtils
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.db.entity.Note
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.sync.SyncRunner
import com.orgzly.android.ui.*
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.book.BookFragment
import com.orgzly.android.ui.settings.SettingsActivity
import com.orgzly.android.ui.share.ShareActivity
import com.orgzly.android.ui.util.*
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.OrgFormatter
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
class NoteFragment : CommonFragment(), View.OnClickListener, TimestampDialogFragment.OnDateTimeSetListener, DrawerItem {

    private lateinit var binding: FragmentNoteBinding

    @Inject
    internal lateinit var dataRepository: DataRepository

    private var listener: Listener? = null

    private lateinit var viewModel: NoteViewModel

    private lateinit var mUserTimeFormatter: UserTimeFormatter

    private var dialog: AlertDialog? = null

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    private val userCancelBackPressHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            userCancel()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        App.appComponent.inject(this)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, activity)

        listener = activity as Listener

        mUserTimeFormatter = UserTimeFormatter(context)
    }

    private fun noteInitialDataFromArguments(): NoteInitialData {
        requireNotNull(arguments).let { args ->
            // Book ID must be passed
            val bookId = args.getLong(ARG_BOOK_ID)
            require(bookId > 0) {
                "${NoteFragment::class.java.simpleName} requires $ARG_BOOK_ID argument passed"
            }

            // Is 0 for new notes
            val noteId = args.getLong(ARG_NOTE_ID)

            // Location (for new notes)
            val place: Place? = args.getString(ARG_PLACE)?.let {
                Place.valueOf(it)
            }

            // Initial values when sharing
            val title = args.getString(ARG_TITLE)
            val content = args.getString(ARG_CONTENT)
            val attachmentUri = args.getString(ARG_ATTACHMENT_URI)?.let { Uri.parse(it) }

            return NoteInitialData(bookId, noteId, place, title, content, attachmentUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        val noteInitialData = noteInitialDataFromArguments()

        sharedMainActivityViewModel = ViewModelProvider(requireActivity())
            .get(SharedMainActivityViewModel::class.java)

        val factory = NoteViewModelFactory.getInstance(dataRepository, noteInitialData)

        viewModel = ViewModelProvider(this, factory).get(NoteViewModel::class.java)

        requireActivity().onBackPressedDispatcher.addCallback(this, userCancelBackPressHandler)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        binding = FragmentNoteBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        setupObservers()

        binding.title.apply {
            // Keyboard's action button pressed
            setOnEditorActionListener { _, _, _ ->
                userSave()
                true
            }
        }

        binding.breadcrumbsText.movementMethod = LinkMovementMethod.getInstance()

        if (activity is ShareActivity) {
            binding.breadcrumbs.visibility = View.GONE
            binding.locationContainer.visibility = View.VISIBLE
            binding.locationButton.setOnClickListener(this)
        } else {
            binding.breadcrumbs.visibility = View.VISIBLE
            binding.locationContainer.visibility = View.GONE
        }


        // Hide remove button if there are no tags
        binding.tagsButton.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.tagsRemove.goneIf(TextUtils.isEmpty(binding.tagsButton.text))
            }

            override fun afterTextChanged(s: Editable) {}
        })

        binding.tagsMenu.setOnClickListener {
            binding.tagsButton.showDropDown()
        }

        binding.tagsRemove.setOnClickListener(this)

        setupTagsViewAdapter()

        binding.priorityButton.setOnClickListener(this)
        binding.priorityRemove.setOnClickListener(this)

        binding.stateButton.setOnClickListener(this)
        binding.stateRemove.setOnClickListener(this)

        binding.scheduledButton.setOnClickListener(this)
        binding.scheduledRemove.setOnClickListener(this)

        binding.deadlineButton.setOnClickListener(this)
        binding.deadlineRemove.setOnClickListener(this)

        binding.closedButton.setOnClickListener(this)
        binding.closedRemove.setOnClickListener(this)

        if (AppPreferences.isFontMonospaced(context)) {
            binding.content.setTypeface(Typeface.MONOSPACE)
        }

        binding.content.setOnUserTextChangeListener { str ->
            binding.content.setSourceText(str)

        }

        /*
         * Metadata folding
         */

        binding.metadataHeader.setOnClickListener {
            val isFolded = binding.metadata.visibility != View.VISIBLE
            setMetadataFoldState(!isFolded)
            AppPreferences.noteMetadataFolded(context, !isFolded)
        }

        setMetadataFoldState(AppPreferences.noteMetadataFolded(context))

        /*
         * Content folding
         */

        binding.contentHeader.setOnClickListener {
            isNoteContentFolded().not().let { isFolded ->
                // Close keyboard if content has a focus and it's being folded
                if (isFolded && binding.content.hasFocus()) {
                    KeyboardUtils.closeSoftKeyboard(activity)
                }

                setContentFoldState(isFolded)
                AppPreferences.isNoteContentFolded(context, isFolded)
            }
        }

        setContentFoldState(AppPreferences.isNoteContentFolded(context))
    }

    private fun topToolbarToViewMode() {
        binding.topToolbar.run {
            menu.clear()
            inflateMenu(R.menu.note_actions)

            ActivityUtils.keepScreenOnUpdateMenuItem(activity, menu)

            setNavigationIcon(R.drawable.ic_menu)

            setNavigationOnClickListener {
                sharedMainActivityViewModel.openDrawer()
            }

            if (viewModel.notePayload == null) {
                removeMenuItemsForNoData(menu)
            } else {
                updateMenuMetadataItemVisibility(menu)
            }

            /* Newly created note cannot be deleted. */
            if (viewModel.isNew()) {
                menu.removeItem(R.id.delete)
            }

            setOnMenuItemClickListener { menuItem ->
                handleActionItemClick(menuItem)
            }

            setOnClickListener {
                binding.scrollView.scrollTo(0, 0)
            }
        }
    }

    private fun updateMenuMetadataItemVisibility(menu: Menu) {
        when (AppPreferences.noteMetadataVisibility(context)) {
            "selected" -> menu.findItem(R.id.metadata_show_selected).isChecked = true
            else -> menu.findItem(R.id.metadata_show_all).isChecked = true
        }

        menu.findItem(R.id.metadata_always_show_set).isChecked =
            AppPreferences.alwaysShowSetNoteMetadata(context)
    }

    // Displaying a non-existent note, remove some menu items
    private fun removeMenuItemsForNoData(menu: Menu) {
        menu.removeItem(R.id.done)
        menu.removeItem(R.id.metadata)
        menu.removeItem(R.id.delete)
    }

    private fun handleActionItemClick(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.done -> {
                userSave()
            }

            R.id.clock_in -> {
                val content = binding.content.getSourceText()?.toString()
                val newContent = OrgFormatter.clockIn(content)
                binding.content.setSourceText(newContent)
            }

            R.id.clock_out -> {
                val content = binding.content.getSourceText()?.toString()
                val newContent = OrgFormatter.clockOut(content)
                binding.content.setSourceText(newContent)
            }

            R.id.clock_cancel -> {
                val content = binding.content.getSourceText()?.toString()
                val newContent = OrgFormatter.clockCancel(content)
                binding.content.setSourceText(newContent)
            }

            R.id.metadata_show_all -> {
                menuItem.isChecked = true
                AppPreferences.noteMetadataVisibility(context, "all")
                setMetadataViewsVisibility()
            }

            R.id.metadata_show_selected -> {
                menuItem.isChecked = true
                AppPreferences.noteMetadataVisibility(context, "selected")
                setMetadataViewsVisibility()
            }

            R.id.metadata_always_show_set -> {
                menuItem.isChecked = !menuItem.isChecked
                AppPreferences.alwaysShowSetNoteMetadata(context, menuItem.isChecked)
                setMetadataViewsVisibility()
            }

            R.id.keep_screen_on -> {
                dialog = ActivityUtils.keepScreenOnToggle(activity, menuItem)
            }

            R.id.delete -> {
                userDelete()
            }

            R.id.sync -> {
                SyncRunner.startSync()
            }

            R.id.activity_action_settings -> {
                startActivity(Intent(context, SettingsActivity::class.java))
            }
        }

        // Handled
        return true
    }

    private fun isNoteContentFolded(): Boolean {
        return binding.content.visibility != View.VISIBLE
    }

    private fun setContentFoldState(isFolded: Boolean) {
        binding.content.goneIf(isFolded)
        binding.contentHeaderUpIcon.goneIf(isFolded)
        binding.contentHeaderDownIcon.goneUnless(isFolded)
        // binding.contentHeaderText.invisibleIf(isFolded)
    }

    private fun setMetadataFoldState(isFolded: Boolean) {
        binding.metadata.goneIf(isFolded)
        binding.metadataHeaderUpIcon.goneIf(isFolded)
        binding.metadataHeaderDownIcon.goneUnless(isFolded)
        // binding.metadataHeaderText.invisibleIf(isFolded)
    }

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

            activity?.showSnackbar(message)
        })

        viewModel.noteDeleteRequest.observeSingle(viewLifecycleOwner, Observer { count ->
            val question = resources.getQuantityString(
                R.plurals.delete_note_or_notes_with_count_question, count, count)

            dialog = MaterialAlertDialogBuilder(requireContext())
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

        viewModel.errorEvent.observeSingle(viewLifecycleOwner, Observer { error ->
            activity?.showSnackbar((error.cause ?: error).localizedMessage)
        })

        viewModel.snackBarMessage.observeSingle(viewLifecycleOwner, Observer { resId ->
            activity?.showSnackbar(resId)
        })
    }

    private fun updateViewsFromPayload() {
        val payload = viewModel.notePayload ?: return

        // State
        setStateView(payload.state)

        // Priority
        setPriorityView(payload.priority)

        // Title
        binding.title.setSourceText(payload.title)

        // Tags
        if (payload.tags.isNotEmpty()) {
            binding.tagsButton.setText(TextUtils.join(" ", payload.tags))
        } else {
            binding.tagsButton.text = null
        }

        // Times
        updateTimestampView(TimeType.SCHEDULED, OrgRange.parseOrNull(payload.scheduled))
        updateTimestampView(TimeType.DEADLINE, OrgRange.parseOrNull(payload.deadline))
        updateTimestampView(TimeType.CLOSED, OrgRange.parseOrNull(payload.closed))

        // Properties
        binding.propertiesContainer.removeAllViews()
        for (property in payload.properties.all) {
            addPropertyToList(property.name, property.value)
        }
        addPropertyToList(null, null)

        // Content
        binding.content.noteId = viewModel.noteId
        binding.content.setSourceText(payload.content)
    }

    private fun addPropertyToList(propName: String?, propValue: String?) {
        View.inflate(activity, R.layout.property, binding.propertiesContainer)

        val propView = lastProperty()

        val name = propView.findViewById<EditText>(R.id.name)
        val value = propView.findViewById<EditText>(R.id.value)
        val remove = propView.findViewById<View>(R.id.remove)

        // Last property (this one) needs no remove button
        remove.visibility = View.INVISIBLE

        // Second to last property can now have its remove button
        if (binding.propertiesContainer.childCount > 1) {
            binding.propertiesContainer
                .getChildAt(binding.propertiesContainer.childCount - 2)
                .findViewById<View>(R.id.remove).visibility = View.VISIBLE
        }

        if (propName != null && propValue != null) { // Existing property
            name.setText(propName)
            value.setText(propValue)
        }

        remove.setOnClickListener {
            if (isOnlyProperty(propView) || isLastProperty(propView)) {
                name.text = null
                value.text = null
            } else {
                binding.propertiesContainer.removeView(propView)
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
        return binding.propertiesContainer.childCount == 1
                && binding.propertiesContainer.getChildAt(0) === view
    }

    private fun lastProperty(): ViewGroup {
        return binding.propertiesContainer
            .getChildAt(binding.propertiesContainer.childCount - 1) as ViewGroup
    }

    private fun updatePayloadFromViews() {
        val properties = OrgProperties()

        for (i in 0 until binding.propertiesContainer.childCount) {
            val property = binding.propertiesContainer.getChildAt(i)

            val name = (property.findViewById<View>(R.id.name) as TextView).text
            val value = (property.findViewById<View>(R.id.value) as TextView).text

            if (!TextUtils.isEmpty(name)) { // Ignore property with no name
                properties.put(name.toString(), value.toString())
            }
        }

        // Replace new lines with spaces, in case multi-line text has been pasted
        val title = binding.title.getSourceText().toString().replace("\n".toRegex(), " ").trim { it <= ' ' }

        val content = binding.content.getSourceText()?.toString().orEmpty()

        // TODO: Create a function (extension?) for this
        val state = if (TextUtils.isEmpty(binding.stateButton.text))
            null
        else
            binding.stateButton.text.toString()

        val priority = if (TextUtils.isEmpty(binding.priorityButton.text))
            null
        else
            binding.priorityButton.text.toString()

        val tags = binding.tagsButton.text.toString()
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
                    add(bookTitle, 0, if (viewModel.noteId == 0L) null else fun() {
                        userFollowBookBreadcrumb()
                    })

                    // Ancestors
                    data.ancestors.forEach { ancestor ->
                        add(ancestor.title, 0) {
                            userFollowNoteBreadcrumb(ancestor)
                        }
                    }
                }

                binding.breadcrumbsText.text = breadcrumbs.toCharSequence()

                binding.locationButton.text = bookTitle
            }

            binding.viewFlipper.displayedChild =
                if (viewModel.isNew()) {
                    0
                } else if (viewModel.notePayload != null) {
                    0
                } else {
                    1
                }

            // Load payload from saved Bundle if available
            if (savedInstanceState != null) {
                viewModel.restorePayloadFromBundle(savedInstanceState)
            }

            updateViewsFromPayload()

            topToolbarToViewMode()

            setMetadataViewsVisibility()

            /* Open the keyboard for new notes, unless fragment was given
             * some initial values (for example from ShareActivity).
             */
            if (viewModel.isNew() && !viewModel.hasInitialData()) {
                binding.title.toEditMode(0)
            }
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
                binding.tagsButton.setAdapter(adapter)
                binding.tagsButton.setTokenizer(SpaceTokenizer())
            }
        })
    }

    override fun onResume() {
        super.onResume()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        sharedMainActivityViewModel.setCurrentFragment(FRAGMENT_TAG)
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

        binding.viewFlipper.displayedChild = 0
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
            TimeType.SCHEDULED -> {
                binding.scheduledButton.text = range?.let { mUserTimeFormatter.formatAll(it) }
                binding.scheduledRemove.invisibleIf(binding.scheduledButton.text.isNullOrEmpty())
            }

            TimeType.DEADLINE -> {
                binding.deadlineButton.text = range?.let { mUserTimeFormatter.formatAll(it) }
                binding.deadlineRemove.invisibleIf(binding.deadlineButton.text.isNullOrEmpty())
            }

            TimeType.CLOSED -> {
                binding.closedButton.text = range?.let { mUserTimeFormatter.formatAll(it) }
                binding.closedRemove.invisibleIf(binding.closedButton.text.isNullOrEmpty())

                // Do not display CLOSED button if it's not set.
                binding.closedTimeContainer.goneIf(range == null)
            }

            else -> { }
        }
    }

    private fun handleNoteBookChangeRequest(books: List<BookView>) {
        val bookNames = books.map { it.book.name }.toTypedArray()

        val selected = getSelectedBook(books, viewModel.bookId)

        dialog = MaterialAlertDialogBuilder(requireContext())
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
            R.id.location_button -> {
                viewModel.requestNoteBookChange()
            }

            R.id.tags_remove -> {
                binding.tagsButton.text = null
            }

            R.id.state_button -> {
                val states = NoteStates.fromPreferences(requireContext())

                val keywords = states.array

                val currentState = if (!TextUtils.isEmpty(binding.stateButton.text)) {
                    states.indexOf(binding.stateButton.text.toString())
                } else {
                    -1
                }

                dialog = MaterialAlertDialogBuilder(requireContext())
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

            R.id.state_remove -> {
                setState(null)
            }

            R.id.priority_button -> {
                val priorities = NotePriorities.fromPreferences(requireContext())

                val keywords = priorities.array

                var currentPriority = -1
                if (!TextUtils.isEmpty(binding.priorityButton.text)) {
                    currentPriority = priorities.indexOf(binding.priorityButton.text.toString())
                }

                dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.priority)
                    .setSingleChoiceItems(keywords, currentPriority) { dialog, which ->
                        setPriorityView(priorities[which])
                        dialog.dismiss()
                    }
                    .setNeutralButton(R.string.clear) { _, _ -> setPriorityView(null) }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }

            R.id.priority_remove -> {
                setPriorityView(null)
            }

            /* Setting scheduled time. */
            R.id.scheduled_button ->
                f = TimestampDialogFragment.getInstance(
                    R.id.scheduled_button,
                    TimeType.SCHEDULED,
                    emptySet(), // Unused
                    OrgRange.parseOrNull(viewModel.notePayload?.scheduled)?.startTime)

            R.id.scheduled_remove -> {
                updateTimestampView(TimeType.SCHEDULED, null)
                viewModel.updatePayloadScheduledTime(null)
            }

            /* Setting deadline time. */
            R.id.deadline_button ->
                f = TimestampDialogFragment.getInstance(
                    R.id.deadline_button,
                    TimeType.DEADLINE,
                    emptySet(), // Unused
                    OrgRange.parseOrNull(viewModel.notePayload?.deadline)?.startTime)

            R.id.deadline_remove -> {
                updateTimestampView(TimeType.DEADLINE, null)
                viewModel.updatePayloadDeadlineTime(null)
            }

            /* Setting closed time. */
            R.id.closed_button ->
                f = TimestampDialogFragment.getInstance(
                    R.id.closed_button,
                    TimeType.CLOSED,
                    emptySet(), // Unused
                    OrgRange.parseOrNull(viewModel.notePayload?.closed)?.startTime)

            R.id.closed_remove -> {
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
            R.id.scheduled_button -> {
                updateTimestampView(TimeType.SCHEDULED, range)
                viewModel.updatePayloadScheduledTime(range)
            }

            R.id.deadline_button -> {
                updateTimestampView(TimeType.DEADLINE, range)
                viewModel.updatePayloadDeadlineTime(range)
            }

            R.id.closed_button -> {
                updateTimestampView(TimeType.CLOSED, range)
                viewModel.updatePayloadClosedTime(range)
            }
        }
    }

    override fun onDateTimeAborted(id: Int, noteIds: TreeSet<Long>) {

    }

    private fun setMetadataViewsVisibility() {
        setMetadataViewsVisibility(
            "tags",
            binding.tagsContainer,
            !TextUtils.isEmpty(binding.tagsButton.text))

        setMetadataViewsVisibility(
            "state",
            binding.stateContainer,
            !TextUtils.isEmpty(binding.stateButton.text))

        setMetadataViewsVisibility(
            "priority",
            binding.priorityContainer,
            !TextUtils.isEmpty(binding.priorityButton.text))

        setMetadataViewsVisibility(
            "scheduled_time",
            binding.scheduledTimeContainer,
            !TextUtils.isEmpty(binding.scheduledButton.text))

        setMetadataViewsVisibility(
            "deadline_time",
            binding.deadlineTimeContainer,
            !TextUtils.isEmpty(binding.deadlineButton.text))

        setMetadataViewsVisibility(
            "properties",
            binding.propertiesContainer,
            binding.propertiesContainer.childCount > 1)
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

            container.goneUnless(isVisible)
        }
    }

    private fun userSave() {
        // KeyboardUtils.closeSoftKeyboard(activity)

        updatePayloadFromViews()

        viewModel.saveNote()
    }

    private fun userCancel(): Boolean {
        KeyboardUtils.closeSoftKeyboard(activity)

        updatePayloadFromViews()

        if (viewModel.isNoteModified()) {
            dialog = MaterialAlertDialogBuilder(requireContext())
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
        KeyboardUtils.closeSoftKeyboard(activity)

        updatePayloadFromViews()

        if (viewModel.isNoteModified()) {
            dialog = MaterialAlertDialogBuilder(requireContext())
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
        KeyboardUtils.closeSoftKeyboard(activity)

        updatePayloadFromViews()

        if (viewModel.isNoteModified()) {
            dialog = MaterialAlertDialogBuilder(requireContext())
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
        binding.breadcrumbsText.text = title
        binding.locationButton.text = title

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
        binding.stateButton.text =
            if (state == null || NoteStates.NO_STATE_KEYWORD == state) {
                null
            } else {
                state
            }
        binding.stateRemove.invisibleUnless(!binding.stateButton.text.isNullOrEmpty())
    }

    private fun setPriorityView(priority: String?) {
        binding.priorityButton.text = priority
        binding.priorityRemove.invisibleUnless(!binding.priorityButton.text.isNullOrEmpty())
    }

    /**
     * Mark note's book in the drawer.
     */
    override fun getCurrentDrawerItemId(): String {
        return BookFragment.getDrawerItemId(viewModel.bookId)
    }

    fun getNoteId(): Long {
        return viewModel.noteId
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
        private const val ARG_ATTACHMENT_URI = "attachment_uri"

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
                        initialContent,
                        attachmentUri)
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
                initialContent: String? = null,
                attachmentUri: Uri? = null): NoteFragment {

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

            if (attachmentUri != null) {
                args.putString(ARG_ATTACHMENT_URI, attachmentUri.toString())
            }

            fragment.arguments = args

            return fragment
        }
    }
}
