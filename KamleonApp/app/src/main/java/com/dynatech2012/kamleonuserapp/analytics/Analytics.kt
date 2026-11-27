package com.dynatech2012.kamleonuserapp.analytics

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.dynatech2012.kamleonuserapp.BuildConfig
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.viewmodels.PENDING_SCAN_MAX_AGE_MS
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.UUID

/**
 * The app's analytics (Mixpanel), following the web's contract: same identity (the Firebase uid), same
 * event names and properties, plus `platform: app_android`. A build without a token (internal) sends
 * nothing.
 *
 * Nothing is recorded about the S-Urinal device accounts (`@devices.kamleon.com`, no longer used, but
 * excluded in case one is): from the moment the app knows the account (the email typed in the login,
 * the restored session at start, or the profile) Mixpanel is opted out, which also deletes the events
 * not sent yet, and the opt-out is kept on the phone until that account logs out.
 *
 * [init] it once, in the Application. Every call is safe before that or without a token.
 */
object Analytics {
    private var sender: AnalyticsSender? = null
    private var superProps: Map<String, Any> = emptyMap()

    /** The account in use is an S-Urinal device's: send nothing until it logs out. */
    @Volatile
    private var excluded = false
    private var profileSignature: String? = null

    val screens = ScreenTracker()
    private val drawer = DrawerSession(::nowMs)
    private var qrFlow: QrFlowTracker? = null
    private var authFlow: AuthFlowTracker? = null

    fun init(context: Context) {
        val app = context.applicationContext
        val token = BuildConfig.MIXPANEL_TOKEN
        superProps = superPropsFor(app)
        if (token.isNotBlank()) {
            sender = MixpanelSender(app, token).also { it.register(superProps) }
        }
        // Before the trackers below send anything left by the previous process.
        val restoredEmail = runCatching { FirebaseAuth.getInstance().currentUser?.email }.getOrNull()
        if (AnalyticsProps.isExcludedEmail(restoredEmail)) {
            excludeAccount()
        } else if (sender?.optedOut == true) {
            // A device account was signed out while the app was not running: back to normal.
            optBackIn()
        }
        qrFlow = QrFlowTracker(PrefsQrFlowStore(app), ::nowMs, { UUID.randomUUID().toString() }, ::track)
            .also { it.processStarted(PENDING_SCAN_MAX_AGE_MS) }
        authFlow = AuthFlowTracker(PrefsAuthFlowStore(app), ::nowMs, { UUID.randomUUID().toString() }, ::track)
            .also { it.processStarted() }
    }

    /**
     * The phone's language changed while the app runs: the events that follow carry the new
     * `appLanguage`. Called from the Application's onConfigurationChanged.
     */
    @Synchronized
    fun configurationChanged(context: Context) {
        val props = superPropsFor(context.applicationContext)
        if (props == superProps) return
        superProps = props
        sender?.register(props)
    }

    /** The language comes from a resource, so it is the one the app really shows (English when the phone's is not translated). */
    private fun superPropsFor(context: Context) = AnalyticsProps.superProps(
        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.ANALYTICS_ENVIRONMENT,
        context.getString(R.string.analytics_app_language),
    )

    /** The QR process; null only before [init]. */
    val qr: QrFlowTracker? get() = qrFlow

    /** The access attempts (signin, signup, forgot-password); null only before [init]. */
    val auth: AuthFlowTracker? get() = authFlow

