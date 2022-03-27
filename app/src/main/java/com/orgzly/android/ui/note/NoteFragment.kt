package com.orgzly.android.ui.note

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import com.orgzly.android.ui.main.setupSearchView
import com.orgzly.android.ui.note.NoteViewModel.Companion.APP_BAR_DEFAULT_MODE
import com.orgzly.android.ui.note.NoteViewModel.Companion.APP_BAR_EDIT_MODE
import com.orgzly.android.ui.notes.book.BookFragment
import com.orgzly.android.ui.settings.SettingsActivity
import com.orgzly.android.ui.share.ShareActivity
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.ui.util.removeBackgroundKeepPadding
import com.orgzly.android.ui.util.styledAttributes
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

    @Inject
    internal lateinit var dataRepository: DataRepository

    private var listener: Listener? = null

    private lateinit var viewModel: NoteViewModel

    private lateinit var mUserTimeFormatter: UserTimeFormatter

    private var dialog: AlertDialog? = null

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    private val userCancelBackPressHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            userCancel()
        }
    }

    private val toViewModeBackPressHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.toViewMode()
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

            return NoteInitialData(bookId, noteId, place, title, content)
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

        requireActivity().onBackPressedDispatcher.addCallback(this, toViewModeBackPressHandler)
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

        /*
         * Not working when done in XML.
         * We want imeOptions="actionDone", so we can't use textMultiLine.
         */
        binding.title.apply {
            setHorizontallyScrolling(false)

            maxLines = Integer.MAX_VALUE

            // Keyboard's action button pressed
            setOnEditorActionListener { _, _, _ ->
                userSave()
                true
            }
        }

        binding.titleView.apply {
            removeBackgroundKeepPadding()

            setOnFocusOrClickListener(View.OnClickListener {
                viewModel.toEditTitleMode()
            })
        }

        binding.breadcrumbsText.movementMethod = LinkMovementMethod.getInstance()

        if (activity is ShareActivity) {
            binding.breadcrumbs.visibility = View.GONE

            binding.locationButton.let {
                it.visibility = View.VISIBLE
                it.setOnClickListener(this)
            }
        } else {
            binding.breadcrumbs.visibility = View.VISIBLE
            binding.locationButton.visibility = View.GONE
        }


        /* Hint causes minimum width - when tags' width is smaller then hint's, there is empty space. */
        binding.tags.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding.tags.apply {
                    if (!TextUtils.isEmpty(text.toString())) {
                        hint = ""
                    } else {
                        setHint(R.string.fragment_note_tags_hint)
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        binding.tagsButton.setOnClickListener {
            binding.tags.showDropDown()
        }

        setupTagsViewAdapter()

        binding.priorityButton.setOnClickListener(this)
        binding.priorityRemove.setOnClickListener(this)

        binding.stateButton.setOnClickListener(this)
        binding.stateRemove.setOnClickListener(this)

        binding.scheduledButton.setOnClickListener(this)
        binding.scheduledRemove.setOnClickListener(this)

        binding.deadlineButton.setOnClickListener(this)
        binding.deadlineRemove.setOnClickListener(this)

        binding.closedEditText.setOnClickListener(this)
        binding.closedRemove.setOnClickListener(this)

        binding.bodyView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                // Update bodyEdit text when checkboxes are clicked
                val text = binding.bodyView.getSourceText()
                binding.contentEdit.setText(text)
            }
        })

        binding.bodyView.apply {
            removeBackgroundKeepPadding()

            setOnFocusOrClickListener(View.OnClickListener {
                viewModel.toEditContentMode()
            })
        }

        // View mode on keyboard back press
        listOf(binding.contentEdit, binding.title).forEach { editView ->
            editView.setOnImeBackListener {
                viewModel.toViewMode()
            }
        }

        if (activity != null && AppPreferences.isFontMonospaced(context)) {
            binding.contentEdit.typeface = Typeface.MONOSPACE
            binding.bodyView.typeface = Typeface.MONOSPACE
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
                if (isFolded && binding.contentEdit.hasFocus()) {
                    ActivityUtils.closeSoftKeyboard(activity)
                }

                setContentFoldState(isFolded)
                AppPreferences.isNoteContentFolded(context, isFolded)
            }
        }

        setContentFoldState(AppPreferences.isNoteContentFolded(context))
    }

    private fun appBarToDefault() {
        binding.bottomAppBar.run {
            replaceMenu(R.menu.note_actions)

            setNavigationIcon(context.styledAttributes(R.styleable.Icons) { typedArray ->
                typedArray.getResourceId(R.styleable.Icons_ic_menu_24dp, 0)
            })

            setNavigationOnClickListener {
                sharedMainActivityViewModel.openDrawer()
            }

            // TODO: Move keep_screen_on to the fun, rename all is to be the same for this item
            ActivityUtils.keepScreenOnUpdateMenuItem(
                activity,
                menu,
                menu.findItem(R.id.keep_screen_on))

            if (viewModel.notePayload == null) {
                removeMenuItemsForNoData(menu)

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

            setOnMenuItemClickListener { menuItem ->
                handleActionItemClick(menuItem)
            }

            requireActivity().setupSearchView(menu)
        }

        // binding.fab.hide()
//        binding.fab.run {
//            setOnClickListener {
//                userSave()
//            }
//            show()
//        }
    }

    private fun appBarToEdit() {
        binding.bottomAppBar.run {
            replaceMenu(R.menu.note_actions_edit)

            setNavigationIcon(context.styledAttributes(R.styleable.Icons) { typedArray ->
                typedArray.getResourceId(R.styleable.Icons_ic_close_24dp, 0)
            })

            setNavigationOnClickListener {
                userCancel()
            }

            if (viewModel.notePayload == null) {
                removeMenuItemsForNoData(menu)
            }

            setOnMenuItemClickListener { menuItem ->
                handleActionItemClick(menuItem)
            }

            // requireActivity().setupSearchView(menu)
        }

        // binding.fab.hide()
    }

    // Displaying a non-existent note, remove some menu items
    private fun removeMenuItemsForNoData(menu: Menu) {
        menu.removeItem(R.id.to_edit_mode)
        menu.removeItem(R.id.to_view_mode)
        menu.removeItem(R.id.done)
        menu.removeItem(R.id.metadata)
        menu.removeItem(R.id.delete)
    }

    private fun handleActionItemClick(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.to_view_mode -> {
                viewModel.toViewMode()
            }

            R.id.to_edit_mode -> {
                viewModel.toEditMode()
            }

            R.id.done -> {
                userSave()
            }

            R.id.keep_screen_on -> {
                dialog = ActivityUtils.keepScreenOnToggle(activity, menuItem)
            }

            R.id.delete -> {
                userDelete()
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

            R.id.activity_action_settings -> {
                startActivity(Intent(context, SettingsActivity::class.java))
            }
        }

        // Handled
        return true
    }

    private fun isNoteContentFolded(): Boolean {
        return binding.contentViews.visibility != View.VISIBLE
    }

    private fun setContentFoldState(isFolded: Boolean) {
        binding.contentViews.visibility = visibleOrGone(!isFolded)
        binding.contentHeaderUpIcon.visibility = visibleOrGone(!isFolded)
        binding.contentHeaderDownIcon.visibility = visibleOrGone(isFolded)
        // binding.contentHeaderText.visibility = if (isFolded) View.VISIBLE else View.INVISIBLE
    }

    private fun setMetadataFoldState(isFolded: Boolean) {
        binding.metadata.visibility = visibleOrGone(!isFolded)
        binding.metadataHeaderUpIcon.visibility = visibleOrGone(!isFolded)
        binding.metadataHeaderDownIcon.visibility = visibleOrGone(isFolded)
        // binding.metadataHeaderText.visibility = if (isFolded) View.VISIBLE else View.INVISIBLE
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

            activity?.showSnackbar(message)
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
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed viewEditMode: $viewEditMode")

            when (viewEditMode) {
                NoteViewModel.ViewEditMode.VIEW -> {
                    toViewMode()
                }

                NoteViewModel.ViewEditMode.EDIT -> {
                    toEditMode()
                }

                NoteViewModel.ViewEditMode.EDIT_TITLE_WITH_KEYBOARD -> {
                    toEditMode()
                    ActivityUtils.openSoftKeyboard(activity, binding.title)
                }

                NoteViewModel.ViewEditMode.EDIT_CONTENT_WITH_KEYBOARD -> {
                    toEditMode()
                    ActivityUtils.openSoftKeyboard(activity, binding.contentEdit)
                }

                null -> { }
            }
        })

        viewModel.appBar.mode.observeSingle(viewLifecycleOwner) { mode ->
            when (mode) {
                APP_BAR_DEFAULT_MODE -> {
                    appBarToDefault()
                    sharedMainActivityViewModel.unlockDrawer()
                    userCancelBackPressHandler.isEnabled = true
                    toViewModeBackPressHandler.isEnabled = false
                }

                APP_BAR_EDIT_MODE -> {
                    appBarToEdit()
                    sharedMainActivityViewModel.lockDrawer()
                    userCancelBackPressHandler.isEnabled = false
                    toViewModeBackPressHandler.isEnabled = true
                }
            }
        }

        viewModel.errorEvent.observeSingle(viewLifecycleOwner, Observer { error ->
            activity?.showSnackbar((error.cause ?: error).localizedMessage)
        })

        viewModel.snackBarMessage.observeSingle(viewLifecycleOwner, Observer { resId ->
            activity?.showSnackbar(resId)
        })
    }

    private fun toEditMode() {
        binding.titleView.visibility = View.GONE
        binding.title.visibility = View.VISIBLE

        binding.bodyView.visibility = View.GONE
        binding.contentEdit.visibility = View.VISIBLE

        // binding.toolbar.visibility = View.GONE

        viewModel.appBar.toMode(APP_BAR_EDIT_MODE)
    }

    private fun toViewMode() {
        ActivityUtils.closeSoftKeyboard(activity)

        binding.title.visibility = View.GONE
        binding.titleView.setSourceText(binding.title.text.toString())
        binding.titleView.visibility = View.VISIBLE

        binding.contentEdit.visibility = View.GONE

        binding.bodyView.setSourceText(binding.contentEdit.text.toString())

        ImageLoader.loadImages(binding.bodyView)

        binding.bodyView.visibility = View.VISIBLE

        // binding.toolbar.visibility = View.VISIBLE

        viewModel.appBar.toMode(APP_BAR_DEFAULT_MODE)
    }

    private fun updateViewsFromPayload() {
        val payload = viewModel.notePayload ?: return

        // State
        setStateView(payload.state)

        // Priority
        setPriorityView(payload.priority)

        // Title
        binding.title.setText(payload.title)
        binding.titleView.setSourceText(payload.title)

        // Tags
        if (!payload.tags.isEmpty()) {
            binding.tags.setText(TextUtils.join(" ", payload.tags))
        } else {
            binding.tags.text = null
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

        binding.contentEdit.setText(payload.content)

        binding.bodyView.setSourceText(payload.content ?: "")

        ImageLoader.loadImages(binding.bodyView)
    }

    private fun addPropertyToList(propName: String?, propValue: String?) {
        View.inflate(activity, R.layout.property, binding.propertiesContainer)

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
        val title = binding.title.text.toString().replace("\n".toRegex(), " ").trim { it <= ' ' }

        val content = binding.contentEdit.text.toString()

        // TODO: Create a function (extension?) for this
        val state = if (TextUtils.isEmpty(binding.stateButton.text))
            null
        else
            binding.stateButton.text.toString()

        val priority = if (TextUtils.isEmpty(binding.priorityButton.text))
            null
        else
            binding.priorityButton.text.toString()

        val tags = binding.tags.text.toString()
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

            if (viewModel.isNew()) { // New note
                binding.viewFlipper.displayedChild = 0

            } else { // Existing note
                if (viewModel.notePayload != null) {
                    binding.viewFlipper.displayedChild = 0
                } else {
                    binding.viewFlipper.displayedChild = 1
                    removeMenuItemsForNoData(binding.bottomAppBar.menu)
                }
            }

            // Load payload from saved Bundle if available
            if (savedInstanceState != null) {
                viewModel.restorePayloadFromBundle(savedInstanceState)
            }

            updateViewsFromPayload()

            setMetadataViewsVisibility()

            /* Open keyboard for new notes, unless fragment was given
             * some initial values (for example from ShareActivity).
             */
            if (viewModel.isNew() && !viewModel.hasInitialData()) {
                viewModel.toEditTitleMode(saveMode = false)
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
                binding.tags.setAdapter(adapter)
                binding.tags.setTokenizer(SpaceTokenizer())
            }
        })
    }

    override fun onResume() {
        super.onResume()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        sharedMainActivityViewModel.setCurrentFragment(FRAGMENT_TAG)

        sharedMainActivityViewModel.lockDrawer();
    }

    override fun onPause() {
        super.onPause()

        dialog?.dismiss()
        dialog = null

        ActivityUtils.keepScreenOnClear(activity)

        sharedMainActivityViewModel.unlockDrawer();
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
            TimeType.SCHEDULED -> if (range != null) {
                binding.scheduledButton.text = mUserTimeFormatter.formatAll(range)
            } else {
                binding.scheduledButton.text = null
            }

            TimeType.DEADLINE -> if (range != null) {
                binding.deadlineButton.text = mUserTimeFormatter.formatAll(range)
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "deadline button set to ${binding.deadlineButton.text}")
            } else {
                binding.deadlineButton.text = null
            }

            TimeType.CLOSED ->
                /*
                 * Do not display CLOSED button if it's not set.
                 * It will be updated on state change.
                 */
                if (range != null) {
                    binding.closedEditText.text = mUserTimeFormatter.formatAll(range)
                    binding.closedTimeContainer.visibility = View.VISIBLE
                } else {
                    binding.closedTimeContainer.visibility = View.GONE
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
            R.id.location_button -> {
                viewModel.requestNoteBookChange()
            }

            R.id.state_button -> {
                val states = NoteStates.fromPreferences(requireContext())

                val keywords = states.array

                val currentState = if (!TextUtils.isEmpty(binding.stateButton.text)) {
                    states.indexOf(binding.stateButton.text.toString())
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
            R.id.closed_edit_text ->
                f = TimestampDialogFragment.getInstance(
                    R.id.closed_edit_text,
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

            R.id.closed_edit_text -> {
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
            !TextUtils.isEmpty(binding.tags.text))

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

            container.visibility = if (isVisible) View.VISIBLE else View.GONE
        }
    }

    private fun userSave() {
        // ActivityUtils.closeSoftKeyboard(activity)

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
        this.binding.stateButton.text =
            if (state == null || NoteStates.NO_STATE_KEYWORD == state) {
                null
            } else {
                state
            }
    }

    private fun setPriorityView(priority: String?) {
        this.binding.priorityButton.text = priority
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
