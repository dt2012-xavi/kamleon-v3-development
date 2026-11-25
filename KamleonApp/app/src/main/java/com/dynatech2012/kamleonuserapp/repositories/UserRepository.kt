package com.dynatech2012.kamleonuserapp.repositories

import timber.log.Timber
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.network.CallKind
import com.dynatech2012.kamleonuserapp.network.NetworkGate
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.installations.InstallationTokenResult
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserRepository(private val gate: NetworkGate) {
    private val auth = Firebase.auth

    private var logged = false
    val uuid: String?
        get() = auth.uid
    val email: String?
        get() = auth.currentUser?.email

    val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified ?: true

    /** Asks the server about the signed-in user again; null when that is not possible (no user, no network, no answer in time). */
    suspend fun reloadEmailVerified(): Boolean? {
        val user = auth.currentUser ?: return null
        return gate.safeCall(CallKind.Read, timeoutMs = EMAIL_RELOAD_TIMEOUT_MS) {
            user.reload().await()
            auth.currentUser?.isEmailVerified ?: throw IllegalStateException("No signed-in user")
        }.dataValue
    }

    suspend fun signup(email: String, pass: String): Response<CustomUser> = gate.safeCall(CallKind.Action) {
        auth.createUserWithEmailAndPassword(email, pass).await()
        Timber.d("Firebase account creation succeeded")
        CustomUser(email)
    }

    suspend fun login(email: String, pass: String): Response<CustomUser> = gate.safeCall(CallKind.Action) {
        Timber.d("Firebase sign-in requested")
        auth.signInWithEmailAndPassword(email, pass).await()
        Timber.d("Firebase sign-in succeeded")
        CustomUser(email)
    }

    val checkLogged: Boolean
        get() {
            val user = auth.currentUser
            val isLogged = user != null
            Timber.d("Firebase session present: %s", isLogged)
            return isLogged
        }

    suspend fun signOutSuspend() = suspendCoroutine<Unit> { continuation ->
        auth.signOut()
        continuation.resume(Unit)
    }

    /**
     * Signs the user out. [FirebaseAuth.signOut] is synchronous: when this returns there is no
     * current user, so callers can move on without waiting for an auth state callback.
     */
    fun logout() {
        auth.signOut()
        Timber.d("Firebase user signed out")
        // Best effort: the result is not used, and with no network it fails (reading task.result of a
        // failed task throws, which crashed the logout without a connection).
        FirebaseInstallations.getInstance().getToken(true)
            .addOnCompleteListener { task: Task<InstallationTokenResult> ->
                Timber.d("Installation token refresh after sign-out: success=%s", task.isSuccessful)
            }
    }

    suspend fun changePwd(oldPwd: String, newPwd: String): Response<Unit> {
        val oldEmail = auth.currentUser?.email ?: return Response.Failure(Exception("No email"))
        val credential = EmailAuthProvider.getCredential(oldEmail, oldPwd)
        return gate.safeCall(CallKind.Action) {
            // Prompt the user to re-provide their sign-in credentials
            auth.currentUser?.reauthenticate(credential)?.await()
            auth.currentUser?.updatePassword(newPwd)?.await()
            Unit
        }
    }

    companion object {
        val TAG = FirestoreDataSource::class.simpleName

        /** The startup check does not wait longer than this to learn whether the email was verified. */
        private const val EMAIL_RELOAD_TIMEOUT_MS = 5_000L
    }
}
