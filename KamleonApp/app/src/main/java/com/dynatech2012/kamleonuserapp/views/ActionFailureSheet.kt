package com.dynatech2012.kamleonuserapp.views

import androidx.fragment.app.Fragment
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.network.actionFailureText

/**
 * Tells the user an action failed, the same way on every screen. It goes on the activity's manager with a
 * fixed tag, so two screens that react to the same failure (a picker and the screen under it) show one sheet.
 */
fun Fragment.showActionFailedSheet(failure: Exception?) {
    val (title, message) = actionFailureText(failure)
    requireActivity().supportFragmentManager.showKamleonSheet(
        "action_failed",
        SheetSpec(
            title = getString(title),
            message = getString(message),
            primary = getString(R.string.dialog_ok)
        )
    )
}
