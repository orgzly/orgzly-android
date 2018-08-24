package com.orgzly.android.prefs;


import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;
import com.orgzly.R;
import com.orgzly.org.OrgStatesWorkflow;
import com.orgzly.org.utils.ArrayListSpaceSeparated;

import java.util.HashSet;
import java.util.Set;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;

public class StatesPreferenceFragment extends PreferenceDialogFragmentCompat {
    public static final String FRAGMENT_TAG = StatesPreferenceFragment.class.getName();

    private TextInputLayout todoLayout;
    private EditText todoStates;

    private TextInputLayout doneLayout;
    private EditText doneStates;

    public static PreferenceDialogFragmentCompat getInstance(Preference preference) {
        PreferenceDialogFragmentCompat fragment = new StatesPreferenceFragment();

        Bundle bundle = new Bundle();
        bundle.putString("key", preference.getKey());

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View onCreateDialogView(Context context) {
        return super.onCreateDialogView(context);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        todoStates = (EditText) view.findViewById(R.id.todo_states);
        todoLayout = (TextInputLayout) view.findViewById(R.id.todo_states_layout);

        doneStates = (EditText) view.findViewById(R.id.done_states);
        doneLayout = (TextInputLayout) view.findViewById(R.id.done_states_layout);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                validateAndUpdateButton();
            }
        };

        todoStates.addTextChangedListener(watcher);
        doneStates.addTextChangedListener(watcher);

        StateWorkflows value = new StateWorkflows(AppPreferences.states(getContext()));

        if (value.size() > 0) {
            todoStates.setText(value.get(0).getTodoKeywords().toString());
            doneStates.setText(value.get(0).getDoneKeywords().toString());
        }

        /* Validate states when dialog is displayed for the first time. */
        validateAndUpdateButton();
    }

    private void validateAndUpdateButton() {
        AlertDialog d = (AlertDialog) getDialog();

        if (d != null) {
            Button button = d.getButton(AlertDialog.BUTTON_POSITIVE);

            if (button != null) {
                button.setEnabled(isValid());
            }
        }
    }

    /**
     * Returns {@code false} is there are duplicate entries.
     */
    private boolean isValid() {
        todoLayout.setError(null);
        doneLayout.setError(null);

        Set<String> keywords = new HashSet<>();

        for (String k: new ArrayListSpaceSeparated(todoStates.getText().toString())) {
            if (keywords.contains(k)) {
                todoLayout.setError(getContext().getString(R.string.duplicate_keywords_not_allowed, k));
                return false;
            }
            keywords.add(k);
        }

        for (String k: new ArrayListSpaceSeparated(doneStates.getText().toString())) {
            if (keywords.contains(k)) {
                doneLayout.setError(getContext().getString(R.string.duplicate_keywords_not_allowed, k));
                return false;
            }
            keywords.add(k);
        }

        return true;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            OrgStatesWorkflow workflow = new OrgStatesWorkflow(
                    new ArrayListSpaceSeparated(todoStates.getText().toString()),
                    new ArrayListSpaceSeparated(doneStates.getText().toString())
            );

            String value = workflow.toString();
            AppPreferences.states(getContext(), value);
            getPreference().setSummary(value);
        }
    }
}
