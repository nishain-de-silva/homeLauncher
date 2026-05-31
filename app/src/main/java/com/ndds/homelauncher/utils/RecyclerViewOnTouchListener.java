package com.ndds.homelauncher.utils;

import android.view.MotionEvent;

import androidx.recyclerview.widget.RecyclerView;

public interface RecyclerViewOnTouchListener {
    boolean onTouch(MotionEvent motionEvent, RecyclerView.ViewHolder viewHolder);
}
