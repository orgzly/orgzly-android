package com.orgzly.android.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.android.util.LogUtils;

public class OrgTextView extends TextView {
    public static final String TAG = OrgTextView.class.getName();

    public OrgTextView(Context context) {
        super(context);
    }

    public OrgTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OrgTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OrgTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, event);

        Layout layout = this.getLayout();

        if (layout != null) {
            int line = layout.getLineForVertical((int) event.getY());
            int offset = layout.getOffsetForHorizontal(line, event.getX());

            if (getText() != null && getText() instanceof Spanned) {
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
}
