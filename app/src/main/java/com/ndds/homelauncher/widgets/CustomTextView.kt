package com.ndds.homelauncher.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import com.ndds.homelauncher.R
import com.ndds.homelauncher.TextLayout

class CustomTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var enforceContrast: Boolean
    var text: CharSequence = ""
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
    var staticLayout: TextLayout? = null
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val backdropPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var curveRadius = 0f
    private var textColor = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomTextView)
        backdropPaint.style = Paint.Style.FILL
        backdropPaint.color = context.getColor(R.color.transparent_black)
        try {
            // 1. Get Text
            text = typedArray.getString(R.styleable.CustomTextView_android_text) ?: ""

            // 2. Get Color (Default to Black)
            val textColor = typedArray.getColor(R.styleable.CustomTextView_android_textColor, Color.BLACK)
            this.textColor = textColor

            // 3. Get Size (Default to 15sp converted to pixels)
            val defaultSize = 15f * context.resources.displayMetrics.scaledDensity
            val textSize = typedArray.getDimension(R.styleable.CustomTextView_android_textSize, defaultSize)
            curveRadius = textSize
            val defaultStrokeSize = 2f * context.resources.displayMetrics.scaledDensity
            val strokeWidth = typedArray.getDimension(R.styleable.CustomTextView_strokeSize, defaultStrokeSize)
            textPaint.textSize = textSize
            textPaint.strokeWidth = strokeWidth
            textPaint.strokeJoin = Paint.Join.ROUND
            textPaint.strokeMiter = 10f

            enforceContrast = typedArray.getBoolean(R.styleable.CustomTextView_enforceContrast, false)
            // 4. Get Custom Font
            val fontResId = typedArray.getResourceId(R.styleable.CustomTextView_android_fontFamily, -1)
            if (fontResId != -1) {
                textPaint.typeface = ResourcesCompat.getFont(context, fontResId)
            } else {
                // 2. If no font in XML, fall back to the Theme's default font
                val themeValue = TypedValue()
                // We look for android.R.attr.fontFamily (the standard theme font)
                if (context.theme.resolveAttribute(android.R.attr.fontFamily, themeValue, true)) {
                    // Resolve the resource ID from the theme attribute
                    textPaint.typeface = ResourcesCompat.getFont(context, themeValue.resourceId)
                } else {
                    // 3. Last resort: Default system font
                    textPaint.typeface = Typeface.DEFAULT
                }
            }
        } finally {
            // Crucial: Always recycle the TypedArray to avoid memory leaks
            typedArray.recycle()
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)

        // 1. Tell the system this behaves like a TextView
        info.className = "android.widget.TextView"

        // 2. Provide the actual text content
        info.text = this.text
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        // 1. Determine the width we can use for the text
        val availableWidth = widthSize - paddingLeft - paddingRight

        // 2. Create the Layout (This handles the word breaking)
        staticLayout = TextLayout(textPaint, text.toString(), availableWidth)

        // 3. Set the final dimensions
        val staticLayout = staticLayout!!
        var finalWidth: Int
        if (widthMode == MeasureSpec.EXACTLY)
            finalWidth = widthSize
        else {
            finalWidth = (staticLayout.getWidth() + paddingLeft + paddingRight).toInt()
        }
        val finalHeight = staticLayout.getHeight() + paddingTop + paddingBottom

        setMeasuredDimension(finalWidth, finalHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        if (enforceContrast)
            canvas.drawRoundRect(0f,0f,width.toFloat(), height.toFloat(), paddingLeft.toFloat(),paddingLeft.toFloat(), backdropPaint)
        // Offset for padding
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())

        // Let the layout handle the complex drawing of multiple lines
        textPaint.color = Color.BLACK
        textPaint.style = Paint.Style.STROKE
        staticLayout?.draw(canvas)
        textPaint.color = textColor
        textPaint.style = Paint.Style.FILL
        staticLayout?.draw(canvas)
        canvas.restore()
    }
}