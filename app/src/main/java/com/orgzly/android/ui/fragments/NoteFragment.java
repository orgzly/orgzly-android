package com.orgzly.android.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.Broadcasts;
import com.orgzly.android.Note;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.NotePrioritySpinner;
import com.orgzly.android.ui.Place;
import com.orgzly.android.ui.NoteStateSpinner;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.dialogs.TimestampDialogFragment;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.NoteContentParser;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.android.util.SpaceTokenizer;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.org.OrgProperty;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;
import com.orgzly.org.OrgHead;
import com.orgzly.org.utils.StateChangeLogic;
import com.orgzly.org.parser.OrgParserWriter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.TreeSet;

/**
 * Note editor.
 */
public class NoteFragment extends Fragment
        implements
        View.OnClickListener,
        TimestampDialogFragment.OnDateTimeSetListener {

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

    private Note mNote;
    private Book mBook;


    private ScrollView scrollView;

    private NoteStateSpinner mState;
    private NotePrioritySpinner mPriority;

    private TextInputLayout titleInputLayout;
    private EditText mTitleView;
    private MultiAutoCompleteTextView mTagsView;

    private Button mScheduledButton;
    private Button mDeadlineButton;
    private Button mClosedButton;

    private LinearLayout propertyList;
    private Button addProperty;

    private ToggleButton editSwitch;
    private EditText bodyEdit;
    private TextView bodyView;

    /** Used to switch to note-does-not-exist view, if the note has been deleted. */
    private ViewFlipper mViewFlipper;

    private UserTimeFormatter mUserTimeFormatter;

    public static NoteFragment getInstance(boolean isNew, long bookId, long noteId, Place place, String initialTitle, String initialContent) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();

        args.putBoolean(ARG_IS_NEW, isNew);

        args.putLong(ARG_BOOK_ID, bookId);
        if (noteId > 0) args.putLong(ARG_NOTE_ID, noteId);
        args.putString(ARG_PLACE, place.toString());

        if (initialTitle   != null) args.putString(ARG_TITLE, initialTitle);
        if (initialContent != null) args.putString(ARG_CONTENT, initialContent);

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

    private boolean mStateSpinnerReady = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        final View top = inflater.inflate(R.layout.fragment_note, container, false);


        scrollView = (ScrollView) top.findViewById(R.id.fragment_note_container);

        mPriority = new NotePrioritySpinner(getActivity(), (Spinner) top.findViewById(R.id.fragment_note_priority));
        mState = new NoteStateSpinner(getActivity(), (Spinner) top.findViewById(R.id.fragment_note_state));

        /*
         * Act after state change only if there was a touch (ie user clicked on the spinner).
         */
        mState.getSpinner().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                    mStateSpinnerReady = true;
                    // Load your spinner here
                }
                return false;
            }
        });

        /*
         * On state change - update state and timestamps.
         *
         * There could be issues with onItemSelected called on initialization, not as a result
         * of user selection, which is why mStateSpinnerReady is being used.
         *
         * http://stackoverflow.com/questions/2562248/android-how-to-keep-onitemselected-from-firing-off-on-a-newly-instantiated-spin
         */
        mState.getSpinner().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mStateSpinnerReady) {
                    String state = parent.getItemAtPosition(position).toString();
                    updateNoteForStateChange(getActivity(), mNote, state);
                }

                mStateSpinnerReady = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        titleInputLayout = (TextInputLayout) top.findViewById(R.id.fragment_note_title_input_layout);
        mTitleView = (EditText) top.findViewById(R.id.fragment_note_title);
        MiscUtils.clearErrorOnTextChange(mTitleView, titleInputLayout);

        /*
         * Only works when set from code.
         * We want imeOptions="actionDone", so we can't use textMultiLine.
         */
        mTitleView.setHorizontallyScrolling(false);
        mTitleView.setMaxLines(3);

        mTitleView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                save();
                return true;
            }
        });

        mTagsView = (MultiAutoCompleteTextView) top.findViewById(R.id.fragment_note_tags);

        /* Hint causes minimum width - when tags' width is smaller then hint's, there is empty space. */
        mTagsView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(mTagsView.getText().toString())) {
                    mTagsView.setHint("");
                } else {
                    mTagsView.setHint(R.string.fragment_note_tags_hint);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mScheduledButton = (Button) top.findViewById(R.id.fragment_note_scheduled_button);
        mScheduledButton.setOnClickListener(this);

        mDeadlineButton = (Button) top.findViewById(R.id.fragment_note_deadline_button);
        mDeadlineButton.setOnClickListener(this);

        mClosedButton = (Button) top.findViewById(R.id.fragment_note_closed_button);
        mClosedButton.setOnClickListener(this);

        propertyList = (LinearLayout) top.findViewById(R.id.property_list);
        addProperty = (Button) top.findViewById(R.id.add_property);
        addProperty.setOnClickListener(this);

        bodyEdit = (EditText) top.findViewById(R.id.body_edit);

        bodyView = (TextView) top.findViewById(R.id.body_view);

//        bodyView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                editMode(true, true);
//                return false;
//            }
//        });

//        bodyView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                editMode(true, true);
//            }
//        });

        if (getActivity() != null && AppPreferences.isFontMonospaced(getContext())) {
            bodyEdit.setTypeface(Typeface.MONOSPACE);
            bodyView.setTypeface(Typeface.MONOSPACE);
        }

        editSwitch = (ToggleButton) top.findViewById(R.id.edit_content_toggle);

        editSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, buttonView, isChecked);

                if (isChecked) {
                    bodyView.setVisibility(View.GONE);

                    bodyEdit.setVisibility(View.VISIBLE);

                } else {
                    bodyEdit.setVisibility(View.GONE);

                    bodyView.setText(NoteContentParser.fromOrg(bodyEdit.getText().toString()));
                    bodyView.setVisibility(View.VISIBLE);

                    ActivityUtils.closeSoftKeyboard(getActivity());
                }

            }
        });

        editSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, editSwitch.isChecked());

                if (editSwitch.isChecked()) { // Clicked to edit content
                    ActivityUtils.openSoftKeyboard(getActivity(), bodyEdit);

//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            scrollView.requestChildFocus(bodyEdit, bodyEdit);
//                        }
//                    }, 500);

                } else { // Clicked to finish editing content
                    scrollView.smoothScrollTo(0, 0);
                }
            }
        });


        mViewFlipper = (ViewFlipper) top.findViewById(R.id.fragment_note_view_flipper);

        return top;
    }

    /**
     * Note -> Bundle
     */
    private void updateBundleFromNote(Bundle outState) {
        OrgHead head = mNote.getHead();

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

        /* Store properties as an array of strings: name1 value1 name2 value2 ... */
        if (head.hasProperties()) {
            ArrayList<String> array = new ArrayList<>();
            for (OrgProperty property: head.getProperties()) {
                array.add(property.getName());
                array.add(property.getValue());
            }
            outState.putStringArrayList(ARG_CURRENT_PROPERTIES, array);

        } else {
            outState.remove(ARG_CURRENT_PROPERTIES);
        }

        outState.putString(ARG_CURRENT_CONTENT, head.getContent());
    }

    /**
     * Bundle -> Note
     */
    private void updateNoteFromBundle(Bundle savedInstanceState) {
        OrgHead head = mNote.getHead();

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
            for (int i = 0; i < array.size(); i += 2) {
                head.addProperty(new OrgProperty(array.get(i), array.get(i + 1)));
            }
        }

        head.setContent(savedInstanceState.getString(ARG_CURRENT_CONTENT));
    }

    /**
     * Note -> Views
     */
    private void updateViewsFromNote() {
        OrgHead head = mNote.getHead();

        /* State. */
        mState.setCurrentValue(head.getState());

        /* Priority. */
        mPriority.setCurrentValue(head.getPriority());

        /* Title. */
        mTitleView.setText(head.getTitle());

        /* Tags. */
        if (head.hasTags()) {
            mTagsView.setText(TextUtils.join(" ", head.getTags()));
        } else {
            mTagsView.setText(null);
        }

        /* Times. */
        updateTimestampView(TimeType.SCHEDULED, mScheduledButton, head.getScheduled());
        updateTimestampView(TimeType.DEADLINE, mDeadlineButton, head.getDeadline());
        updateTimestampView(TimeType.CLOSED, mClosedButton, head.getClosed());

        /* Properties. */
        propertyList.removeAllViews();
        if (head.hasProperties()) {
            for (OrgProperty property: head.getProperties()) {
                addPropertyToList(property);
            }
        }

        /* Content. */
        bodyEdit.setText(head.getContent());
        bodyView.setText(NoteContentParser.fromOrg(head.getContent()));
    }

    private void addPropertyToList(OrgProperty property) {
        final ViewGroup propView = (ViewGroup) View.inflate(getActivity(), R.layout.note_property, null);

        final TextView name  = (TextView) propView.findViewById(R.id.name);
        final TextView value = (TextView) propView.findViewById(R.id.value);
        final View delete = propView.findViewById(R.id.delete);

        if (property != null) { // Existing property
            name.setText(property.getName());
            value.setText(property.getValue());

        } else { // User creating new property
            Activity activity = getActivity();
            if (activity != null) {
                ActivityUtils.openSoftKeyboard(activity, name);
            }
        }

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                propertyList.removeView(propView);
            }
        });

        propertyList.addView(propView);
    }

    /**
     * Views -> Note  (Only those fields which are not updated when views are.)
     */
    private void updateNoteFromViews() {
        OrgHead head = mNote.getHead();

        head.setState(mState.getCurrentValue());
        head.setPriority(mPriority.getCurrentValue());

        /* Replace new lines with spaces, in case multi-line text has been pasted. */
        head.setTitle(mTitleView.getText().toString().replaceAll("\n", " ").trim());

        head.setTags(mTagsView.getText().toString().split("\\s+"));

        /* Add properties. */
        head.removeProperties();
        for (int i = 0; i < propertyList.getChildCount(); i++){
            View property = propertyList.getChildAt(i);

            CharSequence name = ((TextView) property.findViewById(R.id.name)).getText();
            CharSequence value = ((TextView) property.findViewById(R.id.value)).getText();

            if (!TextUtils.isEmpty(name)) { // Ignore property with no name
                head.addProperty(new OrgProperty(name.toString(), value.toString()));
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

        setupTagsView();

        if (mBookId != 0) {
            mBook = mShelf.getBook(mBookId);
        }

        if (mIsNew) { /* Creating new note. */
            mNote = new Note();
            mNote.getPosition().setBookId(mBookId);

            updateNewNoteValues();

            mViewFlipper.setDisplayedChild(0);

            editSwitch.setChecked(true);

            /* Open keyboard for new notes, unless fragment was given
             * some initial values (for example from ShareActivity).
             */
            if (TextUtils.isEmpty(mInitialTitle) && TextUtils.isEmpty(mInitialContent)) {
                ActivityUtils.openSoftKeyboard(getActivity(), mTitleView);
            }

        } else { /* Get existing note from database. */
            // TODO: Cleanup: getNote(id, withProperties) or such
            try {
                mNote = mShelf.getNote(mNoteId);
                mNote.getHead().setProperties(mShelf.getNoteProperties(mNoteId));
                mViewFlipper.setDisplayedChild(0);
            } catch (NoSuchElementException e) {
                mNote = null;
                mViewFlipper.setDisplayedChild(1);
            }
        }

        if (mNote != null) {
            /* Get current values from saved Bundle and populate all views. */
            if (savedInstanceState != null) {
                updateNoteFromBundle(savedInstanceState);
            }

            /* Update views from note. */
            updateViewsFromNote();
        }

        /* Store the hash value of original note. */
        if (!getArguments().containsKey(ARG_ORIGINAL_NOTE_HASH) && mNote != null) {
            getArguments().putLong(ARG_ORIGINAL_NOTE_HASH, noteHash(mNote));
        }

        /* Refresh action bar items (hide or display, depending on if book is loaded. */
        if (getActivity() != null) {
            getActivity().supportInvalidateOptionsMenu();
        }
    }

    /*
     * Auto-complete tags using all known tags from database.
     */
    private void setupTagsView() {
        /* Collect all known tags. */
        String[] knownTags = mShelf.getAllTags(0);

        /* White text on light gray background, when using android.R.layout.simple_dropdown_item_1line
         * See https://code.google.com/p/android/issues/detail?id=5237#c8
         */
        // ArrayAdapter <String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, knownTags);
        ArrayAdapter <String> adapter = new ArrayAdapter<>(getActivity(), R.layout.dropdown_item, knownTags);

        mTagsView.setAdapter(adapter);

        mTagsView.setTokenizer(new SpaceTokenizer());
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        announceChangesToActivity();

        mState.updatePossibleValues(getActivity().getApplicationContext());
        mPriority.updatePossibleValues(getActivity().getApplicationContext());
    }

    private void announceChangesToActivity() {
        if (mListener != null) {
            mListener.announceChanges(
                    NoteFragment.FRAGMENT_TAG,
                    Book.getFragmentTitleForBook(mBook),
                    Book.getFragmentSubtitleForBook(mBook),
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

        if (mNote != null) {
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
        if (mNote != null && isNoteModified()) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.note_has_been_modified)
                    .setMessage(R.string.discard_or_save_changes)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            save();
                        }
                    })
                    .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancel();
                        }
                    })
                    .setNeutralButton(R.string.cancel, null)
                    .create()
                    .show();

            return true;

        } else {
            return false;
        }
    }

    public boolean isNoteModified() {
        updateNoteFromViews();

        long currentHash = noteHash(mNote);
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

    private void updateTimestampView(TimeType timeType, TextView button, OrgRange range) {
        switch (timeType) {
            case SCHEDULED:
                if (range != null) {
                    button.setText(mUserTimeFormatter.formatAll(range));
                } else {
                    button.setText(getString(R.string.schedule_button_hint));
                }
                break;

            case CLOSED:
                /*
                 * Do not display CLOSED button if it's not set.
                 * It will be updated on state change.
                 */
                if (range != null) {
                    button.setText(mUserTimeFormatter.formatAll(range));
                    button.setVisibility(View.VISIBLE);
                } else {
                    button.setVisibility(View.GONE);
                }
                break;

            case DEADLINE:
                if (range != null) {
                    button.setText(mUserTimeFormatter.formatAll(range));
                } else {
                    button.setText(getString(R.string.deadline_button_hint));
                }
                break;
        }
    }

    /**
     * Set new Note's initial values.
     */
    private void updateNewNoteValues() {
        OrgHead head = mNote.getHead();

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
        if (NoteStateSpinner.isSet(stateKeyword)) {
            head.setState(stateKeyword);
        } else {
            head.setState(null);
        }

        /* Initial title. */
        if (mInitialTitle != null) {
            head.setTitle(mInitialTitle);
        }

        /* Content. */

        StringBuilder content = new StringBuilder();

        /* Prepend content with created-at property. */
        if (AppPreferences.createdAt(getContext())) {
            String propertyName = AppPreferences.createdAtProperty(getContext());
            String time = new OrgDateTime(false).toString(); /* Inactive time. */

            head.addProperty(new OrgProperty(propertyName, time));
        }

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
            /* Setting scheduled time. */
            case R.id.fragment_note_scheduled_button:
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_scheduled_button,
                        R.string.schedule,
                        mNote.getId(),
                        mNote.getHead().getScheduled() != null ? mNote.getHead().getScheduled().getStartTime() : null);
                break;

            /* Setting deadline time. */
            case R.id.fragment_note_deadline_button:
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_deadline_button,
                        R.string.deadline,
                        mNote.getId(),
                        mNote.getHead().getDeadline() != null ? mNote.getHead().getDeadline().getStartTime() : null);
                break;

            /* Setting closed time. */
            case R.id.fragment_note_closed_button:
                f = TimestampDialogFragment.getInstance(
                        R.id.fragment_note_closed_button,
                        R.string.closed,
                        mNote.getId(),
                        mNote.getHead().getClosed() != null ? mNote.getHead().getClosed().getStartTime() : null);
                break;

            /* New property. */
            case R.id.add_property:
                /* Add a new property with empty name and value. */
                addPropertyToList(null);
                break;
        }

        if (f != null) {
            f.setTargetFragment(this, 0);
            f.show(getActivity().getSupportFragmentManager(), TimestampDialogFragment.FRAGMENT_TAG);
        }
    }

    @Override /* TimestampDialog */
    public void onDateTimeSet(int id, TreeSet<Long> noteIds, OrgDateTime time) {
        OrgRange range = new OrgRange(time);

        switch (id) {
            case R.id.fragment_note_scheduled_button:
                updateTimestampView(TimeType.SCHEDULED, mScheduledButton, range);
                mNote.getHead().setScheduled(range);
                break;

            case R.id.fragment_note_deadline_button:
                updateTimestampView(TimeType.DEADLINE, mDeadlineButton, range);
                mNote.getHead().setDeadline(range);

                /* Warn about alerts not being implemented yet. */
                Activity activity = getActivity();
                if (activity != null) {
                    ((CommonActivity) activity).showSimpleSnackbarLong(
                            R.string.fragment_note_deadline_alarms_not_implemented);
                }

                break;

            case R.id.fragment_note_closed_button:
                updateTimestampView(TimeType.CLOSED, mClosedButton, range);
                mNote.getHead().setClosed(range);
                break;
        }
    }

    @Override /* TimestampDialog */
    public void onDateTimeCleared(int id, TreeSet<Long> noteIds) {
        switch (id) {
            case R.id.fragment_note_scheduled_button:
                updateTimestampView(TimeType.SCHEDULED, mScheduledButton, null);
                mNote.getHead().setScheduled(null);
                break;

            case R.id.fragment_note_deadline_button:
                updateTimestampView(TimeType.DEADLINE, mDeadlineButton, null);
                mNote.getHead().setDeadline(null);
                break;

            case R.id.fragment_note_closed_button:
                updateTimestampView(TimeType.CLOSED, mClosedButton, null);
                mNote.getHead().setClosed(null);
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

        if (mNote == null) { // Displaying non-existent note.
            menu.removeItem(R.id.close);
            menu.removeItem(R.id.done);
            menu.removeItem(R.id.delete);
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

            case R.id.delete:
                delete();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void delete() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.delete_note)
                .setMessage(R.string.delete_note_and_all_subnotes)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onNoteDeleteRequest(mNote);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create()
                .show();
    }

    private void cancel() {
        mListener.onNoteCancelRequest(mNote);
    }

    private void save() {
        /* Make sure notebook is set. */
        if (mNote.getPosition().getBookId() == 0) {
            Activity activity = getActivity();
            if (activity != null) {
                ((CommonActivity) activity).showSimpleSnackbarLong(R.string.note_book_not_set);
            }
            return;
        }

        if (updateNoteFromViewsAndVerify()) {
            if (mIsNew) {
                mListener.onNoteCreateRequest(mNote, place != Place.UNDEFINED ?
                        new NotePlace(mNote.getPosition().getBookId(), mNoteId, place) : null);
            } else {
                mListener.onNoteUpdateRequest(mNote);
            }

            LocalBroadcastManager.getInstance(getContext())
                    .sendBroadcast(new Intent(Broadcasts.ACTION_NOTE_CHANGED));
        }
    }

    private boolean updateNoteFromViewsAndVerify() {
        updateNoteFromViews();

        Activity activity = getActivity();

        if (TextUtils.isEmpty(mNote.getHead().getTitle())) {
            if (activity != null) {
                titleInputLayout.setError(getString(R.string.can_not_be_empty));
            }
            return false;
        }

        return true;
    }

    /**
     * Updates the current book this note belongs to. Only makes sense for new notes.
     * TODO: Should be setPosition and allow filing under specific note
     */
    public void setBook(Book book) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, book);

        mBook = book;
        mBookId = book.getId();

        mNote.getPosition().setBookId(mBookId);

        getArguments().putLong(ARG_BOOK_ID, book.getId());
    }

    private void updateNoteForStateChange(Context context, Note note, String state) {
        StateChangeLogic stateSetOp = new StateChangeLogic(AppPreferences.doneKeywordsSet(context));

        stateSetOp.setState(state,
                note.getHead().getState(),
                note.getHead().getScheduled(),
                note.getHead().getDeadline());

        /* Update note. */
        note.getHead().setState(stateSetOp.getState());
        note.getHead().setScheduled(stateSetOp.getScheduled());
        note.getHead().setDeadline(stateSetOp.getDeadline());
        note.getHead().setClosed(stateSetOp.getClosed());

        /* Update views. */
        mState.setCurrentValue(stateSetOp.getState());
        updateTimestampView(TimeType.SCHEDULED, mScheduledButton, stateSetOp.getScheduled());
        updateTimestampView(TimeType.DEADLINE, mDeadlineButton, stateSetOp.getDeadline());
        updateTimestampView(TimeType.CLOSED, mClosedButton, stateSetOp.getClosed());
    }

    public interface NoteFragmentListener extends FragmentListener {
        void onNoteCreateRequest(Note note, NotePlace notePlace);
        void onNoteUpdateRequest(Note note);
        void onNoteCancelRequest(Note note);
        void onNoteDeleteRequest(Note note);
    }
}