    /**
     * An access event of the screens (Login Attempted, Signup Failed…): the same event, plus the
     * `flowInstanceId` of the attempt in progress, as the web's `trackAuthEvent`.
     */
    fun trackAuth(name: String, props: Map<String, Any> = emptyMap()) {
        track(name, props + (authFlow?.instanceProps() ?: emptyMap()))
    }

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
            excludeAccount()
            return
        }
        includeAccount()
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
            excludeAccount()
            return
        }
        includeAccount()
        val sender = sender ?: return
        if (sender.distinctId != userId) sender.identify(userId)
    }

    /** The email typed to log in or sign up: a device account is excluded before its first event. */
    fun accountEntered(email: String?) {
        if (AnalyticsProps.isExcludedEmail(email)) excludeAccount() else includeAccount()
    }

    @Synchronized
    private fun excludeAccount() {
        if (excluded && sender?.optedOut != false) return
        excluded = true
        // Attempts in progress are dropped without a word, so nothing about them is sent later.
        authFlow?.discard()
        qrFlow?.discard()
        sender?.optOut()
    }

    /**
     * Another account after an excluded one that never signed in (a failed login with a device
     * email): sending resumes, anonymous until this account's login.
     */
    @Synchronized
    private fun includeAccount() {
        if (!excluded) return
        excluded = false
        val sender = sender ?: return
        sender.reset()
        if (sender.optedOut) optBackIn() else sender.register(superProps)
    }

    private fun optBackIn() {
        val sender = sender ?: return
        sender.optIn()
        sender.register(superProps) // the opt-out cleared them
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
        val wasExcluded = excluded
        excluded = false
        profileSignature = null
        screens.clear()
        if (wasExcluded) {
            authFlow?.discard()
            qrFlow?.discard()
        }
        val sender = sender ?: return
        sender.reset()
        if (sender.optedOut) optBackIn() else sender.register(superProps) // reset() clears the super properties too
    }

    // ---- Screens and panels ----

    fun screen(screen: AppScreen) {
        screens.enter(screen)?.let { track(AnalyticsEvents.PAGE_VIEWED, it) }
    }

    fun drawerOpened(name: String, feature: String) {
        track(AnalyticsEvents.DRAWER_OPENED, drawer.open(name, screens.current, feature))
    }

    fun drawerAction(answer: DrawerSession.Answer? = null) = drawer.completedAction(answer)

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

/** The access attempt in progress, so that one left when the app is killed is reported at the next start. */
private class PrefsAuthFlowStore(context: Context) : AuthFlowTracker.Store {
    private val prefs: SharedPreferences = context.getSharedPreferences("analytics_auth_flow", Context.MODE_PRIVATE)
    // Read on every access event: kept in memory, written through.
    private var cached: AuthFlowTracker.Attempt? = read()

    override fun load(): AuthFlowTracker.Attempt? = cached

    override fun save(attempt: AuthFlowTracker.Attempt?) {
        if (attempt == cached) return
        cached = attempt
        prefs.edit {
            if (attempt == null) {
                clear()
            } else {
                putString(ID, attempt.id)
                putString(FLOW, attempt.flow.value)
                putLong(STARTED, attempt.startedAt)
                putLong(STEP_STARTED, attempt.stepStartedAt)
                putLong(LAST_ACTIVE, attempt.lastActiveAt)
                putInt(STEPS, attempt.stepsCompleted)
                putString(LAST_STEP, attempt.lastStep)
            }
        }
    }

    private fun read(): AuthFlowTracker.Attempt? {
        val id = prefs.getString(ID, null) ?: return null
        val flow = AuthFlowTracker.Flow.entries.firstOrNull { it.value == prefs.getString(FLOW, null) } ?: return null
        return AuthFlowTracker.Attempt(
            id = id,
            flow = flow,
            startedAt = prefs.getLong(STARTED, 0),
            stepStartedAt = prefs.getLong(STEP_STARTED, 0),
            lastActiveAt = prefs.getLong(LAST_ACTIVE, 0),
            stepsCompleted = prefs.getInt(STEPS, 0),
            lastStep = prefs.getString(LAST_STEP, null),
        )
    }

    private companion object {
        const val ID = "id"
        const val FLOW = "flow"
        const val STARTED = "startedAt"
        const val STEP_STARTED = "stepStartedAt"
        const val LAST_ACTIVE = "lastActiveAt"
        const val STEPS = "stepsCompleted"
        const val LAST_STEP = "lastStep"
    }
}
