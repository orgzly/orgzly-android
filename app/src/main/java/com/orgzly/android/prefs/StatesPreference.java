package com.orgzly.android.prefs;


import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.orgzly.R;
import com.orgzly.org.OrgStatesWorkflow;
import com.orgzly.org.utils.ArrayListSpaceSeparated;

import java.util.HashSet;
import java.util.Set;

public class StatesPreference extends DialogPreference {
    private TextInputLayout todoLayout;
    private EditText todoStates;

    private TextInputLayout doneLayout;
    private EditText doneStates;

    @TargetApi(21)
    public StatesPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setDialogLayoutResource(R.layout.pref_dialog_states);
    }

    public StatesPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setDialogLayoutResource(R.layout.pref_dialog_states);
    }

    public StatesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.pref_dialog_states);
    }

    @TargetApi(21)
    public StatesPreference(Context context) {
        super(context);
        setDialogLayoutResource(R.layout.pref_dialog_states);
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
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

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

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (!restorePersistedValue) {
            AppPreferences.states(getContext(), (String) defaultValue);
        }
    }

    /**
     * Check for duplicate state keywords.
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
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            OrgStatesWorkflow workflow = new OrgStatesWorkflow(
                    new ArrayListSpaceSeparated(todoStates.getText().toString()),
                    new ArrayListSpaceSeparated(doneStates.getText().toString())
            );

            String value = workflow.toString();
            AppPreferences.states(getContext(), value);
            setSummary(value);
        }
    }

    @Override
    public CharSequence getSummary() {
        String s = AppPreferences.states(getContext()).trim();

        if ("|".equals(s)) {
            s = getContext().getString(R.string.no_states_defined);
        }

        return s;
    }
}
