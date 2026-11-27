package com.dynatech2012.kamleonuserapp.analytics

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * What goes into the events, built the way the web builds it. Pure: no Android, no Mixpanel, so it is
 * unit-tested. Only opaque IDs and values of closed lists ever leave here: never a name, an email, a
 * measurement or the content of a QR code.
 */
object AnalyticsProps {
    const val PLATFORM = "app_android"
    /** Version of the "active time" definition; the web sends the same one. */
    const val DEFINITION_VERSION = 1
    /** Accounts of the S-Urinal devices: nothing about them is sent. */
    const val EXCLUDED_EMAIL_DOMAIN = "@devices.kamleon.com"

    /** A property sent as a JSON null, as the web does for a missing value (an event's map holds no nulls). */
    object NULL

    private val VERSION = Regex("""^\d+\.\d+\.\d+""")

    /** The super properties of every event. [environment] is the flavor's (`dev`, `pre`, `prod`). */
    fun superProps(versionName: String, versionCode: Int, environment: String): Map<String, Any> = mapOf(
        "platform" to PLATFORM,
        "appVersion" to appVersionOf(versionName),
        "appVersionCode" to versionCode,
        "definitionVersion" to DEFINITION_VERSION,
        "environment" to environment,
    )

    /** "1.1.0" from "1.1.0-dev": the dashboard only reads a strict `X.Y.Z`. */
    fun appVersionOf(versionName: String): String = VERSION.find(versionName)?.value ?: "0.0.0"

    /** The local hour (0-23) and weekday (Monday = 0 … Sunday = 6) of the moment [now]. */
    fun localTimeProps(now: Calendar): Map<String, Any> = mapOf(
        "localHour" to now.get(Calendar.HOUR_OF_DAY),
        "localWeekday" to (now.get(Calendar.DAY_OF_WEEK) + 5) % 7,
    )

    fun isExcludedEmail(email: String?): Boolean =
        email.orEmpty().trim().lowercase().endsWith(EXCLUDED_EMAIL_DOMAIN)

    /** The profile's last login, as the web writes it: ISO 8601 in UTC with milliseconds. */
    fun loginProfileProps(nowMs: Long): Map<String, Any> =
        mapOf("lastLoginAt" to ISO.format(Instant.ofEpochMilli(nowMs)))

    private val ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

    /**
     * The person's profile from their user document: user types, status and language. No roles or
     * organizations from the app, and nothing personal.
     */
    fun profileProps(user: Map<String, Any?>): Map<String, Any> = buildMap {
        put("userTypes", (user["userTypes"] as? List<*>)?.filterIsInstance<String>().orEmpty())
        (user["userStatus"] as? String)?.takeIf { it.isNotBlank() }?.let { put("userStatus", it) }
        (user["languageID"] as? String)?.takeIf { it.isNotBlank() }?.let { put("languageID", it) }
    }

    // ---- Errors: the same codes and labels as the web (lib/analytics/error-labels.ts) ----

    private val ERROR_LABELS = mapOf(
        "auth/wrong-password" to "Wrong password",
        "auth/user-not-found" to "Account not found",
        "auth/user-disabled" to "Account disabled",
        "auth/invalid-credential" to "Invalid credentials",
        "auth/invalid-email" to "Invalid email",
        "auth/too-many-requests" to "Too many attempts",
        "auth/network-request-failed" to "Network error",
        "ERR_NETWORK" to "Network error",
        "permissions-empty" to "No permissions loaded",
    )

    fun errorLabel(code: String?): String = ERROR_LABELS[code] ?: "Unknown error"

    /** Firebase Android's codes ("ERROR_WRONG_PASSWORD") as the web's ("auth/wrong-password"). */
    private val FIREBASE_CODES = mapOf(
        "ERROR_WRONG_PASSWORD" to "auth/wrong-password",
        "ERROR_USER_NOT_FOUND" to "auth/user-not-found",
        "ERROR_USER_DISABLED" to "auth/user-disabled",
        "ERROR_INVALID_CREDENTIAL" to "auth/invalid-credential",
        "ERROR_INVALID_EMAIL" to "auth/invalid-email",
        "ERROR_TOO_MANY_REQUESTS" to "auth/too-many-requests",
        "ERROR_EMAIL_ALREADY_IN_USE" to "auth/email-already-in-use",
        "ERROR_WEAK_PASSWORD" to "auth/weak-password",
        "ERROR_NETWORK_REQUEST_FAILED" to "auth/network-request-failed",
    )

    private val SAFE_CODE = Regex("""^(auth/[a-z0-9-]{1,40}|[A-Z][A-Z0-9_]{1,40}|[1-5]\d\d)$""")

    /**
     * The code to send for a failure: the web's Firebase code when [firebaseCode] is a known one, a
     * network failure when [network], otherwise [firebaseCode] if it is code-shaped, else `unknown`.
     * An error's message is never used: it can carry an email.
     */
    fun errorCode(firebaseCode: String?, network: Boolean = false): String = when {
        firebaseCode != null && firebaseCode in FIREBASE_CODES -> FIREBASE_CODES.getValue(firebaseCode)
        network -> "auth/network-request-failed"
        firebaseCode != null && SAFE_CODE.matches(firebaseCode) -> firebaseCode
        else -> "unknown"
    }

    fun failureProps(code: String): Map<String, Any> = mapOf("errorCode" to code, "errorLabel" to errorLabel(code))

    // ---- Invitations (web: buildInvitationAnsweredProps) ----

    fun invitationAnsweredProps(
        invitationID: String,
        role: String?,
        organizationID: String?,
        centerID: String?,
        teamID: String?,
        dateSentSeconds: Long?,
        nowMs: Long,
    ): Map<String, Any> = buildMap {
        put("invitationID", invitationID)
        role?.takeIf { it.isNotBlank() }?.let { put("invitedRole", it) }
        organizationID?.takeIf { it.isNotBlank() }?.let { put("targetOrganizationID", it) }
        centerID?.takeIf { it.isNotBlank() }?.let { put("targetCenterID", it) }
        teamID?.takeIf { it.isNotBlank() }?.let { put("targetTeamID", it) }
        dateSentSeconds?.let { nowMs - it * 1000 }?.takeIf { it >= 0 }?.let { put("timeSinceSentMs", it) }
        put("viaEmailLink", false)
        putAll(PERSONAL)
    }

    /** Every function event of the app: it is always about the person, never an organization. */
    val PERSONAL: Map<String, Any> = mapOf("attribution" to "personal")

    /** Marks an event as a value action of [feature], as the web does, so the dashboard counts it. */
    fun valueAction(feature: String, type: String): Map<String, Any> = mapOf(
        "isValueAction" to true,
        "valueActionFeature" to feature,
        "valueActionType" to type,
    )
}
