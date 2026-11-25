package com.dynatech2012.kamleonuserapp.repositories

import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.CENTERS_COLLECTION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.INVITATIONS_COLLECTION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.ORGANIZATIONS_COLLECTION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.TEAMS_COLLECTION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USERS_COLLECTION
import com.dynatech2012.kamleonuserapp.models.InvitationException
import com.dynatech2012.kamleonuserapp.models.InvitationException.Reason
import com.dynatech2012.kamleonuserapp.models.InvitationItem
import com.dynatech2012.kamleonuserapp.models.InvitationRecord
import com.dynatech2012.kamleonuserapp.models.InvitationRules
import com.dynatech2012.kamleonuserapp.network.CallKind
import com.dynatech2012.kamleonuserapp.network.NetworkGate
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's invitations, read and answered straight in Firestore the way the web does: the list
 * is the `invitations` with the user's e-mail, accepting writes the permission and the invitation in
 * one transaction, and declining, reading and dismissing update the invitation.
 */
@Singleton
class InvitationsDataSource @Inject constructor(
    private val userRepository: UserRepository,
    private val gate: NetworkGate
) {
    private val db = Firebase.firestore

    /** Names of organizations, centers, teams and senders, kept while the app runs. */
    private val names = ConcurrentHashMap<String, String>()

    private val email: String
        get() = InvitationRules.normalizeEmail(userRepository.email ?: throw IllegalStateException("User not logged in"))

    private val uid: String
        get() = userRepository.uuid ?: throw IllegalStateException("User not logged in")

    private fun now(): Long = System.currentTimeMillis() / 1000

    /** All the user's invitations: the pending ones first, then the previous ones. */
    suspend fun load(): Response<List<InvitationItem>> = gate.safeCall(CallKind.Read) {
        val snapshot = db.collection(INVITATIONS_COLLECTION).whereEqualTo("email", email).get().await()
        // A document that cannot be read is skipped instead of hiding the whole list.
        val records = snapshot.documents.mapNotNull { doc -> runCatching { record(doc) }.getOrNull() }
        val now = now()
        coroutineScope {
            records.flatMap {
                listOf(
                    "users" to it.senderID, ORGANIZATIONS_COLLECTION to it.organizationID,
                    CENTERS_COLLECTION to it.centerID, TEAMS_COLLECTION to it.teamID
                )
            }.distinct().map { (collection, id) -> async { loadName(collection, id) } }.awaitAll()
        }
        InvitationItem.build(records, now) { collection, id -> names[key(collection, id)] }
    }

    /** Declines a pending invitation of this user. Nothing else is written. */
    suspend fun decline(id: String): Response<Unit> = gate.safeCall(CallKind.Action) {
        val ref = db.collection(INVITATIONS_COLLECTION).document(id)
        val doc = ref.get().await()
        if (!doc.exists()) throw InvitationException(Reason.NOT_FOUND)
        InvitationRules.checkDeclinable(record(doc), email)
        ref.update(mapOf("status" to "rejected", "dateRejected" to FieldValue.serverTimestamp())).await()
        Unit
    }

    /**
     * Accepts a pending invitation: in one transaction, the permission it grants is written to the
     * user and the invitation is marked accepted. Afterwards, on a best-effort basis, other accepted
     * invitations to the same team with another role are removed and the user's `member` is rebuilt.
     */
    suspend fun accept(id: String): Response<Unit> = gate.safeCall(CallKind.Action) {
        val invitationRef = db.collection(INVITATIONS_COLLECTION).document(id)
        val userRef = db.collection(USERS_COLLECTION).document(uid)
        val userEmail = email
        val accepted = db.runTransaction { tx ->
            val invitationDoc = tx.get(invitationRef)
            val userDoc = tx.get(userRef)
            if (!invitationDoc.exists()) throw InvitationException(Reason.NOT_FOUND)
            val invitation = record(invitationDoc)
            InvitationRules.checkAcceptable(invitation, userEmail, now())
            if (!userDoc.exists()) throw InvitationException(Reason.USER_NOT_FOUND)
            val field = InvitationRules.permissionField(invitation) ?: throw InvitationException(Reason.UNKNOWN_ROLE)
            val entry = InvitationRules.permissionEntry(invitation, userDoc.get("teamsPermissions") as? Map<*, *>)
            tx.update(userRef, field, entry)
            tx.update(invitationRef, mapOf("status" to "accepted", "dateAccepted" to FieldValue.serverTimestamp()))
            invitation
        }.await()
        if (InvitationRules.isTeamRole(accepted.userRole)) removeOtherTeamRoles(accepted, userEmail)
        rebuildMember(userRef)
        Unit
    }

    /** Marks these pending invitations as seen, so they no longer show the dot. */
    suspend fun markRead(ids: List<String>) {
        if (ids.isEmpty()) return
        runCatching {
            val batch = db.batch()
            ids.forEach { batch.update(db.collection(INVITATIONS_COLLECTION).document(it), "uiStatus", "read") }
            batch.commit().await()
        }.onFailure { DebugLog.d(TAG) { "Invitations not marked as read: ${it.javaClass.simpleName}" } }
    }

    /** Hides an expired invitation of this user. */
    suspend fun dismiss(id: String): Response<Unit> = gate.safeCall(CallKind.Action) {
        val ref = db.collection(INVITATIONS_COLLECTION).document(id)
        val doc = ref.get().await()
        if (!doc.exists()) throw InvitationException(Reason.NOT_FOUND)
        if (InvitationRules.normalizeEmail(doc.getString("email").orEmpty()) != email) {
            throw InvitationException(Reason.EMAIL_MISMATCH)
        }
        ref.update("uiStatus", "dismissed").await()
        Unit
    }

    private suspend fun removeOtherTeamRoles(accepted: InvitationRecord, userEmail: String) {
        runCatching {
            db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("email", userEmail)
                .whereEqualTo("teamID", accepted.teamID)
                .whereEqualTo("userType", "teamstaff")
                .whereEqualTo("status", "accepted")
                .whereNotEqualTo("userRole", accepted.userRole)
                .get().await()
                .documents.forEach { it.reference.delete().await() }
        }.onFailure { DebugLog.d(TAG) { "Other team roles not removed: ${it.javaClass.simpleName}" } }
    }

    private suspend fun rebuildMember(userRef: com.google.firebase.firestore.DocumentReference) {
        runCatching {
            val data = userRef.get().await().data ?: return
            userRef.update("member", InvitationRules.memberOf(data)).await()
        }.onFailure { DebugLog.d(TAG) { "Member not rebuilt: ${it.javaClass.simpleName}" } }
    }

    private suspend fun loadName(collection: String, id: String) {
        if (id.isBlank() || names.containsKey(key(collection, id))) return
        // A document the user cannot read (or that no longer exists) just has no name.
        runCatching {
            val doc = db.collection(collection).document(id).get().await()
            val name = if (collection == USERS_COLLECTION) {
                listOf(doc.getString("name"), doc.getString("lastName")).filterNot { it.isNullOrBlank() }.joinToString(" ")
            } else doc.getString("name").orEmpty()
            if (name.isNotBlank()) names[key(collection, id)] = name.trim()
        }
    }

    private fun key(collection: String, id: String) = "$collection/$id"

    private fun record(doc: DocumentSnapshot): InvitationRecord =
        InvitationRecord.from(doc.id, doc.data ?: emptyMap()) { (it as? Timestamp)?.seconds }

    private companion object {
        const val TAG = "InvitationsDataSource"
    }
}
