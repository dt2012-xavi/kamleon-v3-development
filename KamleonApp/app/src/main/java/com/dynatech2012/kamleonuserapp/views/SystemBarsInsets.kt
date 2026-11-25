package com.dynatech2012.kamleonuserapp.views

import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/** Status bar, navigation bar and display cutout: what edge-to-edge content must stay clear of. */
fun WindowInsetsCompat.systemBarsAndCutout(): Insets =
    getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())

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
