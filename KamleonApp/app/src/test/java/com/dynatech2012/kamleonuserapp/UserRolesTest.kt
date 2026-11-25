package com.dynatech2012.kamleonuserapp

import com.dynatech2012.kamleonuserapp.models.RoleEntity
import com.dynatech2012.kamleonuserapp.models.RoleKind
import com.dynatech2012.kamleonuserapp.models.RolePermission
import com.dynatech2012.kamleonuserapp.models.TeamUserAccess
import com.dynatech2012.kamleonuserapp.models.UserRoles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRolesTest {

    private fun perm(role: String, org: String, center: String = "", team: String = "", status: String = "active", extra: Map<String, Any?> = emptyMap()) =
        mapOf("userRole" to role, "status" to status, "organizationID" to org, "centerID" to center, "teamID" to team) + extra

    private val userDoc = mapOf(
        "kamleonPermissions" to perm("kamleon_admin", ""),
        "organizationsPermissions" to mapOf("fed" to perm("orgstaff_admin", "fed")),
        "centersPermissions" to mapOf("test" to perm("centerstaff_owner", "dyn", center = "test")),
        "teamsPermissions" to mapOf(
            "first" to perm("teamstaff_user", "fed", center = "sev", team = "first", extra = mapOf("roster" to true, "rosterNumber" to 7L)),
            "pervers" to perm("teamstaff_pro", "vol", center = "mega", team = "pervers", status = "disabled")
        )
    )

    private val organizations = mapOf(
        "fed" to RoleEntity("Federación española de remo"),
        "dyn" to RoleEntity("Dynatech", "https://example.com/d.png"),
        "vol" to RoleEntity("Volatil Celia")
    )
    private val centers = mapOf("test" to RoleEntity("Test Center"), "sev" to RoleEntity("SEVILLA"), "mega" to RoleEntity("Mega Volatile"))
    private val teams = mapOf("first" to RoleEntity("Primer equipo"), "pervers" to RoleEntity("kamleon perver's"))

    private fun build(doc: Map<String, Any?> = userDoc, orgs: Map<String, RoleEntity> = organizations, teamDocs: Map<String, RoleEntity> = teams) =
        UserRoles.build(RolePermission.allFrom(doc), orgs, centers, teamDocs)

    @Test
    fun `groups are the organizations and Kamleon, alphabetically`() {
        assertEquals(
            listOf("Dynatech", "Federación española de remo", "Kamleon", "Volatil Celia"),
            build().map { it.name }
        )
        assertTrue(build().single { it.name == "Kamleon" }.isKamleon)
    }

    @Test
    fun `inside a group, org admin comes before team user`() {
        val fed = build().single { it.name.startsWith("Fed") }
        assertEquals(listOf(RoleKind.ORG_ADMIN, RoleKind.TEAM_USER), fed.cards.map { it.kind })
    }

    @Test
    fun `only team users get the roster number, screen and PIN`() {
        val groups = build()
        val teamUser = groups.single { it.name.startsWith("Fed") }.cards.last()
        assertEquals("SEVILLA", teamUser.centerName)
        assertEquals(TeamUserAccess(rosterNumber = "7", screenOn = true, pinOn = false), teamUser.teamUser)

        val teamAdmin = groups.single { it.name == "Volatil Celia" }.cards.single()
        assertEquals("Mega Volatile", teamAdmin.centerName)
        assertNull(teamAdmin.teamUser)

        val orgAdmin = groups.single { it.name.startsWith("Fed") }.cards.first()
        assertNull(orgAdmin.centerName)
    }

    @Test
    fun `inactive roles are listed as inactive`() {
        assertFalse(build().single { it.name == "Volatil Celia" }.cards.single().active)
    }

    @Test
    fun `a role whose team or organization no longer exists is left out`() {
        assertFalse(build(teamDocs = teams - "pervers").any { it.name == "Volatil Celia" })
        assertFalse(build(orgs = organizations - "dyn").any { it.name == "Dynatech" })
    }

    @Test
    fun `unknown roles, roles without organization and an empty Kamleon role are left out`() {
        val doc = mapOf(
            "kamleonPermissions" to perm("", ""),
            "teamsPermissions" to mapOf(
                "first" to perm("something_new", "fed", center = "sev"),
                "pervers" to perm("teamstaff_pro", "", center = "mega")
            )
        )
        assertTrue(build(doc).isEmpty())
    }

    @Test
    fun `ids include the centers of team roles`() {
        val ids = UserRoles.idsOf(RolePermission.allFrom(userDoc))
        assertEquals(setOf("fed", "dyn", "vol"), ids.organizations)
        assertEquals(setOf("test", "sev", "mega"), ids.centers)
        assertEquals(setOf("first", "pervers"), ids.teams)
    }
}
