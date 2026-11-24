package com.dynatech2012.kamleonuserapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.withSave
import com.dynatech2012.kamleonuserapp.extensions.px

/**
 * Dims the camera preview except for a rounded window over [frame], the scanner's corner marks, so
 * the eye goes to where the QR code should be. [frame] must share this view's parent.
 */
class ScanViewfinderOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var frame: ImageView? = null
        set(value) {
            field = value
            value?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> invalidate() }
            invalidate()
        }

    private val dimPaint = Paint().apply { color = Color.argb(0x8C, 0, 0, 0) }
    private val window = RectF()
    private val windowPath = android.graphics.Path()
    private val cornerRadius = 12.px.toFloat()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        val marks = frame
        if (marks == null || !windowOver(marks)) {
            canvas.drawPaint(dimPaint)
            return
        }
        windowPath.reset()
        windowPath.addRoundRect(window, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
        canvas.withSave {
            clipOutPath(windowPath)
            drawPaint(dimPaint)
        }
    }

    /** Puts in [window] where the frame's image is drawn, in this view's coordinates. */
    private fun windowOver(marks: ImageView): Boolean {
        val drawable = marks.drawable ?: return false
        window.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        marks.imageMatrix.mapRect(window)
        window.offset(
            (marks.left + marks.paddingLeft - left).toFloat(),
            (marks.top + marks.paddingTop - top).toFloat()
        )
        return !window.isEmpty
    }
}
