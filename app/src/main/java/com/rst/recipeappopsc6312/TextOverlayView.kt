package com.rst.recipeappopsc6312

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.text.Text

class TextOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val textBlocks = mutableListOf<Text.TextBlock>()
    private val boxPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    // This function is called by the fragment to update the text results
    fun updateText(newText: Text) {
        textBlocks.clear()
        textBlocks.addAll(newText.textBlocks)
        // Redraw the view with the new boxes
        invalidate()
    }

    // This function checks if a user's tap is inside one of the text boxes
    fun getTappedText(x: Float, y: Float): String? {
        for (block in textBlocks) {
            for (line in block.lines) {
                if (line.boundingBox?.contains(x.toInt(), y.toInt()) == true) {
                    return line.text
                }
            }
        }
        return null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw a rectangle for each line of text found
        for (block in textBlocks) {
            for (line in block.lines) {
                line.boundingBox?.let { canvas.drawRect(it, boxPaint) }
            }
        }
    }
}
