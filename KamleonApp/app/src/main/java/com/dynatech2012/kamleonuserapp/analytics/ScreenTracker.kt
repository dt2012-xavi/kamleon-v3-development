package com.dynatech2012.kamleonuserapp.analytics

/**
 * The app's screens in the web's taxonomy (section, view, feature). The access screens are not here:
 * like on the web, the access events cover them and they send no Page Viewed.
 */
sealed class AppScreen(val section: String, val view: String?, val feature: String) {
    /** Home: the last measure. */
    data object Home : AppScreen("home", null, "home")
    /** Analytics: the three metrics of the last measure. */
    data object MyDataOverview : AppScreen("my-data", "overview", "my-data")
    /** One metric's graph. */
    data class MetricDetail(val metric: Metric) : AppScreen("my-data", "metric-detail", "my-data")
    /** The QR scanner. */
    data object QrScan : AppScreen("qr-scan", null, "qr-scan")
    /** Settings: the person's profile, password and PIN. */
    data object Account : AppScreen("account", "profile", "account")
}

/** The metrics of the app, as the `metric` property names them. */
enum class Metric(val value: String) { HYDRATION("hydration"), ELECTROLYTES("electrolytes"), VOLUME("volume") }

/**
 * One Page Viewed per entry to a screen: showing the same screen again (a second tap on its tab) sends
 * nothing. `isFeatureEntry` says whether the person came from another function.
 */
class ScreenTracker {
    var current: AppScreen? = null
        private set

    /** The Page Viewed properties of entering [screen], or null when it is the screen already shown. */
    fun enter(screen: AppScreen): Map<String, Any>? {
        val previous = current
        if (previous == screen) return null
        current = screen
        return buildMap {
            put("section", screen.section)
            screen.view?.let { put("view", it) }
            put("feature", screen.feature)
            put("isFeatureEntry", previous?.feature != screen.feature)
            if (screen is AppScreen.MetricDetail) {
                put("metric", screen.metric.value)
                // Opening a metric is a consult of My data, as opening a sample is on the web.
                putAll(AnalyticsProps.valueAction("my-data", "consult"))
            }
            putAll(AnalyticsProps.PERSONAL)
        }
    }

    /** After logging out: the next screen is a new entry. */
    fun clear() {
        current = null
    }
}

/**
 * One opening of a panel (the notifications sheet) = one Drawer Opened and one Drawer Closed, as on the
 * web: `completed` when the person did something in it, `cancelled` when they only looked.
 */
class DrawerSession(private val now: () -> Long) {
    private var drawer: String? = null
    private var openedAt = 0L
    private var actions = 0

    fun open(drawer: String, screen: AppScreen?, feature: String): Map<String, Any> {
        this.drawer = drawer
        openedAt = now()
        actions = 0
        return buildMap {
            put("drawer", drawer)
            screen?.let { put("section", it.section); it.view?.let { v -> put("view", v) } }
            put("feature", feature)
            putAll(AnalyticsProps.PERSONAL)
        }
    }

    /** The person did the panel's main thing (accepted, declined or dismissed an invitation). */
    fun completedAction() {
        if (drawer != null) actions++
    }

    /** The Drawer Closed properties, or null when no panel is open. */
    fun close(): Map<String, Any>? {
        val open = drawer ?: return null
        drawer = null
        return mapOf(
            "drawer" to open,
            "outcome" to if (actions > 0) "completed" else "cancelled",
            "attempts" to actions,
            "durationMs" to (now() - openedAt).coerceAtLeast(0),
        ) + AnalyticsProps.PERSONAL
    }
}
