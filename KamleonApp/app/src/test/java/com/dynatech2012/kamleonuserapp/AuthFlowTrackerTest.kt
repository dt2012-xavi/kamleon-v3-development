package com.dynatech2012.kamleonuserapp

import com.dynatech2012.kamleonuserapp.analytics.AnalyticsEvents
import com.dynatech2012.kamleonuserapp.analytics.AnalyticsProps
import com.dynatech2012.kamleonuserapp.analytics.AuthFlowTracker
import com.dynatech2012.kamleonuserapp.analytics.AuthFlowTracker.Exit
import com.dynatech2012.kamleonuserapp.analytics.AuthFlowTracker.Flow
import com.dynatech2012.kamleonuserapp.analytics.AuthFlowTracker.Outcome
import com.dynatech2012.kamleonuserapp.analytics.AuthFlowTracker.Steps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFlowTrackerTest {
    private var now = 0L
    private var ids = 0
    private val events = mutableListOf<Pair<String, Map<String, Any>>>()
    private val store = object : AuthFlowTracker.Store {
        var attempt: AuthFlowTracker.Attempt? = null
        override fun load() = attempt
        override fun save(attempt: AuthFlowTracker.Attempt?) { this.attempt = attempt }
    }
    private fun tracker() = AuthFlowTracker(store, { now }, { "id${++ids}" }, { name, props -> events += name to props })

    private fun names() = events.map { it.first }
    private fun last(name: String) = events.last { it.first == name }.second

    @Test
    fun `a sign-in with a wrong password first is one attempt that succeeds`() {
        val auth = tracker()
        auth.enter(Flow.SIGNIN) // the login screen is shown
        now = 1_000
        auth.enter(Flow.SIGNIN) // the person taps Sign in: same attempt
        val attempted = auth.instanceProps()
        now = 2_000
        auth.enter(Flow.SIGNIN) // a wrong password does not end it: the retry is the same attempt
        now = 5_000
        assertEquals(attempted, auth.instanceProps())
        auth.completeFlow(Outcome.SUCCESS)

        assertEquals(listOf(AnalyticsEvents.AUTH_FLOW_STARTED, AnalyticsEvents.AUTH_FLOW_COMPLETED), names())
        assertEquals(mapOf("flow" to "signin", "flowInstanceId" to "id1"), events[0].second)
        assertEquals(
            mapOf("flow" to "signin", "flowInstanceId" to "id1", "durationMs" to 5_000L, "stepsCompleted" to 0, "outcome" to "success"),
            last(AnalyticsEvents.AUTH_FLOW_COMPLETED)
        )
        assertEquals(mapOf("flowInstanceId" to "id1"), attempted)
        // Nothing in progress any more: the next access event goes without an id.
        assertEquals(emptyMap<String, Any>(), auth.instanceProps())
    }

    @Test
    fun `a signup has the web's steps and props`() {
        val auth = tracker()
        auth.enter(Flow.SIGNUP)
        now = 30_000
        auth.completeStep(Steps.ACCOUNT)
        now = 50_000
        auth.completeStep(Steps.PROFILE)
        auth.completeFlow(Outcome.SUCCESS)

        assertEquals(mapOf("flow" to "signup", "flowInstanceId" to "id1", "viaInvitation" to false), events[0].second)
        val steps = events.filter { it.first == AnalyticsEvents.AUTH_STEP_COMPLETED }.map { it.second }
        assertEquals(
            listOf(
                mapOf("flow" to "signup", "flowInstanceId" to "id1", "step" to "account", "stepDurationMs" to 30_000L),
                mapOf("flow" to "signup", "flowInstanceId" to "id1", "step" to "profile", "stepDurationMs" to 20_000L),
            ),
            steps
        )
        val completed = last(AnalyticsEvents.AUTH_FLOW_COMPLETED)
        assertEquals(2, completed["stepsCompleted"])
        assertEquals(50_000L, completed["durationMs"])
        assertEquals(false, completed["viaInvitation"])
    }

    @Test
    fun `another flow abandons the one in progress as switched-flow`() {
        val auth = tracker()
        auth.enter(Flow.SIGNIN)
        now = 4_000
        auth.enter(Flow.SIGNUP)
        val abandoned = last(AnalyticsEvents.AUTH_FLOW_ABANDONED)
        assertEquals(
            mapOf(
                "flow" to "signin", "flowInstanceId" to "id1", "durationMs" to 4_000L, "stepsCompleted" to 0,
                "lastStep" to AnalyticsProps.NULL, "exitType" to "switched-flow", "switchedTo" to "signup",
            ),
            abandoned
        )
        assertEquals("id2", last(AnalyticsEvents.AUTH_FLOW_STARTED)["flowInstanceId"])
    }

    @Test
    fun `leaving the access screens abandons as navigated, once`() {
        val auth = tracker()
        auth.enter(Flow.FORGOT_PASSWORD)
        auth.abandon(Exit.NAVIGATED)
        auth.abandon(Exit.NAVIGATED)
        assertEquals(1, events.count { it.first == AnalyticsEvents.AUTH_FLOW_ABANDONED })
        assertEquals("navigated", last(AnalyticsEvents.AUTH_FLOW_ABANDONED)["exitType"])
        assertTrue(!last(AnalyticsEvents.AUTH_FLOW_ABANDONED).containsKey("switchedTo"))
    }

    @Test
    fun `an app killed mid-attempt is closed at the next start, with the time it was in use`() {
        tracker().apply {
            enter(Flow.SIGNUP)
            now = 10_000
            completeStep(Steps.ACCOUNT)
        }
        now = 10_000 + 20 * 60 * 1000L // the app comes back 20 minutes later, in a new process
        tracker().processStarted()
        val abandoned = last(AnalyticsEvents.AUTH_FLOW_ABANDONED)
        assertEquals("closed", abandoned["exitType"])
        assertEquals(10_000L, abandoned["durationMs"])
        assertEquals(1, abandoned["stepsCompleted"])
        assertEquals("account", abandoned["lastStep"])
        assertNull(store.attempt)
    }

    @Test
    fun `an attempt idle for more than 30 minutes is dropped without a word`() {
        tracker().enter(Flow.SIGNIN)
        now = AuthFlowTracker.MAX_IDLE_MS + 1
        tracker().processStarted()
        assertEquals(listOf(AnalyticsEvents.AUTH_FLOW_STARTED), names())
        assertNull(store.attempt)

        // Left in the background: coming back to the screen starts a new attempt, the old one is not sent.
        val auth = tracker()
        auth.enter(Flow.SIGNIN)
        now += AuthFlowTracker.MAX_IDLE_MS + 1
        auth.keepAlive()
        assertNull(store.attempt)
        auth.enter(Flow.SIGNIN)
        assertEquals(listOf(AnalyticsEvents.AUTH_FLOW_STARTED, AnalyticsEvents.AUTH_FLOW_STARTED, AnalyticsEvents.AUTH_FLOW_STARTED), names())
        assertEquals(emptyMap<String, Any>(), auth.instanceProps().filterValues { it == "id2" })
    }

    @Test
    fun `steps and results without an attempt send nothing`() {
        val auth = tracker()
        auth.completeStep(Steps.REQUEST)
        auth.completeFlow(Outcome.ERROR)
        auth.abandon(Exit.NAVIGATED)
        tracker().processStarted()
        assertTrue(events.isEmpty())
    }
}
