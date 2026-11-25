package com.dynatech2012.kamleonuserapp.models

import androidx.annotation.StringRes
import com.dynatech2012.kamleonuserapp.R
import java.text.Collator

/** The roles a user can hold, in the order they are listed inside an organization. */
enum class RoleKind(val raw: List<String>, @param:StringRes val label: Int) {
    ORG_ADMIN(listOf("orgstaff_admin"), R.string.role_org_admin),
    CENTER_ADMIN(listOf("centerstaff_owner"), R.string.role_center_admin),
    CENTER_ADMIN_RESTRICTED(listOf("centerstaff_admin_restricted"), R.string.role_center_admin_restricted),
    TEAM_ADMIN(listOf("teamstaff_pro"), R.string.role_team_admin),
    TEAM_USER(listOf("teamstaff_user"), R.string.role_team_user),
    TEAM_VIEWER(listOf("teamstaff_lector"), R.string.role_team_viewer),
    KAMLEON_OWNER(listOf("kamleon_owner"), R.string.role_kamleon_owner),
    KAMLEON_ADMIN(listOf("kamleon_admin"), R.string.role_kamleon_admin),
    KAMLEON_EDITOR(listOf("kamleon_editor"), R.string.role_kamleon_editor),
    KAMLEON_VIEWER(listOf("kamleon_viewer"), R.string.role_kamleon_viewer);

    companion object {
        /** The role saved as [userRole]; null for an empty or unknown one, which is not listed. */
        fun of(userRole: String): RoleKind? = entries.firstOrNull { userRole in it.raw }
    }
}

/** Which map of the user document a permission comes from: it says what its key identifies. */
enum class PermissionScope { KAMLEON, ORGANIZATION, CENTER, TEAM }

/** One entry of the user's permission maps, as saved in `users/{uid}`. */
data class RolePermission(
    val scope: PermissionScope,
    /** The key of the map entry: the organization, center or team it is about (empty for Kamleon). */
    val entityId: String,
    val userRole: String,
    val status: String,
    val organizationID: String,
    val centerID: String,
    val roster: Boolean?,
    val pinEnabled: Boolean?,
    val rosterNumber: String?
) {
    companion object {
        /** Every permission of a user document's [data]. */
        fun allFrom(data: Map<String, Any?>): List<RolePermission> {
            val list = mutableListOf<RolePermission>()
            (data["kamleonPermissions"] as? Map<*, *>)?.let { list += from(PermissionScope.KAMLEON, "", it) }
            listOf(
                "organizationsPermissions" to PermissionScope.ORGANIZATION,
                "centersPermissions" to PermissionScope.CENTER,
                "teamsPermissions" to PermissionScope.TEAM
            ).forEach { (field, scope) ->
                (data[field] as? Map<*, *>)?.forEach { (key, value) ->
                    if (key is String && value is Map<*, *>) list += from(scope, key, value)
                }
            }
            return list
        }

        private fun from(scope: PermissionScope, id: String, p: Map<*, *>) = RolePermission(
            scope = scope,
            entityId = id,
            userRole = p["userRole"] as? String ?: "",
            status = p["status"] as? String ?: "",
            organizationID = p["organizationID"] as? String ?: "",
            centerID = p["centerID"] as? String ?: "",
            roster = p["roster"] as? Boolean,
            pinEnabled = p["pinEnabled"] as? Boolean,
            rosterNumber = when (val n = p["rosterNumber"]) {
                is Number -> n.toLong().toString()
                is String -> n.trim().takeIf { it.isNotEmpty() }
                else -> null
            }
        )
    }
}

/** An organization, center or team document: what a role card needs of it. */
data class RoleEntity(val name: String, val imageUrl: String? = null)

/** What a team user can do on the Kamleon screen. Only team users have it. */
data class TeamUserAccess(val rosterNumber: String?, val screenOn: Boolean, val pinOn: Boolean)

