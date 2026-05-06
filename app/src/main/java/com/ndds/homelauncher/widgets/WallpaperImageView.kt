package com.ndds.homelauncher.widgets

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.widget.ImageView

class WallpaperImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
): androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    var wallpaperBitmap: Bitmap? = null
    override fun setImageBitmap(bm: Bitmap?) {
        wallpaperBitmap = bm
        super.setImageBitmap(bm)
    }
}