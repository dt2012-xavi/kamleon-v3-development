package com.dynatech2012.kamleonuserapp.repositories

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.installations.InstallationTokenResult
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserRepository {
    private val auth = Firebase.auth

    private var logged = false
    val uuid: String?
        get() = auth.uid

    suspend fun signup(email: String, pass: String) = suspendCoroutine { continuation ->
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    val customUser = CustomUser(email)
                    continuation.resume(Response.Success(customUser))
                } else {
                    Log.e(TAG, "createUserWithEmail:failure", task.exception)
                    continuation.resume(Response.Failure(task.exception ?: Exception()))
                }
            }
    }

    suspend fun login(email: String, pass: String) = suspendCoroutine { continuation ->
        Log.d(TAG, "Will try  to login: $email _ $pass")
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "login:success")
                    val user = auth.currentUser
                    val customUser = CustomUser(email)
                    continuation.resume(Response.Success(customUser))
                } else {
                    Log.e(TAG, "login:failure", task.exception)
                    continuation.resume(Response.Failure(task.exception ?: Exception()))
                }
            }
    }

    val checkLogged: Boolean
        get() {

            val user = auth.currentUser
            val isLogged = user != null


            Log.d(TAG, "checkLogged: $isLogged")
            return isLogged
        }

    fun logout() {
        // Resets Instance ID and revokes all tokens.
        FirebaseInstallations.getInstance().getToken(true)
            .addOnCompleteListener { task: Task<InstallationTokenResult> ->
                val tokenS = task.result
                val tokenS2 = tokenS.token
                Log.d(TAG, "token app: $tokenS")
            }
        auth.addAuthStateListener {
            if (auth.currentUser != null) {
                logged = true
                Log.d(TAG, "User is signed in.")
            } else {
                logged = false
                Log.d(TAG, "No user is signed in.")
            }
        }
        auth.signOut()
    }

    suspend fun changeEmail(currentPwd: String, newEmail: String) {
        val oldEmail = auth.currentUser?.email
        if (oldEmail != null) {
            val credential = EmailAuthProvider
                .getCredential(oldEmail, currentPwd)
            // Prompt the user to re-provide their sign-in credentials
            auth.currentUser?.reauthenticate(credential)?.await()
            auth.currentUser?.updateEmail(newEmail)?.await()
        }
    }

    suspend fun changePwd(oldPwd: String, newPwd: String) {
        val oldEmail = auth.currentUser?.email
        if (oldEmail != null) {
            val credential = EmailAuthProvider
                .getCredential(oldEmail, oldPwd)
            // Prompt the user to re-provide their sign-in credentials
            auth.currentUser?.reauthenticate(credential)?.await()
            auth.currentUser?.updatePassword(newPwd)?.await()
        }
    }

    suspend fun deleteUser() {
        auth.currentUser?.delete()?.await()
    }

    companion object {
        val TAG = FirestoreDataSource::class.simpleName
    }
}