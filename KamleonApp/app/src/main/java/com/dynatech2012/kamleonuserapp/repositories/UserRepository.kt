package com.dynatech2012.kamleonuserapp.repositories

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.dynatech2012.kamleonuserapp.models.CustomUser
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

class UserRepository {
    private val auth = Firebase.auth

    private var logged = false
    val uuid: String?
        get() = auth.uid
    val email: String?
        get() = auth.currentUser?.email

    val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified ?: true

    suspend fun isEmailUsed(email: String): Boolean {
        val user = auth.fetchSignInMethodsForEmail(email).await()
        return user.signInMethods?.isNotEmpty() ?: false
    }

    suspend fun signup(
        email: String,
        pass: String
    ): Response<CustomUser> {//= suspendCoroutine { continuation ->
        return try {
            auth.createUserWithEmailAndPassword(email, pass).await()
            Log.d(TAG, "createUserWithEmail:success")
            val user = auth.currentUser
            val customUser = CustomUser(email)
            Response.Success(customUser)
        } catch (e: Exception) {
            Log.e(TAG, "createUserWithEmail:failure", e)
            Response.Failure(e)
        }
    }

    suspend fun login(
        email: String,
        pass: String
    ): Response<CustomUser> {// = suspendCoroutine { continuation ->
        Log.d(TAG, "Will try  to login: $email _ $pass")
        try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Log.d(TAG, "login:success")
            val user = auth.currentUser
            val customUser = CustomUser(email)
            return Response.Success(customUser)
        } catch (e: Exception) {
            when (e) {
                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                    // Pass wrong
                    Log.e(TAG, "login:failure: $e _ ${e.cause}", e)
                    return Response.Failure(e)
                }

                is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                    // user invalid
                    Log.e(TAG, "login:failure: $e _ ${e.cause}", e)
                    return Response.Failure(e)
                }

                else -> {
                    Log.e(TAG, "login:failure: $e _ ${e.cause}", e)
                    return Response.Failure(e)
                }
            }
        }
    }

    val checkLogged: Boolean
        get() {
            val user = auth.currentUser
            Log.i(TAG, "checkLogged here: $user")
            val isLogged = user != null

            Log.d(TAG, "checkLogged: $isLogged")
            return isLogged
        }

    suspend fun signOutSuspend() = suspendCoroutine<Unit> { continuation ->
        auth.signOut()
        continuation.resume(Unit)
    }

    val logoutStatus = MutableLiveData<Boolean>()

    val authStateListener = FirebaseAuth.AuthStateListener {
        if (it.currentUser == null) {
            FirebaseInstallations.getInstance().getToken(true)
                .addOnCompleteListener { task: Task<InstallationTokenResult> ->
                    Log.d(TAG, "User is signed out")
                    val tokenS = task.result
                    val tokenS2 = tokenS.token
                    logoutStatus.postValue(true)
                }
        }
    }

    fun removeAuthListener(){
        auth.removeAuthStateListener(authStateListener)
    }

    fun logout() {
        // Resets Instance ID and revokes all tokens.
        auth.addAuthStateListener(authStateListener)
        auth.signOut()
        //auth.removeAuthStateListener(authStateListener)
    }

    suspend fun changeEmail(currentPwd: String, newEmail: String): Response<Unit> {
        val oldEmail = auth.currentUser?.email ?: return Response.Failure(Exception("No email"))
        return try {
            val credential = EmailAuthProvider.getCredential(oldEmail, currentPwd)
            // Prompt the user to re-provide their sign-in credentials
            auth.currentUser?.reauthenticate(credential)?.await()
            auth.currentUser?.updateEmail(newEmail)?.await()
            Response.Success(Unit)
        }
        catch (e: Exception) {
            Response.Failure(e)
        }
    }

    suspend fun changePwd(oldPwd: String, newPwd: String): Response<Unit> {
        val oldEmail = auth.currentUser?.email ?: return Response.Failure(Exception("No email"))
        val credential = EmailAuthProvider
            .getCredential(oldEmail, oldPwd)
        // Prompt the user to re-provide their sign-in credentials
        return try {
            auth.currentUser?.reauthenticate(credential)?.await()
            auth.currentUser?.updatePassword(newPwd)?.await()
            Response.Success(Unit)
        } catch (e: Exception) {
            Response.Failure(e)
        }
    }

    suspend fun deleteUser(): Response<Unit> {
        val user = auth.currentUser ?: return Response.Failure(Exception("No user"))
        return try {
            user.delete().await()
            Response.Success(Unit)
        } catch (e: Exception) {
            Response.Failure(e)
        }
    }

    companion object {
        val TAG = FirestoreDataSource::class.simpleName
    }
}