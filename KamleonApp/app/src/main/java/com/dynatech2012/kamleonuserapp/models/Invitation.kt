package com.dynatech2012.kamleonuserapp.models

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** An `invitations/{id}` document, as the web reads it. Dates are epoch seconds. */
data class InvitationRecord(
    val id: String,
    val email: String,
    val organizationID: String,
    val centerID: String,
    val teamID: String,
    val userType: String,
    val userRole: String,
    val permissions: List<String>,
    val status: String,
    /** Absent (never seen), "read" or "dismissed". */
    val uiStatus: String?,
    val dateSent: Long?,
    val dateAccepted: Long?,
    val senderID: String,
    val position: String?
) {
    companion object {
        /** The document [data] of invitation [id]; [seconds] turns a stored timestamp into epoch seconds. */
        fun from(id: String, data: Map<String, Any?>, seconds: (Any?) -> Long?): InvitationRecord = InvitationRecord(
            id = id,
            email = data["email"] as? String ?: "",
            organizationID = data["organizationID"] as? String ?: "",
            centerID = data["centerID"] as? String ?: "",
            teamID = data["teamID"] as? String ?: "",
            userType = data["userType"] as? String ?: "",
            userRole = data["userRole"] as? String ?: "",
            permissions = (data["permissions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            status = data["status"] as? String ?: "",
            uiStatus = data["uiStatus"] as? String,
            dateSent = seconds(data["dateSent"]),
            dateAccepted = seconds(data["dateAccepted"]),
            senderID = data["senderID"] as? String ?: "",
            position = (data["position"] as? String)?.takeIf { it.isNotBlank() }
        )
    }
}

/** Why an invitation could not be accepted or declined. */
class InvitationException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { NOT_FOUND, NOT_PENDING, EXPIRED, EMAIL_MISMATCH, USER_NOT_FOUND, UNKNOWN_ROLE }

    /** The invitation was already answered or has expired: it is no longer valid. */
    val noLongerValid: Boolean get() = reason == Reason.NOT_PENDING || reason == Reason.EXPIRED
}

/** The web's invitation rules: what is pending, unread, expired, and what accepting writes. */
object InvitationRules {
    const val EXPIRY_SECONDS = 14L * 24 * 3600
    const val RECENTLY_ACCEPTED_SECONDS = 5L * 24 * 3600

    private val KAMLEON_ROLES = setOf("kamleon_owner", "kamleon_admin", "kamleon_editor", "kamleon_viewer")
    private val CENTER_ROLES = setOf("centerstaff_owner", "centerstaff_admin_restricted")
    private val TEAM_ROLES = setOf("teamstaff_pro", "teamstaff_user", "teamstaff_lector")
    private val NON_ADMIN_ROLES = setOf("teamstaff_user", "teamstaff_lector")

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    /** "sent", or the old "read" that meant the same. */
    fun isPendingStatus(status: String): Boolean = status == "sent" || status == "read"

    /** Listed with its actions and counted on the bell. */
    fun isPending(r: InvitationRecord): Boolean = isPendingStatus(r.status) && r.uiStatus != "dismissed"

    /** Shows the dot until the user has seen it. */
    fun isUnread(r: InvitationRecord): Boolean = isPending(r) && r.uiStatus != "read"

    /** Pending for more than 14 days; one without a sent date never expires. */
    fun isExpired(r: InvitationRecord, now: Long): Boolean =
        isPending(r) && r.dateSent != null && now - r.dateSent > EXPIRY_SECONDS

    fun expiresAt(r: InvitationRecord): Long? = r.dateSent?.plus(EXPIRY_SECONDS)

    fun isRecentlyAccepted(r: InvitationRecord, now: Long): Boolean =
        r.status == "accepted" && r.dateAccepted != null && now - r.dateAccepted <= RECENTLY_ACCEPTED_SECONDS

    fun isTeamRole(role: String): Boolean = role in TEAM_ROLES

    /** Administrator roles accept the admin privacy policy; team users and viewers the user one. */
    fun isAdminRole(role: String): Boolean = role !in NON_ADMIN_ROLES

    /** Whole calendar days from [then] to [now] in [zone]: 0 today, 1 yesterday. */
    fun daysBetween(then: Long, now: Long, zone: ZoneId): Long = ChronoUnit.DAYS.between(
        Instant.ofEpochSecond(then).atZone(zone).toLocalDate(),
        Instant.ofEpochSecond(now).atZone(zone).toLocalDate()
    )

    /** The `users/{uid}` field accepting [r] writes; null for a role that cannot be accepted. */
    fun permissionField(r: InvitationRecord): String? = when (r.userRole) {
        in KAMLEON_ROLES -> "kamleonPermissions"
        "orgstaff_admin" -> r.organizationID.takeIf { it.isNotBlank() }?.let { "organizationsPermissions.$it" }
        in CENTER_ROLES -> r.centerID.takeIf { it.isNotBlank() }?.let { "centersPermissions.$it" }
        in TEAM_ROLES -> r.teamID.takeIf { it.isNotBlank() }?.let { "teamsPermissions.$it" }
        else -> null
    }

    /**
     * The permission accepting [r] grants. For team roles, the `integrations` of another team role
     * the user already has in the same organization are copied, and the invitation's position is kept.
     */
    fun permissionEntry(r: InvitationRecord, teamsPermissions: Map<*, *>?): Map<String, Any> {
        val entry = mutableMapOf<String, Any>(
            "organizationID" to r.organizationID,
            "centerID" to r.centerID,
            "teamID" to r.teamID,
            "userType" to r.userType,
            "userRole" to r.userRole,
            "status" to "active",
            "permissions" to r.permissions,
            "roster" to false
        )
        if (isTeamRole(r.userRole)) {
            teamsPermissions?.values
                ?.filterIsInstance<Map<*, *>>()
                ?.firstOrNull {
                    it["organizationID"] == r.organizationID &&
                        (it["userRole"] as? String)?.let(::isTeamRole) == true &&
                        it["integrations"] != null
                }
                ?.get("integrations")
                ?.let { entry["integrations"] = it }
            r.position?.let { entry["position"] = it }
        }
        return entry
    }

    /** Checks done before accepting [r] as the user with [userEmail] at [now]. */
    fun checkAcceptable(r: InvitationRecord, userEmail: String, now: Long) {
        if (!isPendingStatus(r.status)) throw InvitationException(InvitationException.Reason.NOT_PENDING)
        if (r.dateSent != null && now - r.dateSent > EXPIRY_SECONDS) throw InvitationException(InvitationException.Reason.EXPIRED)
        if (normalizeEmail(r.email) != normalizeEmail(userEmail)) throw InvitationException(InvitationException.Reason.EMAIL_MISMATCH)
    }

    /** Checks done before declining [r] as the user with [userEmail]: expired ones can still be declined. */
    fun checkDeclinable(r: InvitationRecord, userEmail: String) {
        if (!isPendingStatus(r.status)) throw InvitationException(InvitationException.Reason.NOT_PENDING)
        if (normalizeEmail(r.email) != normalizeEmail(userEmail)) throw InvitationException(InvitationException.Reason.EMAIL_MISMATCH)
    }

    /**
     * The `member` field of a user document's [data]: every team, center and organization the user
     * has a permission on (whatever its status), without repeats.
     */
    fun memberOf(data: Map<String, Any?>): Map<String, List<String>> {
        val teams = linkedSetOf<String>()
        val centers = linkedSetOf<String>()
        val organizations = linkedSetOf<String>()
        fun entries(field: String) = (data[field] as? Map<*, *>).orEmpty()
        entries("teamsPermissions").forEach { (key, value) ->
            (key as? String)?.let(teams::add)
            (value as? Map<*, *>)?.let { p ->
                (p["centerID"] as? String)?.takeIf { it.isNotBlank() }?.let(centers::add)
                (p["organizationID"] as? String)?.takeIf { it.isNotBlank() }?.let(organizations::add)
            }
        }
        entries("centersPermissions").forEach { (key, value) ->
            (key as? String)?.let(centers::add)
            ((value as? Map<*, *>)?.get("organizationID") as? String)?.takeIf { it.isNotBlank() }?.let(organizations::add)
        }
        entries("organizationsPermissions").keys.filterIsInstance<String>().forEach(organizations::add)
        return mapOf(
            "teams" to teams.toList(),
            "centers" to centers.toList(),
            "organizations" to organizations.toList()
        )
    }
}

/** What the invitations sheet shows of one invitation. */
data class InvitationItem(
    val record: InvitationRecord,
    val senderName: String?,
    val organizationName: String?,
    val centerName: String?,
    val teamName: String?,
    val state: State,
    val unread: Boolean,
    /** A previous invitation shown before "See all": accepted in the last days. */
    val recent: Boolean = false
) {
    /** PENDING and EXPIRED can still be answered; the rest are previous invitations. */
    enum class State { PENDING, EXPIRED, ACCEPTED, DECLINED, DISMISSED }

    val isPrevious: Boolean get() = state == State.ACCEPTED || state == State.DECLINED || state == State.DISMISSED

    /** What the user is invited to: the team, center or organization (Kamleon for Kamleon roles). */
    val entityName: String?
        get() = when {
            record.userRole.startsWith("kamleon_") -> UserRoles.KAMLEON_NAME
            InvitationRules.isTeamRole(record.userRole) -> teamName
            record.userRole == "orgstaff_admin" -> organizationName
            else -> centerName
        }

    /** Where that is: the organization, and the center for team roles. */
    val context: String?
        get() = when {
            record.userRole.startsWith("kamleon_") || record.userRole == "orgstaff_admin" -> null
            InvitationRules.isTeamRole(record.userRole) ->
                kotlin.collections.listOf(organizationName, centerName).filterNot { it.isNullOrBlank() }.joinToString(" · ").ifBlank { null }
            else -> organizationName
        }

    companion object {
        /**
         * The sheet's items from the user's [records] at [now]: the pending ones (newest first), then
         * the previous ones (newest answer first); [name] reads a document's name.
         */
        fun build(
            records: List<InvitationRecord>,
            now: Long,
            name: (collection: String, id: String) -> String?
        ): List<InvitationItem> {
            fun item(r: InvitationRecord, state: State) = InvitationItem(
                record = r,
                senderName = name("users", r.senderID),
                organizationName = name("organizations", r.organizationID),
                centerName = name("centers", r.centerID),
                teamName = name("teams", r.teamID),
                state = state,
                unread = InvitationRules.isUnread(r),
                recent = InvitationRules.isRecentlyAccepted(r, now)
            )
            val pending = records.filter(InvitationRules::isPending)
                .sortedByDescending { it.dateSent ?: 0 }
                .map { item(it, if (InvitationRules.isExpired(it, now)) State.EXPIRED else State.PENDING) }
            val previous = records.filterNot(InvitationRules::isPending)
                .mapNotNull { r ->
                    when {
                        r.status == "accepted" -> State.ACCEPTED
                        r.status == "rejected" -> State.DECLINED
                        InvitationRules.isPendingStatus(r.status) -> State.DISMISSED
                        else -> null
                    }?.let { item(r, it) }
                }
                .sortedByDescending { it.record.dateAccepted ?: it.record.dateSent ?: 0 }
            return pending + previous
        }
    }
}
