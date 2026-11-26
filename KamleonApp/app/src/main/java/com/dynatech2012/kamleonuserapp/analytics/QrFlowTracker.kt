package com.dynatech2012.kamleonuserapp.analytics

/**
 * The QR process as attempts: one QR Flow Started and exactly one QR Flow Finished per attempt, with the
 * same opaque `flowInstanceId`. An attempt that waits for the person to log in (a scan from the phone's
 * camera with no session) is still the same attempt when it goes on after the login, even if the app
 * process changed meanwhile: that is why the attempt in progress is kept in [store].
 *
 * Never the content of the QR or the IDs it carries.
 */
class QrFlowTracker(
    private val store: Store,
    private val now: () -> Long,
    private val newId: () -> String,
    private val track: (String, Map<String, Any>) -> Unit,
) {
    enum class Entry(val value: String) { IN_APP("in-app"), NATIVE_CAMERA("native-camera") }

    /** The steps of an attempt, in order. A scan from the camera with a session skips [SIGNED_IN]. */
    enum class Step(val value: String) {
        /** A Kamleon scan link with usable IDs was read (in the app or by the phone's camera). */
        CODE_READ("code-read"),
        /** The person logged in to continue an attempt that was waiting for it. */
        SIGNED_IN("signed-in"),
    }

    enum class Outcome(val value: String) {
        SUCCESS("success"),
        /** The link is incomplete or expired. */
        INVALID("invalid"),
        /** The person left the scanner without reading a Kamleon code. */
        CANCELLED("cancelled"),
        /** Never completed: the app was closed, or the login never happened. */
        ABANDONED("abandoned"),
        /** A valid code that could not be linked (no connection, server error). */
        FAILED("failed"),
    }

    data class Attempt(
        val id: String,
        val entry: Entry,
        val pendingSession: Boolean,
        val startedAt: Long,
        val steps: List<Step> = emptyList(),
    )

    interface Store {
        fun load(): Attempt?
        fun save(attempt: Attempt?)
    }

    val active: Attempt? get() = store.load()

    fun start(entry: Entry, pendingSession: Boolean) {
        finish(Outcome.ABANDONED)
        val attempt = Attempt(newId(), entry, pendingSession, now())
        store.save(attempt)
        track(AnalyticsEvents.QR_FLOW_STARTED, base(attempt) + mapOf("pendingSession" to pendingSession))
    }

    /** Records [step] once per attempt. */
    fun step(step: Step) {
        val attempt = store.load() ?: return
        if (step in attempt.steps) return
        store.save(attempt.copy(steps = attempt.steps + step))
        track(AnalyticsEvents.QR_FLOW_STEP, base(attempt) + mapOf("step" to step.value))
    }

    fun finish(outcome: Outcome) {
        val attempt = store.load() ?: return
        store.save(null)
        val props = base(attempt) + mapOf(
            "outcome" to outcome.value,
            "pendingSession" to attempt.pendingSession,
            "stepsCompleted" to attempt.steps.size,
            "durationMs" to (now() - attempt.startedAt).coerceAtLeast(0),
        ) + if (outcome == Outcome.SUCCESS) AnalyticsProps.valueAction("qr-scan", "scan") else emptyMap()
        track(AnalyticsEvents.QR_FLOW_FINISHED, props)
    }

    /** The in-app scanner was left: an attempt that had not read a code ends as cancelled. */
    fun scannerLeft() {
        val attempt = store.load() ?: return
        if (attempt.entry == Entry.IN_APP && attempt.steps.isEmpty()) finish(Outcome.CANCELLED)
    }

    /**
     * When the app process starts: an attempt left by the previous process is over (abandoned), except a
     * camera scan still young enough to be continued after the login ([pendingMaxAgeMs]).
     */
    fun processStarted(pendingMaxAgeMs: Long) {
        val attempt = store.load() ?: return
        val canContinue = attempt.entry == Entry.NATIVE_CAMERA && now() - attempt.startedAt in 0..pendingMaxAgeMs
        if (!canContinue) finish(Outcome.ABANDONED)
    }

    // ---- What the app tells it ----

    /** The phone's camera opened a /scan link; [signedIn] says whether there was a session. */
    fun cameraLinkOpened(valid: Boolean, signedIn: Boolean) {
        start(Entry.NATIVE_CAMERA, pendingSession = !signedIn)
        if (valid) step(Step.CODE_READ) else finish(Outcome.INVALID)
    }

    /** The in-app scanner is reading codes: an attempt starts unless one of its own is going on. */
    fun scannerReady() {
        if (active?.entry != Entry.IN_APP) start(Entry.IN_APP, pendingSession = false)
    }

    /** The in-app scanner read a Kamleon scan link. */
    fun scannerCodeRead() = step(Step.CODE_READ)

    /** The main screen got a camera scan to link: after the login when the attempt was waiting for it. */
    fun cameraLinkDelivered() {
        val attempt = active
        if (attempt?.entry != Entry.NATIVE_CAMERA) {
            start(Entry.NATIVE_CAMERA, pendingSession = false)
            step(Step.CODE_READ)
        } else if (attempt.pendingSession) {
            step(Step.SIGNED_IN)
        }
    }

    fun linked(success: Boolean) = finish(if (success) Outcome.SUCCESS else Outcome.FAILED)

    /** A camera scan that cannot be used any more (it expired while the person logged in). */
    fun cameraLinkInvalid() = finish(Outcome.INVALID)

    private fun base(attempt: Attempt): Map<String, Any> = mapOf(
        "flowInstanceId" to attempt.id,
        "entry" to attempt.entry.value,
        "feature" to "qr-scan",
    ) + AnalyticsProps.PERSONAL
}
