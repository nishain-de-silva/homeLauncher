package com.ndds.homelauncher.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import com.ndds.homelauncher.R

class ModalItem(context: Context, attrs:AttributeSet? = null): LinearLayout(context, attrs) {
    init {
        val rootView = LayoutInflater.from(context)
            .inflate(R.layout.shortcut_row, this@ModalItem, true)
        gravity = Gravity.CENTER_VERTICAL
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.ModalItem) {
                val label = getString(R.styleable.ModalItem_label)
                val icon = getDrawable(R.styleable.ModalItem_icon)
                rootView.findViewById<TextView>(R.id.shortcutLabel).text = label
                if (icon != null)
                    rootView.findViewById<ImageView>(R.id.shortcutIcon).setImageDrawable(icon)
            }
        }
    }
    fun setLabel(text: CharSequence) {
        findViewById<TextView>(R.id.shortcutLabel).text = text
    }

    fun setIcon(drawable: Drawable) {
        findViewById<ImageView>(R.id.shortcutIcon).setImageDrawable(drawable)
    }
}
