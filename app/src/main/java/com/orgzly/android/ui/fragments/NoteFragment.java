package com.orgzly.android.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.App;
import com.orgzly.android.Book;
import com.orgzly.android.BookUtils;
import com.orgzly.android.Note;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.NotePriorities;
import com.orgzly.android.ui.NoteStates;
import com.orgzly.android.ui.Place;
import com.orgzly.android.ui.ShareActivity;
import com.orgzly.android.ui.dialogs.TimestampDialogFragment;
import com.orgzly.android.ui.drawer.DrawerItem;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.ui.views.TextViewWithMarkup;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.android.util.OrgFormatter;
import com.orgzly.android.util.SpaceTokenizer;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.OrgHead;
import com.orgzly.org.OrgProperties;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;
import com.orgzly.org.parser.OrgParserWriter;
import com.orgzly.org.utils.StateChangeLogic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Note editor.
 */
public class NoteFragment extends Fragment
        implements
        View.OnClickListener,
        TimestampDialogFragment.OnDateTimeSetListener,
        DrawerItem {

    private static final String TAG = NoteFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = NoteFragment.class.getName();

    private static final String ARG_ORIGINAL_NOTE_HASH = "original_note_hash";

    private static final String ARG_IS_NEW = "is_new";
    private static final String ARG_BOOK_ID = "book_id";
    private static final String ARG_NOTE_ID = "note_id";
    private static final String ARG_PLACE = "place";
    private static final String ARG_TITLE = "title";
    private static final String ARG_CONTENT = "content";

    /* Bundle keys for saving note. */
    private static final String ARG_CURRENT_STATE = "current_state";
    private static final String ARG_CURRENT_PRIORITY = "current_priority";
    private static final String ARG_CURRENT_TITLE = "current_title";
    private static final String ARG_CURRENT_TAGS = "current_tags";
    private static final String ARG_CURRENT_SCHEDULED = "current_scheduled";
    private static final String ARG_CURRENT_DEADLINE = "current_deadline";
    private static final String ARG_CURRENT_CLOSED = "current_closed";
    private static final String ARG_CURRENT_PROPERTIES = "current_properties";
    private static final String ARG_CURRENT_CONTENT = "current_content";

    private NoteFragmentListener mListener;

    private Shelf mShelf;

    /* Arguments. */
    private boolean mIsNew;
    private long mBookId;
    private long mNoteId; /* Could be null if new note is being created. */
    private Place place; /* Relative location, used for new notes. */
    private String mInitialTitle; /* Initial title (used for when sharing to Orgzly) */
    private String mInitialContent; /* Initial content (used when sharing to Orgzly) */

    private Note note;
    private Book book;


    private EditText title;

    private TextView locationView;
    private TextView locationButtonView;

    private View tagsContainer;
    private MultiAutoCompleteTextView tagsView;

    private View stateContainer;
    private TextView state;

    private View priorityContainer;
    private TextView priority;

    private View scheduledTimeContainer;
    private TextView scheduledButton;

    private View deadlineTimeContainer;
    private TextView deadlineButton;

    private ViewGroup closedTimeContainer;
    private TextView closedText;

    private LinearLayout propertiesContainer;

    private ToggleButton editSwitch;
    private EditText bodyEdit;
    private TextViewWithMarkup bodyView;

    /** Used to switch to note-does-not-exist view, if the note has been deleted. */
    private ViewFlipper mViewFlipper;

    private UserTimeFormatter mUserTimeFormatter;

    private AlertDialog dialog;

    public static NoteFragment forSharedNote(long bookId, String title, String content) {
        return getInstance(true, bookId, 0, Place.UNSPECIFIED, title, content);
    }

    public static NoteFragment forBook(boolean isNew, long bookId, long noteId, Place place) {
        return getInstance(isNew, bookId, noteId, place, null, null);
    }

    private static NoteFragment getInstance(boolean isNew, long bookId, long noteId, Place place, String initialTitle, String initialContent) {
        NoteFragment fragment = new NoteFragment();

        Bundle args = new Bundle();

        args.putBoolean(ARG_IS_NEW, isNew);

        args.putLong(ARG_BOOK_ID, bookId);

        if (noteId > 0) {
            args.putLong(ARG_NOTE_ID, noteId);
        }

        args.putString(ARG_PLACE, place.toString());

        if (initialTitle != null) {
            args.putString(ARG_TITLE, initialTitle);
        }

        if (initialContent != null) {
            args.putString(ARG_CONTENT, initialContent);
        }

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NoteFragment() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
    }

    @Override
    public void onAttach(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, getActivity());
        super.onAttach(context);

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (NoteFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + NoteFragmentListener.class);
        }

        mShelf = new Shelf(getActivity().getApplicationContext());

        parseArguments();

        mUserTimeFormatter = new UserTimeFormatter(getActivity().getApplicationContext());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        final View top = inflater.inflate(R.layout.fragment_note, container, false);


        title = top.findViewById(R.id.fragment_note_title);

        /*
         * Only works when set from code.
         * We want imeOptions="actionDone", so we can't use textMultiLine.
         */
        title.setHorizontallyScrolling(false);
        title.setMaxLines(3);

        /* Keyboard's action button pressed. */
        title.setOnEditorActionListener((v, actionId, event) -> {
            save();
            return true;
        });

        locationView = top.findViewById(R.id.fragment_note_location);
        locationButtonView = top.findViewById(R.id.fragment_note_location_button);

        if (getActivity() instanceof ShareActivity) {
            locationView.setVisibility(View.GONE);
            locationButtonView.setVisibility(View.VISIBLE);
            locationButtonView.setOnClickListener(this);
        } else {
            locationView.setVisibility(View.VISIBLE);
            locationButtonView.setVisibility(View.GONE);
        }

        tagsContainer = top.findViewById(R.id.fragment_note_tags_container);

        tagsView = top.findViewById(R.id.fragment_note_tags);

        tagsView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && tagsView.getAdapter() == null) {
                setupTagsViewAdapter();
            }
        });

        /* Hint causes minimum width - when tags' width is smaller then hint's, there is empty space. */
        tagsView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(tagsView.getText().toString())) {
                    tagsView.setHint("");
                } else {
                    tagsView.setHint(R.string.fragment_note_tags_hint);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        priorityContainer = top.findViewById(R.id.fragment_note_priority_container);
        priority = top.findViewById(R.id.fragment_note_priority_button);
        priority.setOnClickListener(this);

        stateContainer = top.findViewById(R.id.fragment_note_state_container);
        state = top.findViewById(R.id.fragment_note_state_button);
        state.setOnClickListener(this);

        scheduledTimeContainer = top.findViewById(R.id.fragment_note_scheduled_time_container);
        scheduledButton = top.findViewById(R.id.fragment_note_scheduled_button);
        scheduledButton.setOnClickListener(this);

        deadlineTimeContainer = top.findViewById(R.id.fragment_note_deadline_time_container);
        deadlineButton = top.findViewById(R.id.fragment_note_deadline_button);
        deadlineButton.setOnClickListener(this);

        closedTimeContainer = top.findViewById(R.id.fragment_note_closed_time_container);
        closedText = top.findViewById(R.id.fragment_note_closed_edit_text);
        closedText.setOnClickListener(this);

        propertiesContainer = top.findViewById(R.id.fragment_note_properties_container);

        bodyEdit = top.findViewById(R.id.body_edit);

        bodyView = top.findViewById(R.id.body_view);

        bodyView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Update bodyEdit text when checkboxes are clicked
                CharSequence text = bodyView.getRawText();
                bodyEdit.setText(text);
            }
        });

        if (getActivity() != null && AppPreferences.isFontMonospaced(getContext())) {
            bodyEdit.setTypeface(Typeface.MONOSPACE);
            bodyView.setTypeface(Typeface.MONOSPACE);
        }

        editSwitch = top.findViewById(R.id.edit_content_toggle);

        editSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, buttonView, isChecked);

            if (isChecked) {
                bodyView.setVisibility(View.GONE);

                bodyEdit.setVisibility(View.VISIBLE);

            } else {
                bodyEdit.setVisibility(View.GONE);

                bodyView.setRawText(bodyEdit.getText());
                bodyView.setVisibility(View.VISIBLE);

                ActivityUtils.closeSoftKeyboard(getActivity());
            }
        });

        editSwitch.setOnClickListener(view -> {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, editSwitch.isChecked());

            if (editSwitch.isChecked()) { // Clicked to edit content
                ActivityUtils.openSoftKeyboard(getActivity(), bodyEdit);
            }
        });

        mViewFlipper = top.findViewById(R.id.fragment_note_view_flipper);

        return top;
    }

    /**
     * Note -> Bundle
     */
    private void updateBundleFromNote(Bundle outState) {
        OrgHead head = note.getHead();

        outState.putString(ARG_CURRENT_STATE, head.getState());
        outState.putString(ARG_CURRENT_PRIORITY, head.getPriority());

        outState.putString(ARG_CURRENT_TITLE, head.getTitle());

        outState.putString(ARG_CURRENT_TAGS, TextUtils.join(" ", head.getTags()));

        if (head.getScheduled() != null) {
            outState.putString(ARG_CURRENT_SCHEDULED, head.getScheduled().toString());
        }

        if (head.getDeadline() != null) {
            outState.putString(ARG_CURRENT_DEADLINE, head.getDeadline().toString());
        }

        if (head.getClosed() != null) {
            outState.putString(ARG_CURRENT_CLOSED, head.getClosed().toString());
        }

        /* Store properties as an array of strings.
         *
         *     name1 value1 name2 value2
         */
        if (head.hasProperties()) {
            ArrayList<String> list = MiscUtils.toArrayList(head.getProperties());
            outState.putStringArrayList(ARG_CURRENT_PROPERTIES, list);

        } else {
            outState.remove(ARG_CURRENT_PROPERTIES);
        }

        outState.putString(ARG_CURRENT_CONTENT, head.getContent());
    }

    /**
     * Bundle -> Note
     */
    private void updateNoteFromBundle(Bundle savedInstanceState) {
        OrgHead head = note.getHead();

        head.setState(savedInstanceState.getString(ARG_CURRENT_STATE));
        head.setPriority(savedInstanceState.getString(ARG_CURRENT_PRIORITY));

        head.setTitle(savedInstanceState.getString(ARG_CURRENT_TITLE));

        if (savedInstanceState.getString(ARG_CURRENT_TAGS) != null) {
            head.setTags(savedInstanceState.getString(ARG_CURRENT_TAGS).split("\\s+"));
        } else {
            head.setTags(new String[] {});
        }

        if (TextUtils.isEmpty(savedInstanceState.getString(ARG_CURRENT_SCHEDULED))) {
            head.setScheduled(null);
        } else {
            head.setScheduled(OrgRange.parse(savedInstanceState.getString(ARG_CURRENT_SCHEDULED)));
        }

        if (TextUtils.isEmpty(savedInstanceState.getString(ARG_CURRENT_DEADLINE))) {
            head.setDeadline(null);
        } else {
            head.setDeadline(OrgRange.parse(savedInstanceState.getString(ARG_CURRENT_DEADLINE)));
        }

        if (TextUtils.isEmpty(savedInstanceState.getString(ARG_CURRENT_CLOSED))) {
            head.setClosed(null);
        } else {
            head.setClosed(OrgRange.parse(savedInstanceState.getString(ARG_CURRENT_CLOSED)));
        }

        head.removeProperties();
        if (savedInstanceState.containsKey(ARG_CURRENT_PROPERTIES)) {
            ArrayList<String> array = savedInstanceState.getStringArrayList(ARG_CURRENT_PROPERTIES);
            if (array != null) {
                for (int i = 0; i < array.size(); i += 2) {
                    head.addProperty(array.get(i), array.get(i + 1));
                }
            }
        }

        head.setContent(savedInstanceState.getString(ARG_CURRENT_CONTENT));
    }

    /**
     * Note -> Views
     */
    private void updateViewsFromNote() {
        OrgHead head = note.getHead();

        setStateView(head.getState());

        setPriorityView(head.getPriority());

        /* Title. */
        title.setText(head.getTitle());

        /* Tags. */
        if (head.hasTags()) {
            tagsView.setText(TextUtils.join(" ", head.getTags()));
        } else {
            tagsView.setText(null);
        }

        /* Times. */
        updateTimestampView(TimeType.SCHEDULED, head.getScheduled());
        updateTimestampView(TimeType.DEADLINE, head.getDeadline());
        updateTimestampView(TimeType.CLOSED, head.getClosed());

        /* Properties. */
        propertiesContainer.removeAllViews();
        if (head.hasProperties()) {
            OrgProperties properties = head.getProperties();
            for (String name: properties.keySet()) {
                String value = properties.get(name);
                addPropertyToList(name, value);
            }
        }
        addPropertyToList(null, null);

        /* Content. */
        bodyEdit.setText(head.getContent());
        bodyView.setRawText(head.getContent());
    }

    private void addPropertyToList(String propName, String propValue) {
        View.inflate(getActivity(), R.layout.fragment_note_property, propertiesContainer);

        final ViewGroup propView = lastProperty();

        final EditText name  = propView.findViewById(R.id.name);
        final EditText value = propView.findViewById(R.id.value);
        final View delete = propView.findViewById(R.id.delete);

        if (propName != null && propValue != null) { // Existing property
            name.setText(propName);
            value.setText(propValue);
        }

        delete.setOnClickListener(view -> {
            if (isOnlyProperty(propView) || isLastProperty(propView)) {
                name.setText(null);
                value.setText(null);
            } else {
                propertiesContainer.removeView(propView);
            }
        });

        /*
         * Add new empty property if last one is being edited.
         */
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isLastProperty(propView) && !TextUtils.isEmpty(s)) {
                    addPropertyToList(null, null);
                }
            }
        };

        name.addTextChangedListener(textWatcher);
        value.addTextChangedListener(textWatcher);
    }

    private boolean isLastProperty(View view) {
        return lastProperty() == view;
    }

    private boolean isOnlyProperty(View view) {
        return propertiesContainer.getChildCount() == 1 && propertiesContainer.getChildAt(0) == view;
    }

    private ViewGroup lastProperty() {
        return (ViewGroup) propertiesContainer.getChildAt(propertiesContainer.getChildCount() - 1);
    }

    /**
     * Views -> Note  (Only those fields which are not updated when views are.)
     */
    private void updateNoteFromViews() {
        OrgHead head = note.getHead();

        head.setState(TextUtils.isEmpty(state.getText()) ? null : state.getText().toString());

        head.setPriority(TextUtils.isEmpty(priority.getText()) ? null : priority.getText().toString());

        /* Replace new lines with spaces, in case multi-line text has been pasted. */
        head.setTitle(title.getText().toString().replaceAll("\n", " ").trim());

        head.setTags(tagsView.getText().toString().split("\\s+"));

        /* Add properties. */
        head.removeProperties();
        for (int i = 0; i < propertiesContainer.getChildCount(); i++) {
            View property = propertiesContainer.getChildAt(i);

            CharSequence name = ((TextView) property.findViewById(R.id.name)).getText();
            CharSequence value = ((TextView) property.findViewById(R.id.value)).getText();

            if (!TextUtils.isEmpty(name)) { // Ignore property with no name
                head.addProperty(name.toString(), value.toString());
            }
        }

        head.setContent(bodyEdit.getText().toString());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, view, savedInstanceState);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        book = mShelf.getBook(mBookId); // FIXME: ANR reported

        if (book != null) {
            locationView.setText(BookUtils.getFragmentTitleForBook(book));
            locationButtonView.setText(BookUtils.getFragmentTitleForBook(book));
        }

        if (mIsNew) { /* Creating new note. */
            note = new Note();
            note.getPosition().setBookId(mBookId);

            updateNewNoteValues();

            mViewFlipper.setDisplayedChild(0);

            editSwitch.setChecked(true);

            /* Open keyboard for new notes, unless fragment was given
             * some initial values (for example from ShareActivity).
             */
            if (TextUtils.isEmpty(mInitialTitle) && TextUtils.isEmpty(mInitialContent)) {
                ActivityUtils.openSoftKeyboard(getActivity(), title);
            }

        } else { /* Get existing note from database. */
            note = mShelf.getNote(mNoteId);

            if (note != null) {
                // TODO: Cleanup: getNote(id, withProperties) or such
                note.getHead().setProperties(mShelf.getNoteProperties(mNoteId));

                mViewFlipper.setDisplayedChild(0);

                // If there is no content, start in edit mode
                if (!note.getHead().hasContent()) {
                    editSwitch.setChecked(true);
                }

            } else {
                mViewFlipper.setDisplayedChild(1);
            }
        }

        if (note != null) {
            /* Get current values from saved Bundle and populate all views. */
            if (savedInstanceState != null) {
                updateNoteFromBundle(savedInstanceState);
            }

            /* Update views from note. */
            updateViewsFromNote();
        }

        /* Store the hash value of original note. */
        if (!getArguments().containsKey(ARG_ORIGINAL_NOTE_HASH) && note != null) {
            getArguments().putLong(ARG_ORIGINAL_NOTE_HASH, noteHash(note));
        }

        /* Refresh action bar items (hide or display, depending on if book is loaded. */
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    /**
     * Set adapter for tags view for auto-complete.
     */
    private void setupTagsViewAdapter() {
        App.EXECUTORS.getDiskIO().execute(() -> {
            String[] knownTags = mShelf.getAllTags(0);

            Context context = getContext();

            if (context != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.dropdown_item, knownTags);

                App.EXECUTORS.getMainThread().execute(() -> {
                    tagsView.setAdapter(adapter);
                    tagsView.setTokenizer(new SpaceTokenizer());
                });
            }
        });
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        announceChangesToActivity();

        setMetadataVisibility();
    }

    private void announceChangesToActivity() {
        if (mListener != null) {
            mListener.announceChanges(
                    NoteFragment.FRAGMENT_TAG,
                    BookUtils.getFragmentTitleForBook(book),
                    BookUtils.getFragmentSubtitleForBook(getContext(), book),
                    0);
        }
    }

    @Override
    public void onDestroyView() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDestroyView();

        mViewFlipper.setDisplayedChild(0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, outState);
        super.onSaveInstanceState(outState);

        if (note != null) {
            updateNoteFromViews();
            updateBundleFromNote(outState);
        }
    }

    @Override
    public void onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDetach();

        mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void parseArguments() {
        if (getArguments() == null) {
            throw new IllegalArgumentException("No arguments found to " + NoteFragment.class.getSimpleName());
        }

        mIsNew = getArguments().getBoolean(ARG_IS_NEW);

        /* Book ID must exist. */
        if (! getArguments().containsKey(ARG_BOOK_ID)) {
            throw new IllegalArgumentException(NoteFragment.class.getSimpleName() + " requires " + ARG_BOOK_ID + " argument passed");
        }

        mBookId = getArguments().getLong(ARG_BOOK_ID);

//        /* Book ID must be valid. */
//        if (mBookId <= 0) {
//            throw new IllegalArgumentException("Passed argument book id is not valid (" + mBookId + ")");
//        }

        /* Note ID might or might not be passed - it depends if note is being edited or created. */
        if (getArguments().containsKey(ARG_NOTE_ID)) {
            mNoteId = getArguments().getLong(ARG_NOTE_ID);

            /* Note ID must be valid if it exists. */
            if (mNoteId <= 0) {
                throw new IllegalArgumentException("Note id is " + mNoteId);
            }
        }

        /* Location (used for new notes). */
        place = Place.valueOf(getArguments().getString(ARG_PLACE));

        mInitialTitle = getArguments().getString(ARG_TITLE);
        mInitialContent = getArguments().getString(ARG_CONTENT);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Arguments parsed for " + this);
    }

    public boolean isAskingForConfirmationForModifiedNote() {
        /* It's possible that note does not exist
         * if it has been deleted and the user went back to it.
         */
        if (note != null && isNoteModified()) {
            dialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.note_has_been_modified)
                    .setMessage(R.string.discard_or_save_changes)
                    .setPositiveButton(R.string.save, (dialog, which) -> save())
                    .setNegativeButton(R.string.discard, (dialog, which) -> cancel())
                    .setNeutralButton(R.string.cancel, null)
                    .create();
            dialog.show();

            return true;

        } else {
            return false;
        }
    }

    public boolean isNoteModified() {

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Pre: State from view tag: " + state.getText());
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Pre: State from note    : " + note.getHead().getState());

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Pre: Prio from view tag: " + priority.getText());
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Pre: Prio from note    : " + note.getHead().getPriority());

        updateNoteFromViews();

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Post: State from view tag: " + state.getText());
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Post: State from note    : " + note.getHead().getState());

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Post: Prio from view tag: " + priority.getText());
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Post: Prio from note    : " + note.getHead().getPriority());


        long currentHash = noteHash(note);
        long originalHash = getArguments().getLong(ARG_ORIGINAL_NOTE_HASH);

        return currentHash != originalHash;
    }

    /**
     * Hash used to detect note modifications.
     */
    private long noteHash(Note note) {
        OrgParserWriter parserWriter = new OrgParserWriter();
        String head = parserWriter.whiteSpacedHead(note.getHead(), note.getPosition().getLevel(), false);

        return MiscUtils.sha1(head);
    }

    private enum TimeType { SCHEDULED, DEADLINE, CLOSED, CLOCKED }

    private void updateTimestampView(TimeType timeType, OrgRange range) {
        switch (timeType) {
            case SCHEDULED:
                if (range != null) {
                    scheduledButton.setText(mUserTimeFormatter.formatAll(range));
                } else {
                    scheduledButton.setText(null);
                }
                break;

            case DEADLINE:
                if (range != null) {
                    deadlineButton.setText(mUserTimeFormatter.formatAll(range));
                } else {
                    deadlineButton.setText(null);
                }
                break;

            case CLOSED:
                /*
                 * Do not display CLOSED button if it's not set.
                 * It will be updated on state change.
                 */
                if (range != null) {
                    closedText.setText(mUserTimeFormatter.formatAll(range));
                    closedTimeContainer.setVisibility(View.VISIBLE);
                } else {
                    closedTimeContainer.setVisibility(View.GONE);
                }
                break;
        }
    }

    /**
     * Set new Note's initial values.
     */
    private void updateNewNoteValues() {
        OrgHead head = note.getHead();

        /* Set scheduled time for a new note. */
        if (AppPreferences.isNewNoteScheduled(getContext())) {
            Calendar cal = Calendar.getInstance();

            OrgDateTime timestamp = new OrgDateTime.Builder()
                    .setIsActive(true)
                    .setYear(cal.get(Calendar.YEAR))
                    .setMonth(cal.get(Calendar.MONTH))
                    .setDay(cal.get(Calendar.DAY_OF_MONTH))
                    .build();

            OrgRange time = new OrgRange(timestamp);

            head.setScheduled(time);
        }

        /* Set state for a new note. */
        String stateKeyword = AppPreferences.newNoteState(getContext());
        if (NoteStates.isKeyword(stateKeyword)) {
            head.setState(stateKeyword);
        } else {
            head.setState(null);
        }

        /* Initial title. */
        if (mInitialTitle != null) {
            head.setTitle(mInitialTitle);
        }

        StringBuilder content = new StringBuilder();

        /* Initial content. */
        if (mInitialContent != null) {
            if (content.length() > 0) {
                content.append("\n\n");
            }
            content.append(mInitialContent);
        }

        if (content.length() > 0) {
            head.setContent(content.toString());
        }
    }

    @Override
    public void onClick(View view) {
        DialogFragment f = null;

        switch (view.getId()) {
            case R.id.fragment_note_location_button:
                List<Book> books = getBooksList();

                String[] bookNames = new String[books.size()];
                for (int i = 0; i < books.size(); i++) {
                    bookNames[i] = books.get(i).getName();
                }

                int selected = getSelectedBook(books, mBookId);

                dialog = new AlertDialog.Builder(getContext())
                        .setTitle(R.string.state)
                        .setSingleChoiceItems(bookNames, selected, (dialog, which) -> {
                            Book book = books.get(which);

                            if (book != null) {
                                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Setting book for fragment", book);
                                setBook(book);
                            }


                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create();

                dialog.show();

                break;

            case R.id.fragment_note_state_button:
                NoteStates states = NoteStates.fromPreferences(getContext());

                String[] keywords = states.getArray();

                int currentState = -1;
                if (!TextUtils.isEmpty(state.getText())) {
                    currentState = states.indexOf(state.getText().toString());
                }

                dialog = new AlertDialog.Builder(getContext())
                        .setTitle(R.string.state)
                        .setSingleChoiceItems(keywords, currentState, (dialog, which) -> {
                            // On state change - update state and timestamps
                            setState(note, states.get(which));
                            dialog.dismiss();
                        })
                        .setNeutralButton(R.string.clear, (dialog, which) -> {
                            // On state change - update state and timestamps
                            setState(note, NoteStates.NO_STATE_KEYWORD);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create();

                dialog.show();

                break;

            case R.id.fragment_note_priority_button:
                NotePriorities priorities = NotePriorities.fromPreferences(getContext());

                keywords = priorities.getArray();

                int currentPriority = -1;
                if (!TextUtils.isEmpty(priority.getText())) {
                    currentPriority = priorities.indexOf(priority.getText().toString());
                }

                dialog = new AlertDialog.Builder(getContext())
                        .setTitle(R.string.priority)
                        .setSingleChoiceItems(keywords, currentPriority, (dialog, which) -> {
                            setPriorityView(priorities.get(which));
                            dialog.dismiss();
                        })
                        .setNeutralButton(R.string.clear, (dialog, which) -> setPriorityView(null))
                        .setNegativeButton(R.string.cancel, null)
                        .create();

                dialog.show();

                break;

            /* Setting scheduled time. */
            case R.id.fragment_note_scheduled_button:
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_scheduled_button,
                        R.string.schedule,
                        note.getId(),
                        note.getHead().getScheduled() != null ? note.getHead().getScheduled().getStartTime() : null);
                break;

            /* Setting deadline time. */
            case R.id.fragment_note_deadline_button:
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_deadline_button,
                        R.string.deadline,
                        note.getId(),
                        note.getHead().getDeadline() != null ? note.getHead().getDeadline().getStartTime() : null);
                break;

            /* Setting closed time. */
            case R.id.fragment_note_closed_edit_text:
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_closed_edit_text,
                        R.string.closed,
                        note.getId(),
                        note.getHead().getClosed() != null ? note.getHead().getClosed().getStartTime() : null);
                break;
        }

        if (f != null) {
            f.show(getChildFragmentManager(), TimestampDialogFragment.FRAGMENT_TAG);
        }
    }

    private List<Book> getBooksList() {
        List<Book> books = mShelf.getBooks();

        if (books.size() == 0) {
            try {
                Book book = mShelf.createBook(AppPreferences.shareNotebook(getContext()));
                books.add(book);
            } catch (IOException e) {
                // TODO: Test and handle better.
                e.printStackTrace();
            }
        }

        return books;
    }

    private Book createDefaultBook() {
        try {
            return mShelf.createBook(AppPreferences.shareNotebook(getContext()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getSelectedBook(List<Book> books, long bookId) {
        int selected = -1;

        if (bookId != -1) {
            for (int i = 0; i < books.size(); i++) {
                if (books.get(i).getId() == bookId) {
                    selected = i;
                    break;
                }
            }

        } else {
            String defaultBookName = AppPreferences.shareNotebook(getContext());
            for (int i = 0; i < books.size(); i++) {
                if (defaultBookName.equals(books.get(i).getName())) {
                    selected = i;
                    break;
                }
            }
        }

        return selected;
    }

    @Override /* TimestampDialog */
    public void onDateTimeSet(int id, TreeSet<Long> noteIds, OrgDateTime time) {
        if (note == null) {
            return;
        }

        OrgHead head = note.getHead();
        OrgRange range = new OrgRange(time);

        switch (id) {
            case R.id.fragment_note_scheduled_button:
                updateTimestampView(TimeType.SCHEDULED, range);
                head.setScheduled(range);
                break;

            case R.id.fragment_note_deadline_button:
                updateTimestampView(TimeType.DEADLINE, range);
                head.setDeadline(range);
                break;

            case R.id.fragment_note_closed_edit_text:
                updateTimestampView(TimeType.CLOSED, range);
                head.setClosed(range);
                break;
        }
    }

    @Override /* TimestampDialog */
    public void onDateTimeCleared(int id, TreeSet<Long> noteIds) {
        if (note == null) {
            return;
        }

        OrgHead head = note.getHead();

        switch (id) {
            case R.id.fragment_note_scheduled_button:
                updateTimestampView(TimeType.SCHEDULED, null);
                head.setScheduled(null);
                break;

            case R.id.fragment_note_deadline_button:
                updateTimestampView(TimeType.DEADLINE, null);
                head.setDeadline(null);
                break;

            case R.id.fragment_note_closed_edit_text:
                updateTimestampView(TimeType.CLOSED, null);
                head.setClosed(null);
                break;
        }
    }

    @Override /* TimestampDialog */
    public void onDateTimeAborted(int id, TreeSet<Long> noteIds) {
    }

    /*
     * Options Menu.
     */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater);

        inflater.inflate(R.menu.note_actions, menu);

        /* Remove search item. */
        menu.removeItem(R.id.activity_action_search);

        if (note == null) { // Displaying non-existent note.
            menu.removeItem(R.id.close);
            menu.removeItem(R.id.done);
            menu.removeItem(R.id.hide_metadata);
            menu.removeItem(R.id.delete);

        } else {
            String metadataVisibility = AppPreferences.noteMetadataVisibility(getContext());

            if ("all".equals(metadataVisibility)) {
                menu.findItem(R.id.metadata_show_all).setChecked(true);
            } else if ("none".equals(metadataVisibility)) {
                menu.findItem(R.id.metadata_show_none).setChecked(true);
            } else {
                menu.findItem(R.id.metadata_show_selected).setChecked(true);
            }

            menu.findItem(R.id.metadata_always_show_set)
                    .setChecked(AppPreferences.alwaysShowSetNoteMetadata(getContext()));
        }

        /* Newly created note cannot be deleted. */
        if (mIsNew) {
            menu.removeItem(R.id.delete);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item);

        switch (item.getItemId()) {
            case R.id.done:
                save();
                return true;

            case R.id.close:
                if (!isAskingForConfirmationForModifiedNote()) {
                    cancel();
                }
                return true;

            case R.id.metadata_show_all:
                item.setChecked(true);
                AppPreferences.noteMetadataVisibility(getContext(), "all");
                setMetadataVisibility();
                return true;

            case R.id.metadata_show_selected:
                item.setChecked(true);
                AppPreferences.noteMetadataVisibility(getContext(), "selected");
                setMetadataVisibility();
                return true;

            case R.id.metadata_show_none:
                item.setChecked(true);
                AppPreferences.noteMetadataVisibility(getContext(), "none");
                setMetadataVisibility();
                return true;

            case R.id.metadata_always_show_set:
                item.setChecked(!item.isChecked());
                AppPreferences.alwaysShowSetNoteMetadata(getContext(), item.isChecked());
                setMetadataVisibility();
                return true;

            case R.id.delete:
                delete();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setMetadataVisibility() {
        setMetadataVisibility(
                "tags",
                tagsContainer,
                !TextUtils.isEmpty(tagsView.getText()));

        setMetadataVisibility(
                "state",
                stateContainer,
                !TextUtils.isEmpty(state.getText()));

        setMetadataVisibility(
                "priority",
                priorityContainer,
                !TextUtils.isEmpty(priority.getText()));

        setMetadataVisibility(
                "scheduled_time",
                scheduledTimeContainer,
                !TextUtils.isEmpty(scheduledButton.getText()));

        setMetadataVisibility(
                "deadline_time",
                deadlineTimeContainer,
                !TextUtils.isEmpty(deadlineButton.getText()));

        setMetadataVisibility(
                "properties",
                propertiesContainer,
                propertiesContainer.getChildCount() > 1);

        setMetadataVisibility(
                null,
                lastProperty(),
                false);
    }

    private void setMetadataVisibility(String name, View container, boolean isSet) {
        Context context = getContext();

        if (context != null) {
            String visibility = AppPreferences.noteMetadataVisibility(context);
            Set<String> selected = AppPreferences.selectedNoteMetadata(context);
            boolean showSet = AppPreferences.alwaysShowSetNoteMetadata(context);

            boolean isVisible = "all".equals(visibility)
                                || ("selected".equals(visibility) && name != null && selected.contains(name))
                                || (showSet && isSet);

            container.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void delete() {
        dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.delete_note)
                .setMessage(R.string.delete_note_and_all_subnotes)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (mListener != null) {
                        mListener.onNoteDeleteRequest(note);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> { })
                .create();
        dialog.show();
    }

    private void cancel() {
        if (mListener != null) {
            mListener.onNoteCancelRequest(note);
        }
    }

    private void save() {
        /* Make sure notebook is set. */
        if (note.getPosition().getBookId() == 0) {
            Activity activity = getActivity();
            if (activity != null) {
                ((CommonActivity) activity).showSnackbar(R.string.note_book_not_set);
            }
            return;
        }

        if (updateNoteFromViewsAndVerify()) {
            if (mListener != null) {
                if (mIsNew) { // New note
                    mListener.onNoteCreateRequest(note, place != Place.UNSPECIFIED ?
                            new NotePlace(note.getPosition().getBookId(), mNoteId, place) : null);

                } else { // Existing note
                    if (isNoteModified()) {
                        mListener.onNoteUpdateRequest(note);
                    } else {
                        mListener.onNoteCancelRequest(note);
                    }
                }
            }
        }
    }

    private boolean updateNoteFromViewsAndVerify() {
        updateNoteFromViews();
        return isTitleValid();
    }

    private boolean isTitleValid() {
        if (TextUtils.isEmpty(note.getHead().getTitle())) {
            CommonActivity.showSnackbar(getContext(), getString(R.string.title_can_not_be_empty));
            return false;
        } else {
            return true;
        }
    }

    /**
     * Updates the current book this note belongs to. Only makes sense for new notes.
     * TODO: Should be setPosition and allow filing under specific note
     */
    public void setBook(Book newBook) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, book);

        book = newBook;
        mBookId = book.getId();

        note.getPosition().setBookId(mBookId);

        locationView.setText(BookUtils.getFragmentTitleForBook(book));
        locationButtonView.setText(BookUtils.getFragmentTitleForBook(book));

        getArguments().putLong(ARG_BOOK_ID, book.getId());
    }

    public long getNoteId() {
        return mNoteId;
    }

    /**
     * Update state, timestamps, last-repeat and logbook.
     */
    private void setState(Note note, String state) {
        updateNoteFromViews();

        String originalState = note.getHead().getState();

        Set<String> doneKeywords = AppPreferences.doneKeywordsSet(getContext());

        StateChangeLogic stateChangeLogic = new StateChangeLogic(doneKeywords);

        stateChangeLogic.setState(
                state,
                note.getHead().getState(),
                note.getHead().getScheduled(),
                note.getHead().getDeadline());

        note.getHead().setState(stateChangeLogic.getState());

        note.getHead().setScheduled(stateChangeLogic.getScheduled());
        note.getHead().setDeadline(stateChangeLogic.getDeadline());
        note.getHead().setClosed(stateChangeLogic.getClosed());

        if (stateChangeLogic.isShifted()) {
            String datetime = new OrgDateTime(false).toString();

            if (AppPreferences.setLastRepeatOnTimeShift(getContext())) {
                note.getHead().addProperty(OrgFormatter.LAST_REPEAT_PROPERTY, datetime);
            }

            if (AppPreferences.logOnTimeShift(getContext())) {
                String logEntry = OrgFormatter.stateChangeLine(originalState, state, datetime);
                String content = OrgFormatter.insertLogbookEntryLine(note.getHead().getContent(), logEntry);
                note.getHead().setContent(content);
            }
        }

        updateViewsFromNote();
    }

    private void setStateView(String state) {
        if (state == null || NoteStates.NO_STATE_KEYWORD.equals(state)) {
            this.state.setText(null);
        } else {
            this.state.setText(state);
        }
    }

    private void setPriorityView(String priority) {
        this.priority.setText(priority);
    }

    /**
     * Mark note's book in the drawer.
     */
    @NonNull
    @Override
    public String getCurrentDrawerItemId() {
        return BookFragment.getDrawerItemId(mBookId);
    }

    public interface NoteFragmentListener extends FragmentListener {
        void onNoteCreateRequest(Note note, NotePlace notePlace);
        void onNoteUpdateRequest(Note note);
        void onNoteCancelRequest(Note note);
        void onNoteDeleteRequest(Note note);
    }
}
