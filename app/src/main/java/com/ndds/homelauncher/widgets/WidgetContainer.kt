package com.ndds.homelauncher.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class WidgetContainer(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr) {
    lateinit var onTouchInterceptListener: OnTouchListener
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return onTouchInterceptListener.onTouch(this,ev)
    }
}