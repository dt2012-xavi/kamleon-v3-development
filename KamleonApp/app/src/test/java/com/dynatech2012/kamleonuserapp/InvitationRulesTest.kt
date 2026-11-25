package com.dynatech2012.kamleonuserapp

import com.dynatech2012.kamleonuserapp.models.InvitationException
import com.dynatech2012.kamleonuserapp.models.InvitationItem
import com.dynatech2012.kamleonuserapp.models.InvitationRecord
import com.dynatech2012.kamleonuserapp.models.InvitationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.ZoneOffset

class InvitationRulesTest {
    private val day = 24 * 3600L
    private val now = 1_800_000_000L

    private fun inv(
        role: String = "teamstaff_user",
        status: String = "sent",
        uiStatus: String? = null,
        sentDaysAgo: Long? = 1,
        acceptedDaysAgo: Long? = null,
        email: String = "user@example.com",
        position: String? = null
    ) = InvitationRecord(
        id = "i1", email = email, organizationID = "org", centerID = "center", teamID = "team",
        userType = "teamstaff", userRole = role, permissions = listOf("p"), status = status, uiStatus = uiStatus,
        dateSent = sentDaysAgo?.let { now - it * day }, dateAccepted = acceptedDaysAgo?.let { now - it * day },
        senderID = "s", position = position
    )

    @Test
    fun `sent and the old read are pending unless dismissed`() {
        assertTrue(InvitationRules.isPending(inv(status = "sent")))
        assertTrue(InvitationRules.isPending(inv(status = "read")))
        assertFalse(InvitationRules.isPending(inv(uiStatus = "dismissed")))
        assertFalse(InvitationRules.isPending(inv(status = "rejected")))
    }

    @Test
    fun `pending is unread until marked read`() {
        assertTrue(InvitationRules.isUnread(inv()))
        assertFalse(InvitationRules.isUnread(inv(uiStatus = "read")))
    }

    @Test
    fun `pending expires after 14 days, never without a sent date`() {
        assertFalse(InvitationRules.isExpired(inv(sentDaysAgo = 14), now))
        assertTrue(InvitationRules.isExpired(inv(sentDaysAgo = 15), now))
        assertFalse(InvitationRules.isExpired(inv(sentDaysAgo = null), now))
    }

    @Test
    fun `the list has the pending first, then every previous one`() {
        val items = InvitationItem.build(
            listOf(
                inv().copy(id = "pending"),
                inv(sentDaysAgo = 30).copy(id = "expired"),
                inv(status = "accepted", acceptedDaysAgo = 2).copy(id = "recent"),
                inv(status = "accepted", acceptedDaysAgo = 6).copy(id = "old"),
                inv(status = "rejected", sentDaysAgo = 3).copy(id = "rejected"),
                inv(uiStatus = "dismissed", sentDaysAgo = 40).copy(id = "dismissed")
            ),
            now
        ) { _, _ -> null }
        assertEquals(listOf("pending", "expired", "recent", "rejected", "old", "dismissed"), items.map { it.record.id })
        assertEquals(
            listOf(
                InvitationItem.State.PENDING, InvitationItem.State.EXPIRED, InvitationItem.State.ACCEPTED,
                InvitationItem.State.DECLINED, InvitationItem.State.ACCEPTED, InvitationItem.State.DISMISSED
            ),
            items.map { it.state }
        )
        // Before "See all", only the recently accepted one of the previous ones.
        assertEquals(listOf("recent"), items.filter { it.isPrevious && it.recent }.map { it.record.id })
    }

    @Test
    fun `each role writes its own permission field`() {
        assertEquals("kamleonPermissions", InvitationRules.permissionField(inv(role = "kamleon_viewer")))
        assertEquals("organizationsPermissions.org", InvitationRules.permissionField(inv(role = "orgstaff_admin")))
        assertEquals("centersPermissions.center", InvitationRules.permissionField(inv(role = "centerstaff_admin_restricted")))
        assertEquals("teamsPermissions.team", InvitationRules.permissionField(inv(role = "teamstaff_pro")))
        assertNull(InvitationRules.permissionField(inv(role = "orgstaff_owner")))
        assertNull(InvitationRules.permissionField(inv(role = "centerstaff_admin")))
    }

    @Test
    fun `the permission is active, without roster, and a team role keeps integrations and position`() {
        val existing = mapOf(
            "other" to mapOf("organizationID" to "org", "userRole" to "teamstaff_pro", "integrations" to mapOf("x" to 1)),
            "elsewhere" to mapOf("organizationID" to "org2", "userRole" to "teamstaff_pro", "integrations" to mapOf("y" to 2))
        )
        val entry = InvitationRules.permissionEntry(inv(position = "goalkeeper"), existing)
        assertEquals("active", entry["status"])
        assertEquals(false, entry["roster"])
        assertEquals(listOf("p"), entry["permissions"])
        assertEquals(mapOf("x" to 1), entry["integrations"])
        assertEquals("goalkeeper", entry["position"])
        assertFalse(entry.containsKey("rosterNumber"))

        val admin = InvitationRules.permissionEntry(inv(role = "orgstaff_admin", position = "goalkeeper"), existing)
        assertFalse(admin.containsKey("integrations"))
        assertFalse(admin.containsKey("position"))
    }

    @Test
    fun `accepting checks status, expiry and e-mail`() {
        InvitationRules.checkAcceptable(inv(email = " User@Example.com "), "user@example.com", now)
        expect(InvitationException.Reason.NOT_PENDING) { InvitationRules.checkAcceptable(inv(status = "accepted"), "user@example.com", now) }
        expect(InvitationException.Reason.EXPIRED) { InvitationRules.checkAcceptable(inv(sentDaysAgo = 20), "user@example.com", now) }
        expect(InvitationException.Reason.EMAIL_MISMATCH) { InvitationRules.checkAcceptable(inv(), "other@example.com", now) }
        // Declining does not look at the expiry.
        InvitationRules.checkDeclinable(inv(sentDaysAgo = 20), "user@example.com")
    }

    @Test
    fun `member lists every team, center and organization once`() {
        val member = InvitationRules.memberOf(
            mapOf(
                "teamsPermissions" to mapOf("t1" to mapOf("centerID" to "c1", "organizationID" to "o1")),
                "centersPermissions" to mapOf("c1" to mapOf("organizationID" to "o1"), "c2" to mapOf("organizationID" to "o2")),
                "organizationsPermissions" to mapOf("o3" to mapOf<String, Any>())
            )
        )
        assertEquals(listOf("t1"), member["teams"])
        assertEquals(listOf("c1", "c2"), member["centers"])
        assertEquals(listOf("o1", "o2", "o3"), member["organizations"])
    }

    @Test
    fun `days are counted by calendar day`() {
        val midnight = 1_799_971_200L // a UTC midnight
        assertEquals(0, InvitationRules.daysBetween(midnight + 60, midnight + 3600, ZoneOffset.UTC))
        assertEquals(1, InvitationRules.daysBetween(midnight - 60, midnight + 60, ZoneOffset.UTC))
    }

    private fun expect(reason: InvitationException.Reason, block: () -> Unit) {
        try {
            block()
            fail("Expected $reason")
        } catch (e: InvitationException) {
            assertEquals(reason, e.reason)
        }
    }
}
