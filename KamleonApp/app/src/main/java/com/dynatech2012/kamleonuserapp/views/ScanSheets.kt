package com.dynatech2012.kamleonuserapp.views

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.dynatech2012.kamleonuserapp.R

/** Sheets about scanning; how a scan went is [ScanLinkSheet]. */
object ScanSheets {
    /** How to scan: the animation of a phone over the S-Urinal's QR code. */
    const val SCAN_INTRO = "scan_intro"
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
