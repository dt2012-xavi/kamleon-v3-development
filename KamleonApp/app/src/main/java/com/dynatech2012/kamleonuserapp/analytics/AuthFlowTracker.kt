package com.dynatech2012.kamleonuserapp.analytics

/**
 * The access flows as attempts, ported from the web's `lib/analytics/auth-flow-tracker.ts` so that the
 * dashboard counts the app's attempts the same way: Auth Flow Started, Auth Step Completed, and then
 * Auth Flow Completed or Auth Flow Abandoned, all with the same opaque `flowInstanceId`. The access
 * events of the screens (Login Attempted, Signup Failed…) carry that id too, through [instanceProps].
 *
 * What differs from the web, agreed with it:
 * - The app has `signin`, `signup` and `forgot-password` (only asking for the link). Email
 *   verification and the new password happen on the web, which counts them.
 * - Android does not say when the app is killed, so the attempt in progress is kept in [store] and an
 *   attempt left by the previous process is reported as abandoned (`closed`) when the app starts again.
 * - An attempt idle for more than [maxIdleMs] (in the background, or before the app was killed) is
 *   dropped without sending anything: the app's abandonment rate is a lower bound.
 */
class AuthFlowTracker(
    private val store: Store,
    private val now: () -> Long,
    private val newId: () -> String,
    private val track: (String, Map<String, Any>) -> Unit,
    private val maxIdleMs: Long = MAX_IDLE_MS,
) {
    enum class Flow(val value: String, val props: Map<String, Any>) {
        SIGNIN("signin", emptyMap()),
        SIGNUP("signup", mapOf("viaInvitation" to false)),
        FORGOT_PASSWORD("forgot-password", emptyMap()),
    }

    enum class Exit(val value: String) { CLOSED("closed"), NAVIGATED("navigated"), SWITCHED_FLOW("switched-flow") }

    enum class Outcome(val value: String) { SUCCESS("success"), ERROR("error") }

    /** The step names of the web. */
    object Steps {
        /** Signup: the account was created. */
        const val ACCOUNT = "account"
        /** Signup: the optional questionnaire was saved. */
        const val PROFILE = "profile"
        /** Forgot password: the reset link was sent. */
        const val REQUEST = "request"
    }

    data class Attempt(
        val id: String,
        val flow: Flow,
        val startedAt: Long,
        val stepStartedAt: Long,
        val lastActiveAt: Long,
        val stepsCompleted: Int = 0,
        val lastStep: String? = null,
    )

    interface Store {
        fun load(): Attempt?
        fun save(attempt: Attempt?)
    }

    /**
     * When the app process starts: an attempt left by the previous one ended with the app closed. It is
     * reported only when it was active in the last [maxIdleMs], with the time it lasted while in use.
     */
    fun processStarted() {
        val attempt = store.load() ?: return
        store.save(null)
        if (now() - attempt.lastActiveAt in 0..maxIdleMs) {
            sendAbandoned(attempt, Exit.CLOSED, durationMs = attempt.lastActiveAt - attempt.startedAt)
        }
    }

    /**
     * The person is on a screen of [flow] (or acts in it): the attempt in progress goes on, or a new one
     * starts. Another flow in progress is abandoned as `switched-flow`, as on the web.
     */
    fun enter(flow: Flow) {
        val attempt = live()
        if (attempt?.flow == flow) {
            store.save(attempt.copy(lastActiveAt = now()))
            return
        }
        if (attempt != null) abandon(Exit.SWITCHED_FLOW, switchedTo = flow)
        val t = now()
        val started = Attempt(newId(), flow, startedAt = t, stepStartedAt = t, lastActiveAt = t)
        store.save(started)
        track(AnalyticsEvents.AUTH_FLOW_STARTED, base(started) + flow.props)
    }

    /** The person is still in the flow (a screen came back): drops the attempt if it was idle too long. */
    fun keepAlive() {
        live()?.let { store.save(it.copy(lastActiveAt = now())) }
    }

    fun completeStep(step: String) {
        val attempt = live() ?: return
        val t = now()
        track(AnalyticsEvents.AUTH_STEP_COMPLETED, base(attempt) + mapOf("step" to step, "stepDurationMs" to t - attempt.stepStartedAt))
        store.save(
            attempt.copy(
                stepsCompleted = attempt.stepsCompleted + 1,
                lastStep = step,
                stepStartedAt = t,
                lastActiveAt = t,
            )
        )
    }

    fun completeFlow(outcome: Outcome) {
        val attempt = live() ?: return
        store.save(null)
        track(
            AnalyticsEvents.AUTH_FLOW_COMPLETED,
            base(attempt) + attempt.flow.props + mapOf(
                "durationMs" to now() - attempt.startedAt,
                "stepsCompleted" to attempt.stepsCompleted,
                "outcome" to outcome.value,
            )
        )
    }

    fun abandon(exit: Exit, switchedTo: Flow? = null) {
        val attempt = live() ?: return
        store.save(null)
        sendAbandoned(attempt, exit, durationMs = now() - attempt.startedAt, switchedTo = switchedTo)
    }

    /** `flowInstanceId` of the attempt in progress, for the access events of the screens; empty without one. */
    fun instanceProps(): Map<String, Any> {
        val attempt = live() ?: return emptyMap()
        store.save(attempt.copy(lastActiveAt = now()))
        return mapOf(FLOW_INSTANCE_ID to attempt.id)
    }

    /** The attempt in progress, or null; one idle for too long is dropped without a word. */
    private fun live(): Attempt? {
        val attempt = store.load() ?: return null
        if (now() - attempt.lastActiveAt > maxIdleMs) {
            store.save(null)
            return null
        }
        return attempt
    }

    private fun sendAbandoned(attempt: Attempt, exit: Exit, durationMs: Long, switchedTo: Flow? = null) {
        track(
            AnalyticsEvents.AUTH_FLOW_ABANDONED,
            base(attempt) + attempt.flow.props + buildMap {
                put("durationMs", durationMs.coerceAtLeast(0))
                put("stepsCompleted", attempt.stepsCompleted)
                // The web sends null before the first step; the sender writes it as a JSON null.
                put("lastStep", attempt.lastStep ?: AnalyticsProps.NULL)
                put("exitType", exit.value)
                switchedTo?.let { put("switchedTo", it.value) }
            }
        )
    }

    private fun base(attempt: Attempt): Map<String, Any> =
        mapOf("flow" to attempt.flow.value, FLOW_INSTANCE_ID to attempt.id)

    companion object {
        /** Agreed with the web: one threshold for an app killed and for an attempt left in the background. */
        const val MAX_IDLE_MS = 30 * 60 * 1000L
        private const val FLOW_INSTANCE_ID = "flowInstanceId"
    }
}
