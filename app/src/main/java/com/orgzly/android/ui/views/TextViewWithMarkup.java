package com.orgzly.android.ui.views;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.android.ActionService;
import com.orgzly.android.AppIntent;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.OrgFormatter;

/**
 * {@link TextView} with markup support.
 */
public class TextViewWithMarkup extends AppCompatTextView {
    public static final String TAG = TextViewWithMarkup.class.getName();

    public TextViewWithMarkup(Context context) {
        super(context);
    }

    public TextViewWithMarkup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextViewWithMarkup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, event);

        Layout layout = this.getLayout();

        if (layout != null) {
            int line = layout.getLineForVertical((int) event.getY() - getTotalPaddingTop());
            int offset = getOffsetForPosition(event.getX(), event.getY());

            if (isEventOnText(event, layout, line) && !TextUtils.isEmpty(getText()) && getText() instanceof Spanned) {
                Spanned spanned = (Spanned) getText();

                ClickableSpan[] links = spanned.getSpans(offset, offset, ClickableSpan.class);

                if (links.length > 0) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        return true;

                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        links[0].onClick(this);

                    } else {
                        return super.onTouchEvent(event);
                    }
                }
            }
        }

        return super.onTouchEvent(event);
    }

    /**
     * Check if event's coordinates are within line's text.
     *
     * Needed as getOffsetForHorizontal will return closest character,
     * which is an issue when clicking the empty space next to the text.
     */
    private boolean isEventOnText(MotionEvent event, Layout layout, int line) {
        float left = layout.getLineLeft(line) + getTotalPaddingLeft();
        float right = layout.getLineRight(line) + getTotalPaddingRight();
        float bottom = layout.getLineBottom(line) + getTotalPaddingTop();
        float top = layout.getLineTop(line) + getTotalPaddingTop();

        return left <= event.getX() && event.getX() <= right &&
                top <= event.getY() && event.getY() <= bottom;
    }

    /**
     * Workaround for https://code.google.com/p/android/issues/detail?id=191430
     * (IllegalArgumentException when marking text and then clicking on the view)
     */
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

    public void setRawText(CharSequence str) {
        setText(OrgFormatter.INSTANCE.parse(str.toString(), getContext()));
    }

    public void openNoteWithProperty(String name, String value) {
        Intent intent = new Intent(getContext(), ActionService.class);
        intent.setAction(AppIntent.ACTION_OPEN_NOTE);
        intent.putExtra(AppIntent.EXTRA_PROPERTY_NAME, name);
        intent.putExtra(AppIntent.EXTRA_PROPERTY_VALUE, value);
        ActionService.Companion.enqueueWork(getContext(), intent);
    }
}
