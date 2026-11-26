package com.dynatech2012.kamleonuserapp

import com.dynatech2012.kamleonuserapp.analytics.AnalyticsEvents
import com.dynatech2012.kamleonuserapp.analytics.AnalyticsProps
import com.dynatech2012.kamleonuserapp.analytics.AppScreen
import com.dynatech2012.kamleonuserapp.analytics.DrawerSession
import com.dynatech2012.kamleonuserapp.analytics.Metric
import com.dynatech2012.kamleonuserapp.analytics.QrFlowTracker
import com.dynatech2012.kamleonuserapp.analytics.ScreenTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AnalyticsPropsTest {
    @Test
    fun `super properties follow the web contract`() {
        val props = AnalyticsProps.superProps("1.1.0-dev", 2029, "dev")
        assertEquals(
            mapOf(
                "platform" to "app_android", "appVersion" to "1.1.0", "appVersionCode" to 2029,
                "definitionVersion" to 1, "environment" to "dev",
            ),
            props
        )
        assertEquals("1.1.0", AnalyticsProps.appVersionOf("1.1.0"))
    }

    @Test
    fun `weekday starts on Monday`() {
        val monday = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { set(2026, Calendar.NOVEMBER, 23, 9, 30) }
        assertEquals(mapOf("localHour" to 9, "localWeekday" to 0), AnalyticsProps.localTimeProps(monday))
        val sunday = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { set(2026, Calendar.NOVEMBER, 29, 23, 0) }
        assertEquals(6, AnalyticsProps.localTimeProps(sunday)["localWeekday"])
    }

    @Test
    fun `device accounts are excluded`() {
        assertTrue(AnalyticsProps.isExcludedEmail(" Unit-7@Devices.Kamleon.com "))
        assertFalse(AnalyticsProps.isExcludedEmail("person@kamleon.com"))
        assertFalse(AnalyticsProps.isExcludedEmail(null))
    }

    @Test
    fun `last login is ISO with milliseconds in UTC`() {
        assertEquals(mapOf("lastLoginAt" to "2027-01-15T08:00:00.000Z"), AnalyticsProps.loginProfileProps(1_800_000_000_000))
    }

    @Test
    fun `the profile has user types, status and language only`() {
        val props = AnalyticsProps.profileProps(
            mapOf(
                "userTypes" to listOf("teamstaff", 3), "userStatus" to "active", "languageID" to "es",
                "name" to "Someone", "email" to "someone@example.com",
            )
        )
        assertEquals(mapOf("userTypes" to listOf("teamstaff"), "userStatus" to "active", "languageID" to "es"), props)
        assertEquals(mapOf("userTypes" to emptyList<String>()), AnalyticsProps.profileProps(emptyMap()))
    }

    @Test
    fun `error codes are the web's and never free text`() {
        assertEquals("auth/wrong-password", AnalyticsProps.errorCode("ERROR_WRONG_PASSWORD"))
        assertEquals("auth/network-request-failed", AnalyticsProps.errorCode(null, network = true))
        assertEquals("RESOURCE_EXHAUSTED", AnalyticsProps.errorCode("RESOURCE_EXHAUSTED"))
        assertEquals("unknown", AnalyticsProps.errorCode("user someone@example.com not found"))
        assertEquals("unknown", AnalyticsProps.errorCode(null))
        assertEquals(
            mapOf("errorCode" to "auth/too-many-requests", "errorLabel" to "Too many attempts"),
            AnalyticsProps.failureProps("auth/too-many-requests")
        )
        assertEquals("Unknown error", AnalyticsProps.errorLabel("unknown"))
    }

    @Test
    fun `an answered invitation has IDs and times, never the email`() {
        val props = AnalyticsProps.invitationAnsweredProps(
            "inv1", "teamstaff_user", "org", "", "team", dateSentSeconds = 1_000, nowMs = 1_060_000
        )
        assertEquals(
            mapOf(
                "invitationID" to "inv1", "invitedRole" to "teamstaff_user", "targetOrganizationID" to "org",
                "targetTeamID" to "team", "timeSinceSentMs" to 60_000L, "viaEmailLink" to false,
                "attribution" to "personal",
            ),
            props
        )
    }
}

class ScreenTrackerTest {
    @Test
    fun `one Page Viewed per entry, none for the same screen again`() {
        val tracker = ScreenTracker()
        val home = tracker.enter(AppScreen.Home)!!
        assertEquals("home", home["section"])
        assertEquals("home", home["feature"])
        assertEquals(true, home["isFeatureEntry"])
        assertNull(home["view"])
        assertEquals("personal", home["attribution"])
        assertNull(tracker.enter(AppScreen.Home))
    }

    @Test
    fun `a metric is a consult inside My data`() {
        val tracker = ScreenTracker()
        tracker.enter(AppScreen.MyDataOverview)
        val detail = tracker.enter(AppScreen.MetricDetail(Metric.VOLUME))!!
        assertEquals("my-data", detail["section"])
        assertEquals("metric-detail", detail["view"])
        assertEquals("volume", detail["metric"])
        assertEquals(false, detail["isFeatureEntry"])
        assertEquals(true, detail["isValueAction"])
        assertEquals("my-data", detail["valueActionFeature"])
        assertEquals("consult", detail["valueActionType"])
        // Another metric is another entry.
        assertEquals("hydration", tracker.enter(AppScreen.MetricDetail(Metric.HYDRATION))!!["metric"])
    }

