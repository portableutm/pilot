package com.dronfieslabs.portableutmpilot.ui.widgets;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.dronfieslabs.portableutmpilot.R;

public class StatusBar extends LinearLayout {

    public StatusBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        int backgroundColor = attrs.getAttributeResourceValue(
                        "http://schemas.android.com/apk/res/android",
                        "background", R.color.tr_black_80);

        setBackgroundColor(ContextCompat.getColor(context, backgroundColor));

        setOrientation(HORIZONTAL);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_status_bar, this, true);
    }

    public StatusBar(Context context) {
        this(context, null);
    }


}
