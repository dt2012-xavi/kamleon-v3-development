package com.dynatech2012.kamleonuserapp.views

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.network.ActionResult
import com.dynatech2012.kamleonuserapp.network.scanFailureText

/** The sheets about scanning, the same from the in-app scanner and from a /scan app link. */
object ScanSheets {
    /** The scan was linked; only way on is OK. */
    const val SCANNED = "scanned"
    /** The scan could not be linked; closing it lets the scanner read again. */
    const val SCAN_FAILED = "scan_failed"
    /** How to scan: the animation of a phone over the S-Urinal's QR code. */
    const val SCAN_INTRO = "scan_intro"
}

/** Says how linking a scan went. Listen for [ScanSheets.SCANNED] and [ScanSheets.SCAN_FAILED]. */
fun FragmentManager.showScanResultSheet(context: Context, result: ActionResult) {
    if (result.success) {
        showKamleonSheet(
            ScanSheets.SCANNED,
            SheetSpec(
                title = context.getString(R.string.dialog_scan_scanned),
                message = context.getString(R.string.dialog_scan_scanned_message),
                primary = context.getString(R.string.dialog_ok),
                dismissible = false
            )
        )
    } else {
        val (title, message) = scanFailureText(result.failure)
        showKamleonSheet(
            ScanSheets.SCAN_FAILED,
            SheetSpec(
                title = context.getString(title),
                message = context.getString(message),
                primary = context.getString(R.string.dialog_ok)
            )
        )
    }
}

/** Shows how to scan, unless it is already on screen. */
fun FragmentManager.showScanIntroSheet(context: Context) {
    showKamleonSheet(
        ScanSheets.SCAN_INTRO,
        SheetSpec(
            image = R.drawable.animation_qr,
            title = context.getString(R.string.scan_help),
            message = context.getString(R.string.scan_intro_message),
            primary = context.getString(R.string.dialog_ok)
        )
    )
}
