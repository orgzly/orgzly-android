package com.orgzly.android.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.orgzly.R;
import com.orgzly.android.util.MiscUtils;

public class WhatsNewChange extends LinearLayout {
    public WhatsNewChange(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WhatsNewChange, 0, 0);
        String content = a.getString(R.styleable.WhatsNewChange_text);
        a.recycle();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.text_list_item, this, true);

        TextView c = view.findViewById(R.id.content);
        c.setText(MiscUtils.fromHtml(content));
        c.setMovementMethod(LinkMovementMethod.getInstance());
    }
}

