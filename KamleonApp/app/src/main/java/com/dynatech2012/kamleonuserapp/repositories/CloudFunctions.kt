package com.dynatech2012.kamleonuserapp.repositories

import timber.log.Timber
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.InvitationStatus
import com.dynatech2012.kamleonuserapp.models.Organization
import com.dynatech2012.kamleonuserapp.network.CallKind
import com.dynatech2012.kamleonuserapp.network.NetworkGate
import com.google.firebase.Firebase
import com.google.firebase.functions.functions
import kotlinx.coroutines.tasks.await

/**
 *
 * <font color="teal">
 * Edu: <br></br>
 * Http request to notify firebase that an invitation has been accepted or denied.
 * This will fire a cloud function in firebase
</font> */
class CloudFunctions(
    private val userRepository: UserRepository,
    private val gate: NetworkGate
) {
    private val functions = Firebase.functions
    private val uuid: String?
        get() = if (Constants.DEBUG_MODE) FirebaseConstants.USER_UID_DEBUG else userRepository.uuid
    private val email: String?
        get() = userRepository.email

    // Invitations
    suspend fun getInvitations(): Response<ArrayList<Invitation>> {
        Timber.d("Loading invitations")
        val userEmail = email ?: return Response.Failure(Exception("User not logged in"))
        val body = hashMapOf("email" to userEmail)
        return gate.safeCall(CallKind.Read) {
            val result = functions.getHttpsCallable("getInvitationsForUser").call(body).await()
            val data = result?.data as? ArrayList<HashMap<String, Any>> ?: ArrayList()
            val invitations = ArrayList<Invitation>()
            data.forEach { invitations.add(Invitation(it)) }
            Timber.d("Invitations loaded: count=%s", invitations.size)
            invitations.sortByDescending { it.dateSent }
            invitations
        }
    }

    suspend fun acceptInvitation(
        invitationId: String,
        optional: Boolean
    ): ResponseNullable<Nothing> {
        return changeInvitationStatus(invitationId, InvitationStatus.ACCEPTED, optional)
    }

    suspend fun rejectInvitation(invitationId: String): ResponseNullable<Nothing> {
        return changeInvitationStatus(invitationId, InvitationStatus.REJECTED, false)
    }

    private suspend fun changeInvitationStatus(
        invitationId: String,
        status: InvitationStatus,
        optional: Boolean?
    ): ResponseNullable<Nothing> {
        val userId = uuid ?: return ResponseNullable.Failure(Exception("User not logged in"))
        val body = hashMapOf(
            "invitationID" to invitationId,
            "userID" to userId,
            "status" to status.rawValue,
            "roster" to (optional ?: false)
        )
        Timber.d("Changing invitation status")
        return gate.safeCallNullable(CallKind.Action) {
            functions.getHttpsCallable("changeInvitationStatus").call(body).await()
            Timber.d("Invitation status changed")
        }
    }

    // User profiles
    suspend fun getUserProfiles(): Response<ArrayList<Organization>> {
        Timber.d("Loading user profiles")
        val userId = uuid ?: return Response.Failure(Exception("User not logged in"))
        val body = hashMapOf("id" to userId)
        return gate.safeCall(CallKind.Read) {
            val result = functions.getHttpsCallable("getUserProfiles").call(body).await()
            val data = result?.data as? ArrayList<HashMap<String, Any>> ?: ArrayList()
            val organizations = ArrayList<Organization>()
            data.forEach { organizations.add(Organization(it)) }
            Timber.d("User profiles loaded: count=%s", organizations.size)
            organizations
        }
    }

    suspend fun resetPwd(email: String): ResponseNullable<Nothing> =
        callEmailFunction("sendPasswordEmail", hashMapOf("email" to email), "Reset Password email sent successfully")

    suspend fun resetPin(email: String, userId: String): ResponseNullable<Nothing> =
        callEmailFunction("sendPINEmail", hashMapOf("email" to email, "id" to userId), "Reset PIN email sent successfully")

    suspend fun sendVerificationEmail(
        userId: String,
        email: String,
        username: String
    ): ResponseNullable<Nothing> =
        callEmailFunction("sendEmailVerifyEmail", hashMapOf("id" to userId, "email" to email, "username" to username), null)

    suspend fun sendTrialEmail(email: String): ResponseNullable<Nothing> =
        callEmailFunction("sendPremiumInterestEmail", hashMapOf("email" to email), null)

    /** A function that sends an email: an action. [expectedMessage], when given, is what a successful answer says. */
    private suspend fun callEmailFunction(
        name: String,
        body: HashMap<String, String>,
        expectedMessage: String?
    ): ResponseNullable<Nothing> = gate.safeCallNullable(CallKind.Action) {
        val result = functions.getHttpsCallable(name).call(body).await()
        val message = (result.data as? HashMap<String, String>)?.get("message")
        if (expectedMessage != null && message != expectedMessage) throw Exception("$message")
        Timber.d("Email function %s answered", name)
    }

    companion object {
        val TAG: String = CloudFunctions::class.java.simpleName
    }
}