package com.ndds.homelauncher.utils

import android.graphics.Canvas
import android.graphics.Paint

class TextLayout(val paint: Paint, text: String, availableWidth: Int) {
    private var lineSpace = 0f
    private var fontHeight = 0f
    private val lines = ArrayList<Node>()
    private var maxWidth: Float
    private class Node(var text: String, var lineWidth: Float)
    init {
        fontHeight = -paint.fontMetrics.ascent + paint.fontMetrics.descent
        lineSpace = paint.fontMetrics.descent
        val words = text.split(" ")
        var textWidth = paint.measureText(words[0])
        var node = Node(words[0], textWidth)
        maxWidth = textWidth
        lines.add(node)
        for (i in 1 until words.size) {
            val word = words[i]
            val concatenatedText = node.text + " " + word
            textWidth = paint.measureText(concatenatedText)
            if (textWidth < availableWidth) {
                if (textWidth > maxWidth)
                    maxWidth = textWidth
                node.text = concatenatedText
                node.lineWidth = textWidth
            } else {
                textWidth = paint.measureText(word)
                if (textWidth > maxWidth)
                    maxWidth = textWidth
                node = Node(word,textWidth)
                lines.add(node)
            }
        }
    }

    fun getWidth() = maxWidth
    fun getHeight(): Float {
        if (lines.isEmpty())
            return 0f
        return fontHeight + (lines.size - 1) * (fontHeight + lineSpace)
    }

    fun draw(canvas: Canvas) {
        lines.forEachIndexed { index, line ->
            canvas.drawText(
                line.text,
                (maxWidth - line.lineWidth) / 2f,
                -paint.fontMetrics.ascent + index * (fontHeight + lineSpace),
                paint
            )
        }
    }
}