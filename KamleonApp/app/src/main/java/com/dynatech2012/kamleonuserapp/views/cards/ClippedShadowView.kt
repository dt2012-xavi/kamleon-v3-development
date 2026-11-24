package com.dynatech2012.kamleonuserapp.views.cards

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.core.graphics.withSave
import com.dynatech2012.kamleonuserapp.R

/**
 * Casts the shadow of a rounded rectangle its own size, but only outside it: for translucent surfaces,
 * whose own elevation shadow would show through them. The View counterpart of [ClippedShadowCard], so
 * XML surfaces get the same platform shadow as the Compose cards. Place it behind the surface, same bounds;
 * its parent must not clip children, since the shadow falls outside these bounds.
 */
class ClippedShadowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val cornerRadius: Float
    private val clip = Path()

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ClippedShadowView)
        cornerRadius = a.getDimension(R.styleable.ClippedShadowView_clippedShadowRadius, 0f)
        val elevation = a.getDimension(R.styleable.ClippedShadowView_clippedShadowElevation, 0f)
        a.recycle()

        clipChildren = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        // The child casts the shadow; this view clips its inside away when drawing it.
        addView(View(context).apply {
            this.elevation = elevation
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                    outline.alpha = 1f
                }
            }
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clip.reset()
        clip.addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.withSave {
            clipOutPath(clip)
            super.dispatchDraw(this)
        }
    }
}
