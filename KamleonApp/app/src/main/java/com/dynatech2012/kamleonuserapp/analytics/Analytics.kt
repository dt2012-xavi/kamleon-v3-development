package com.dynatech2012.kamleonuserapp.analytics

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.dynatech2012.kamleonuserapp.BuildConfig
import com.dynatech2012.kamleonuserapp.viewmodels.PENDING_SCAN_MAX_AGE_MS
import java.util.Calendar
import java.util.UUID

/**
 * The app's analytics (Mixpanel), following the web's contract: same identity (the Firebase uid), same
 * event names and properties, plus `platform: app_android`. A build without a token (internal) sends
 * nothing, and nothing is sent about the S-Urinal device accounts.
 *
 * [init] it once, in the Application. Every call is safe before that or without a token.
 */
object Analytics {
    private var sender: AnalyticsSender? = null
    private var superProps: Map<String, Any> = emptyMap()

    /** The signed-in account is an S-Urinal device's: send nothing until it logs out. */
    @Volatile
    private var excluded = false
    private var profileSignature: String? = null

    val screens = ScreenTracker()
    private val drawer = DrawerSession(::nowMs)
    private var qrFlow: QrFlowTracker? = null

    fun init(context: Context) {
        val app = context.applicationContext
        val token = BuildConfig.MIXPANEL_TOKEN
        superProps = AnalyticsProps.superProps(
            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.ANALYTICS_ENVIRONMENT
        )
        if (token.isNotBlank()) {
            sender = MixpanelSender(app, token).also { it.register(superProps) }
        }
        qrFlow = QrFlowTracker(PrefsQrFlowStore(app), ::nowMs, { UUID.randomUUID().toString() }, ::track)
            .also { it.processStarted(PENDING_SCAN_MAX_AGE_MS) }
    }

    /** The QR process; null only before [init]. */
    val qr: QrFlowTracker? get() = qrFlow

    fun track(name: String, props: Map<String, Any> = emptyMap()) {
        if (excluded) return
        sender?.track(name, props + AnalyticsProps.localTimeProps(Calendar.getInstance()))
    }

    // ---- Identity ----

    /**
     * A login that worked ([recordLogin]) or a registration that created the account: from now on the
     * events are this user's. Before it they were anonymous, and Mixpanel joins both.
     */
    @Synchronized
    fun identify(userId: String, email: String?, recordLogin: Boolean = true) {
        if (AnalyticsProps.isExcludedEmail(email)) {
            excluded = true
            return
        }
        val sender = sender ?: return
        sender.identify(userId)
        if (recordLogin) sender.setProfile(AnalyticsProps.loginProfileProps(nowMs()))
    }

    /**
     * A session the app found already open (Firebase restored it): make sure the events are its user's,
     * without counting it as a login.
     */
    @Synchronized
    fun syncIdentity(userId: String, email: String?) {
        if (AnalyticsProps.isExcludedEmail(email)) {
            excluded = true
            return
        }
        val sender = sender ?: return
        if (sender.distinctId != userId) sender.identify(userId)
    }

    /** The profile from the user document; only written when it changed. */
    @Synchronized
    fun syncProfile(userId: String, user: Map<String, Any?>) {
        if (excluded) return
        val sender = sender ?: return
        val props = AnalyticsProps.profileProps(user)
        val signature = "$userId|$props"
        if (signature == profileSignature) return
        profileSignature = signature
        sender.setProfile(props)
    }

    /** Logging out: the next person on this phone starts anonymous. */
    @Synchronized
    fun reset() {
        excluded = false
        profileSignature = null
        screens.clear()
        val sender = sender ?: return
        sender.reset()
        sender.register(superProps) // reset() clears the super properties too
    }

    // ---- Screens and panels ----

    fun screen(screen: AppScreen) {
        screens.enter(screen)?.let { track(AnalyticsEvents.PAGE_VIEWED, it) }
    }

    fun drawerOpened(name: String, feature: String) {
        track(AnalyticsEvents.DRAWER_OPENED, drawer.open(name, screens.current, feature))
    }

    fun drawerAction() = drawer.completedAction()

    fun drawerClosed() {
        drawer.close()?.let { track(AnalyticsEvents.DRAWER_CLOSED, it) }
    }

    private fun nowMs() = System.currentTimeMillis()
}

/** The QR attempt in progress, in its own preferences file so that logging out does not wipe it. */
private class PrefsQrFlowStore(context: Context) : QrFlowTracker.Store {
    private val prefs: SharedPreferences = context.getSharedPreferences("analytics_qr_flow", Context.MODE_PRIVATE)

    override fun load(): QrFlowTracker.Attempt? {
        val id = prefs.getString(ID, null) ?: return null
        val entry = QrFlowTracker.Entry.entries.firstOrNull { it.value == prefs.getString(ENTRY, null) } ?: return null
        val steps = prefs.getString(STEPS, "").orEmpty().split(',')
            .mapNotNull { s -> QrFlowTracker.Step.entries.firstOrNull { it.value == s } }
        return QrFlowTracker.Attempt(id, entry, prefs.getBoolean(PENDING, false), prefs.getLong(STARTED, 0), steps)
    }

    override fun save(attempt: QrFlowTracker.Attempt?) = prefs.edit {
        if (attempt == null) {
            clear()
        } else {
            putString(ID, attempt.id)
            putString(ENTRY, attempt.entry.value)
            putBoolean(PENDING, attempt.pendingSession)
            putLong(STARTED, attempt.startedAt)
            putString(STEPS, attempt.steps.joinToString(",") { it.value })
        }
    }

    private companion object {
        const val ID = "id"
        const val ENTRY = "entry"
        const val PENDING = "pendingSession"
        const val STARTED = "startedAt"
        const val STEPS = "steps"
    }
}
