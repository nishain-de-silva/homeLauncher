package com.ndds.homelauncher.widgets

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet

class WallpaperImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
): androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    var hasWallpaper = false
    override fun setImageBitmap(bm: Bitmap?) {
        hasWallpaper = bm != null
        super.setImageBitmap(bm)
    }
}