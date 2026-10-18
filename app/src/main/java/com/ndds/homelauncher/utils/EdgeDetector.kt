package com.ndds.homelauncher.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlin.math.abs
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

class EdgeDetector {
    val threshold = 0.15
    val hueMultiplyFactor = threshold / (15 / 360f)
    fun getHueDistance(color1: FloatArray, color2: FloatArray): Boolean {
        val hueDist = hueMultiplyFactor * abs(color1[0] - color2[0]) / 360
        val saturationDist = abs(color1[1] - color2[1])
        val valueDist = abs(color1[2] - color2[2])
        return sqrt((hueDist * hueDist + saturationDist * saturationDist + valueDist * valueDist)) > threshold
    }
    private fun convertDrawableToBitmap(iconDrawable: Drawable, color: Int): Bitmap {
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
                canvas.drawColor(color, BlendMode.MULTIPLY)
            }
        }
        return bitmap
    }
    private fun convertDrawableToBitmap2(iconDrawable: Drawable): Bitmap {
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
        return bitmap
    }
    companion object {
        fun extractAllAppIcons(context: Context) {
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val pm = context.packageManager
            val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            for (resolveInfo in resolveInfoList) {
                val packageName = resolveInfo.activityInfo.applicationInfo.packageName
                val icon = resolveInfo.loadIcon(pm)
                val bitmap = EdgeDetector().evaluate(icon, ColorTool.getLightPrimaryColorFromWallpaper(context))
                val id = "${resolveInfo.loadLabel(pm)}$packageName"
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, context.openFileOutput("$id.png", MODE_PRIVATE))
            }
        }
        fun loadAppIcon(context: Context, resolveInfo: ResolveInfo, monochromeColor: Int): Drawable {
            val packageManager = context.packageManager
            val id = "${resolveInfo.loadLabel(packageManager)}${resolveInfo.activityInfo.packageName}"
            val defaultIcon = resolveInfo.loadIcon(packageManager)
            var appIcon: Drawable
            if (monochromeColor != 0) {
                val bitmap = EdgeDetector().evaluate(defaultIcon, monochromeColor)
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    context.openFileOutput("$id.png", MODE_PRIVATE)
                )
                appIcon = bitmap.toDrawable(context.resources)
            } else
                appIcon = defaultIcon
            return appIcon
        }
    }
    fun capValue(value: Int): Int {
        if (value < 0) return 0
        if (value > 255) return 255
        return value
    }
    fun luminance(r: Int, g: Int, b: Int): Float {
        return (r * 0.2126 + g * 0.7152 + b * 0.0722).toFloat()
    }
    fun evaluate(iconDrawable: Drawable, lineColor: Int): Bitmap {
        val bitmap = convertDrawableToBitmap(iconDrawable, lineColor)
        val width = bitmap.width
        val height = bitmap.height
        val r1 = ((lineColor shr 16) and 0xff)
        val g1 = ((lineColor shr 8) and 0xff)
        val b1 = (lineColor and 0xff)
        val iconPixels = IntArray(width * height)
        val outPixels = IntArray(iconPixels.size)
        bitmap.getPixels(iconPixels, 0, width, 0, 0, width, height)
        var maxLuminance = 0f
        for (i in 0 until iconPixels.size) {
            val p = iconPixels[i]
            val r = ((p shr 16) and 0xff)
            val g = ((p shr 8) and 0xff)
            val b = (p and 0xff)
            val luminanceAmount = luminance(r, g, b)
            if (luminanceAmount > maxLuminance)
                maxLuminance = luminanceAmount
        }
        for (i in 0 until iconPixels.size) {
            val p = iconPixels[i]
            val r = ((p shr 16) and 0xff)
            val g = ((p shr 8) and 0xff)
            val b = (p and 0xff)
            val a = (p and 0xff000000.toInt())
            val luminanceAmount = luminance(r, g, b) / maxLuminance
            val r2 = capValue((luminanceAmount * r1).toInt())
            val g2 = capValue((luminanceAmount * g1).toInt())
            val b2 = capValue((luminanceAmount * b1).toInt())

            val mixAmt = 0.1
            val or = (r2 + ((r - r2) * mixAmt)).toInt()
            val og = (g2 + ((g - g2) * mixAmt)).toInt()
            val ob = (b2 + ((b - b2) * mixAmt)).toInt()
            outPixels[i] = a or (or shl 16) or (og shl 8) or ob
        }
        return Bitmap.createBitmap(outPixels, width, height, Bitmap.Config.ARGB_8888)
    }

    fun evaluate2(iconPixels: IntArray, width: Int, height: Int, lineColor: Int, destPixels: IntArray): Bitmap {
        val strokeWidth = 1
        val averageMaskSize = 10
        val floatBuffer = FloatArray(3)
        val floatBuffer2 = FloatArray(3)
        for (y in averageMaskSize until height - averageMaskSize) {
            for (x in averageMaskSize until width - averageMaskSize) {
                val i = width * y + x
                val color = iconPixels[i]
                val previousPixel = iconPixels[i - 1]
//                if (previousPixel == 0 || color == 0)
//                    continue
                Color.colorToHSV(color, floatBuffer)
                Color.colorToHSV(previousPixel, floatBuffer2)
                if (getHueDistance(floatBuffer, floatBuffer2)) {
                    for (sy in -strokeWidth until strokeWidth + 1) {
                        for (sx in -strokeWidth until strokeWidth + 1) {
                            destPixels[width * sy + sx + i] = lineColor
                        }
                    }
                } else {
                    val previousPixel = iconPixels[i - width]
                    Color.colorToHSV(previousPixel, floatBuffer2)
                    if (getHueDistance(floatBuffer, floatBuffer2)) {
                        for (sy in -strokeWidth until strokeWidth + 1) {
                            for (sx in -strokeWidth until strokeWidth + 1) {
                                destPixels[width * sy + sx + i] = lineColor
                            }
                        }
                    }
                }
            }
        }
        return Bitmap.createBitmap(destPixels, width, height, Bitmap.Config.ARGB_8888)
    }
}