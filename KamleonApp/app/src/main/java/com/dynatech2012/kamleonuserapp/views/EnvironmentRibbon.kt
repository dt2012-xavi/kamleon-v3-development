package com.dynatech2012.kamleonuserapp.views

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dynatech2012.kamleonuserapp.BuildConfig
import kotlin.math.sqrt

/**
 * A diagonal ribbon in the top-right corner of every screen saying which environment the build talks
 * to ("INT", "DEV" or "PRE"), so test builds are never mistaken for the production app. Production
 * ([BuildConfig.ENVIRONMENT_LABEL] empty) shows nothing. It draws over the content without taking
 * touches or being read by TalkBack.
 */
class EnvironmentRibbon(context: Context) : View(context) {

    /** "INT", "DEV" or "PRE". */
    var label: String = ""
        set(value) {
            field = value
            band.color = colorFor(value)
            invalidate()
        }

    private val band = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9f, resources.displayMetrics)
        letterSpacing = 0.1f
    }
    private val bandHeight = dp(15f)

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        // Below the status bar, so it covers neither the clock nor the battery.
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            view.translationY = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top.toFloat()
            insets
        }
    }

    override fun onDraw(canvas: Canvas) {
        val size = width.toFloat()
        canvas.withTranslation(size / 2, size / 2) {
            rotate(45f)
            // After the rotation, negative y points at the top-right corner, half a diagonal away.
            val center = -size * sqrt(2f) / 2 * 0.38f
            drawRect(-size, center - bandHeight / 2, size, center + bandHeight / 2, band)
            val baseline = center - (text.ascent() + text.descent()) / 2
            drawText(label, 0f, baseline, text)
        }
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    companion object {
        private const val TAG = "environment_ribbon"
        private const val SIZE_DP = 56f

        private fun colorFor(label: String) = when (label) {
            "DEV" -> "#E6F5A623".toColorInt()
            "PRE" -> "#E6FC664E".toColorInt()
            else -> "#E600A59B".toColorInt()
        }

        /** Adds the ribbon to every activity of [application], unless this is a production build. */
        fun install(application: Application, label: String = BuildConfig.ENVIRONMENT_LABEL) {
            if (label.isBlank()) return
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                // On start rather than create: by then the activity has set its content.
                override fun onActivityStarted(activity: Activity) = attach(activity, label)
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            })
        }

        private fun attach(activity: Activity, label: String) {
            val decor = activity.window?.decorView as? ViewGroup ?: return
            if (decor.findViewWithTag<View>(TAG) != null) return
            val ribbon = EnvironmentRibbon(activity).apply {
                tag = TAG
                this.label = label
            }
            val size = ribbon.dp(SIZE_DP).toInt()
            decor.addView(ribbon, FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.END))
            ViewCompat.requestApplyInsets(ribbon)
        }
    }
}
