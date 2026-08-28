package com.ndds.homelauncher.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.abs
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap

class EdgeDetector {
    val threshold = 0.15
    val hueMultiplyFactor = threshold / (15 / 360f)
    fun getHueDistance(color1: FloatArray, color2: FloatArray): Boolean {
        val hueDist = hueMultiplyFactor * abs(color1[0] - color2[0]) / 360
        val saturationDist = abs(color1[1] - color2[1])
        val valueDist = abs(color1[2] - color2[2])
        return sqrt((hueDist * hueDist + saturationDist * saturationDist + valueDist * valueDist)) > threshold
    }

    fun evaluate(iconDrawable: Drawable, lineColor: Int): Bitmap {
        var bitmap: Bitmap
        if (iconDrawable is AdaptiveIconDrawable) {
            bitmap =
                createBitmap(iconDrawable.intrinsicWidth, iconDrawable.intrinsicHeight)
            val canvas = Canvas(bitmap)
            iconDrawable.setBounds(0, 0, canvas.width, canvas.height)
            iconDrawable.draw(canvas)
        } else {
            if ((iconDrawable is BitmapDrawable))
                bitmap = iconDrawable.bitmap
            else {
                bitmap = createBitmap(
                    iconDrawable.intrinsicWidth,
                    iconDrawable.intrinsicHeight
                )
                val canvas = Canvas(bitmap)
                iconDrawable.draw(canvas)
            }
        }
        val width = bitmap.width
        val height = bitmap.height
        val iconPixels = IntArray(width * height)
        bitmap.getPixels(iconPixels, 0, width, 0, 0, width, height)
        val strokeWidth = 1
        val averageMaskSize = 10
        val out = IntArray(width * height)
        val floatBuffer = FloatArray(3)
        val floatBuffer2 = FloatArray(3)
        for (y in averageMaskSize until height - averageMaskSize) {
            for (x in averageMaskSize until width - averageMaskSize) {
                val i = width * y + x
                val color = iconPixels[i]
                val previousPixel = iconPixels[i - 1]
                if (previousPixel == 0 || color == 0)
                    continue
                Color.colorToHSV(color, floatBuffer)
                Color.colorToHSV(previousPixel, floatBuffer2)
                if (getHueDistance(floatBuffer, floatBuffer2)) {
                    for (sy in -strokeWidth until strokeWidth + 1) {
                        for (sx in -strokeWidth until strokeWidth + 1) {
                            out[width * sy + sx + i] = lineColor
                        }
                    }
                } else {
                    val previousPixel = iconPixels[i - width]
                    Color.colorToHSV(previousPixel, floatBuffer2)
                    if (getHueDistance(floatBuffer, floatBuffer2)) {
                        for (sy in -strokeWidth until strokeWidth + 1) {
                            for (sx in -strokeWidth until strokeWidth + 1) {
                                out[width * sy + sx + i] = lineColor
                            }
                        }
                    }
                }
            }
        }
        return Bitmap.createBitmap(out, width, height, Bitmap.Config.ARGB_8888)
    }
}