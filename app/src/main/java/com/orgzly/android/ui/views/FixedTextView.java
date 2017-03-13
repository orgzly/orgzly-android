package com.orgzly.android.ui.views;


import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Workaround for https://code.google.com/p/android/issues/detail?id=191430
 * (IllegalArgumentException when marking text and then clicking on the view)
 */
public class FixedTextView extends TextViewWithLinks {
    public FixedTextView(Context context) {
        super(context);
    }

    public FixedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();

        if (selectionStart != selectionEnd) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                CharSequence text = getText();
                setText(null);
                setText(text);
            }
        }

        return super.dispatchTouchEvent(event);
    }
}