/** One role card: the role, what it is held on and, for team roles, the center. */
data class RoleCard(
    val kind: RoleKind,
    val entityName: String,
    val active: Boolean,
    /** The center of a team admin or team user role; null for other roles. */
    val centerName: String? = null,
    val teamUser: TeamUserAccess? = null
)

/** The roles held in one organization, or in Kamleon itself ([isKamleon]). */
data class RoleGroup(
    val name: String,
    val imageUrl: String?,
    val isKamleon: Boolean,
    val cards: List<RoleCard>
)

/**
 * Builds the roles list the way the web's account details do: grouped by organization (Kamleon is a
 * group of its own), groups in alphabetical order and, inside each, org admin, center admin, team
 * admin, team user and the rest. A permission whose organization, center or team no longer exists is
 * left out, as is one without an organization (except Kamleon's). Inactive roles are listed too.
 */
object UserRoles {
    const val KAMLEON_NAME = "Kamleon"

    /** The organizations, centers and teams the permissions point at. */
    data class Ids(val organizations: Set<String>, val centers: Set<String>, val teams: Set<String>)

    fun idsOf(permissions: List<RolePermission>): Ids {
        val organizations = mutableSetOf<String>()
        val centers = mutableSetOf<String>()
        val teams = mutableSetOf<String>()
        permissions.filter { it.scope != PermissionScope.KAMLEON }.forEach {
            organizations += it.organizationID
            when (it.scope) {
                PermissionScope.ORGANIZATION -> organizations += it.entityId
                PermissionScope.CENTER -> centers += it.entityId
                PermissionScope.TEAM -> { teams += it.entityId; centers += it.centerID }
                PermissionScope.KAMLEON -> Unit
            }
        }
        return Ids(
            organizations.filterNot { it.isBlank() }.toSet(),
            centers.filterNot { it.isBlank() }.toSet(),
            teams.filterNot { it.isBlank() }.toSet()
        )
    }

    fun build(
        permissions: List<RolePermission>,
        organizations: Map<String, RoleEntity>,
        centers: Map<String, RoleEntity>,
        teams: Map<String, RoleEntity>
    ): List<RoleGroup> {
        val kamleonCards = mutableListOf<RoleCard>()
        val byOrganization = linkedMapOf<String, MutableList<RoleCard>>()
        permissions.forEach { p ->
            val kind = RoleKind.of(p.userRole) ?: return@forEach
            val active = p.status == "active"
            if (p.scope == PermissionScope.KAMLEON) {
                kamleonCards += RoleCard(kind, KAMLEON_NAME, active)
                return@forEach
            }
            if (p.organizationID.isBlank() || organizations[p.organizationID] == null) return@forEach
            val entity = when (p.scope) {
                PermissionScope.ORGANIZATION -> organizations[p.entityId]
                PermissionScope.CENTER -> centers[p.entityId]
                else -> teams[p.entityId]
            } ?: return@forEach
            val teamRole = kind == RoleKind.TEAM_ADMIN || kind == RoleKind.TEAM_USER
            byOrganization.getOrPut(p.organizationID) { mutableListOf() } += RoleCard(
                kind = kind,
                entityName = entity.name,
                active = active,
                // Always the center's name, never its alias.
                centerName = if (teamRole) centers[p.centerID]?.name.orEmpty() else null,
                teamUser = if (kind == RoleKind.TEAM_USER) {
                    TeamUserAccess(p.rosterNumber, screenOn = p.roster == true, pinOn = p.pinEnabled == true)
                } else null
            )
        }
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        val cardOrder = compareBy<RoleCard> { it.kind.ordinal }.thenBy(collator) { it.entityName }
        val groups = byOrganization.map { (id, cards) ->
            val organization = organizations.getValue(id)
            RoleGroup(organization.name, organization.imageUrl, isKamleon = false, cards.sortedWith(cardOrder))
        } + listOfNotNull(
            kamleonCards.takeIf { it.isNotEmpty() }?.let {
                RoleGroup(KAMLEON_NAME, null, isKamleon = true, it.sortedWith(cardOrder))
            }
        )
        return groups.sortedWith(compareBy(collator) { it.name })
    }
}
