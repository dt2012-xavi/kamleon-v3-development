package com.dynatech2012.kamleonuserapp.repositories

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthRepository {
    private val auth = Firebase.auth

    fun getUuid(): String?
    {
        return auth.uid;
    }
    suspend fun signup(email: String, pass: String) = suspendCoroutine<Result<CustomUser>> { continuation ->
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    val customUser = CustomUser(email)
                    continuation.resume(Result.success(customUser))
                } else {
                    Log.e(TAG, "createUserWithEmail:failure", task.exception)
                    continuation.resume(Result.failure(Throwable(task.exception)))
                }
            }
    }

    suspend fun login(email: String, pass: String) = suspendCoroutine<Result<CustomUser>> { continuation ->
        Log.d(TAG, "Will try  to login: $email _ $pass")
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "login:success")
                    val user = auth.currentUser
                    val customUser = CustomUser(email)
                    continuation.resume(Result.success(customUser))
                } else {
                    Log.e(TAG, "login:failure", task.exception)
                    continuation.resume(Result.failure(Throwable(task.exception)))
                }
            }
    }

    fun checkLogged(): Boolean {
        val user = auth.currentUser
        return user != null
    }

    fun logout() {
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

    companion object {
        val TAG = FirestoreRepository::class.simpleName
    }
}