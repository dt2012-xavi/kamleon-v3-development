package com.dynatech2012.kamleonuserapp.views.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.dynatech2012.kamleonuserapp.BuildConfig
import com.dynatech2012.kamleonuserapp.R
import java.util.Locale

/** How Settings reaches support and the legal pages. */
object SupportLinks {
    const val EMAIL = "info@kamleon.com"

    /**
     * The line at the end of a support e-mail, so support knows the app and phone without asking:
     * "App 1.0.15 (2028) · Android 14 · Samsung SM-S911B". Nothing about the user.
     */
    fun emailFooter(appVersion: String, build: Int, androidRelease: String, manufacturer: String, model: String): String {
        val maker = manufacturer.trim().replaceFirstChar { it.titlecase(Locale.ROOT) }
        val device = if (model.startsWith(manufacturer, ignoreCase = true)) model.trim() else "$maker ${model.trim()}"
        return "App $appVersion ($build) · Android $androidRelease · ${device.trim()}"
    }
}

/**
 * Opens the user's e-mail app with a new message to support: the address, a subject and the app and
 * phone versions at the end. Returns false when there is no e-mail app.
 */
fun Context.contactSupport(): Boolean {
    val footer = SupportLinks.emailFooter(
        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL
    )
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SupportLinks.EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_email_subject))
        putExtra(Intent.EXTRA_TEXT, "\n\n\n—\n$footer")
    }
    // Started directly: since Android 11 asking first whether an e-mail app exists needs a manifest
    // query, and without it the answer is always no.
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/**
 * Opens [url] in a Custom Tab over the app, with Kamleon's dark header; back returns to the app. Returns
 * false when no browser can open it.
 */
fun Context.openInAppBrowser(url: String): Boolean {
    val colors = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(ContextCompat.getColor(this, R.color.kamleon_dark_grey))
        .build()
    val tab = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(colors)
        .setShowTitle(true)
        .build()
    return try {
        tab.launchUrl(this, url.toUri())
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
