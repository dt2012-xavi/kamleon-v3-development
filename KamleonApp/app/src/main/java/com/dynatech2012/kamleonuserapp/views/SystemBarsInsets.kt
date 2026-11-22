package com.dynatech2012.kamleonuserapp.views

import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

/** Status bar, navigation bar and display cutout: what edge-to-edge content must stay clear of. */
fun WindowInsetsCompat.systemBarsAndCutout(): Insets =
    getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())

/**
 * Adds the system bars to this view's own padding, so its content stays clear of them while its
 * background (or a sibling behind it) still draws under the bars. Insets are passed on untouched.
 */
fun View.padForSystemBars(top: Boolean = true, bottom: Boolean = true) {
    val start = paddingLeft
    val initialTop = paddingTop
    val end = paddingRight
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.systemBarsAndCutout()
        v.updatePadding(
            left = start + bars.left,
            top = initialTop + if (top) bars.top else 0,
            right = end + bars.right,
            bottom = initialBottom + if (bottom) bars.bottom else 0,
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/** Adds the navigation bar to this view's bottom margin (for views pinned to the bottom edge). */
fun View.marginForNavigationBar() {
    val initialBottom = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.systemBarsAndCutout()
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = initialBottom + bars.bottom }
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