    @Test
    fun `after logging out the first screen is an entry again`() {
        val tracker = ScreenTracker()
        tracker.enter(AppScreen.Home)
        tracker.clear()
        assertEquals(true, tracker.enter(AppScreen.Home)!!["isFeatureEntry"])
    }

    @Test
    fun `a panel opens and closes once, completed when something was done`() {
        var now = 1_000L
        val session = DrawerSession { now }
        val opened = session.open("notifications", AppScreen.Home, "notifications")
        assertEquals(mapOf("drawer" to "notifications", "section" to "home", "feature" to "notifications", "attribution" to "personal"), opened)
        session.completedAction()
        now = 4_000L
        val closed = session.close()!!
        assertEquals("completed", closed["outcome"])
        assertEquals(1, closed["attempts"])
        assertEquals(3_000L, closed["durationMs"])
        assertNull(session.close())
        session.open("notifications", null, "notifications")
        assertEquals("cancelled", session.close()!!["outcome"])
    }
}

class QrFlowTrackerTest {
    private var now = 0L
    private var ids = 0
    private val events = mutableListOf<Pair<String, Map<String, Any>>>()
    private val store = object : QrFlowTracker.Store {
        var attempt: QrFlowTracker.Attempt? = null
        override fun load() = attempt
        override fun save(attempt: QrFlowTracker.Attempt?) { this.attempt = attempt }
    }
    private fun tracker() = QrFlowTracker(store, { now }, { "id${++ids}" }, { name, props -> events += name to props })

    private fun finished() = events.filter { it.first == AnalyticsEvents.QR_FLOW_FINISHED }.map { it.second }

    @Test
    fun `an in-app scan that links is one attempt with a value action`() {
        val qr = tracker()
        qr.scannerReady()
        qr.scannerReady() // the camera rebinds: same attempt
        now = 2_500
        qr.scannerCodeRead()
        qr.linked(success = true)
        assertEquals(
            listOf(AnalyticsEvents.QR_FLOW_STARTED, AnalyticsEvents.QR_FLOW_STEP, AnalyticsEvents.QR_FLOW_FINISHED),
            events.map { it.first }
        )
        val started = events[0].second
        assertEquals("in-app", started["entry"])
        assertEquals(false, started["pendingSession"])
        val finish = finished().single()
        assertEquals("id1", finish["flowInstanceId"])
        assertEquals("success", finish["outcome"])
        assertEquals(1, finish["stepsCompleted"])
        assertEquals(2_500L, finish["durationMs"])
        assertEquals("qr-scan", finish["valueActionFeature"])
        assertEquals("scan", finish["valueActionType"])
        assertNull(store.attempt)
    }

    @Test
    fun `leaving the scanner without a code cancels, with a code it does not`() {
        val qr = tracker()
        qr.scannerReady()
        qr.scannerLeft()
        assertEquals("cancelled", finished().single()["outcome"])
        qr.scannerReady()
        qr.scannerCodeRead()
        qr.scannerLeft()
        assertEquals(1, finished().size)
        qr.linked(success = false)
        assertEquals("failed", finished().last()["outcome"])
        assertFalse(finished().last().containsKey("isValueAction"))
    }

    @Test
    fun `a camera scan without session is the same attempt after the login`() {
        val qr = tracker()
        qr.cameraLinkOpened(valid = true, signedIn = false)
        assertEquals(true, events[0].second["pendingSession"])
        assertEquals("native-camera", events[0].second["entry"])
        // The app process restarts while the person logs in: a young attempt is kept.
        now = 60_000
        tracker().processStarted(pendingMaxAgeMs = 600_000)
        val qr2 = tracker()
        qr2.cameraLinkDelivered()
        qr2.linked(success = true)
        val steps = events.filter { it.first == AnalyticsEvents.QR_FLOW_STEP }.map { it.second["step"] }
        assertEquals(listOf("code-read", "signed-in"), steps)
        assertEquals(listOf("success"), finished().map { it["outcome"] })
        assertEquals(2, finished().single()["stepsCompleted"])
    }

    @Test
    fun `an attempt left by a previous process is abandoned`() {
        tracker().scannerReady()
        tracker().processStarted(pendingMaxAgeMs = 600_000)
        assertEquals("abandoned", finished().single()["outcome"])

        tracker().cameraLinkOpened(valid = true, signedIn = false)
        now = 700_000
        tracker().processStarted(pendingMaxAgeMs = 600_000)
        assertEquals("abandoned", finished().last()["outcome"])
    }

    @Test
    fun `an invalid link ends at once, and a new attempt abandons the old one`() {
        val qr = tracker()
        qr.cameraLinkOpened(valid = false, signedIn = true)
        assertEquals("invalid", finished().single()["outcome"])
        assertEquals(0, finished().single()["stepsCompleted"])
        qr.cameraLinkInvalid() // the main screen hears about it later: nothing more
        assertEquals(1, finished().size)

        qr.cameraLinkOpened(valid = true, signedIn = false)
        qr.cameraLinkOpened(valid = true, signedIn = true)
        assertEquals(listOf("invalid", "abandoned"), finished().map { it["outcome"] })
    }

    @Test
    fun `a camera scan delivered without an attempt starts one`() {
        val qr = tracker()
        qr.cameraLinkDelivered()
        qr.linked(success = true)
        assertEquals(listOf(AnalyticsEvents.QR_FLOW_STARTED, AnalyticsEvents.QR_FLOW_STEP, AnalyticsEvents.QR_FLOW_FINISHED), events.map { it.first })
        assertTrue(events.all { it.second["feature"] == "qr-scan" && it.second["attribution"] == "personal" })
    }
}
