package com.dynatech2012.kamleonuserapp.repositories

import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.models.UserStatus
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreRepository {
    private val db = Firebase.firestore
    private lateinit var user: CustomUser

    suspend fun createUserStep1(email: String, fName: String, lName: String
    ): Result<CustomUser> {
        AuthRepository().getUuid()?.let { uuid ->
            val doc = db.collection(Constants.usersCollection).document(uuid)
            user = CustomUser(email, fName, lName, UserStatus.active)
            doc.set(user).await()
            Firebase.auth.currentUser?.let {
                return Result.success(user)
            }
        }
        return Result.failure(Throwable())
    }

    suspend fun createUserStep2(birthday: Date,
                                height: Float, weight: Float, gender: Gender
    ): Result<CustomUser> {
        AuthRepository().getUuid()?.let { uuid ->
            val doc = db.collection(Constants.usersCollection).document(uuid)
            doc.update(hashMapOf("dateOfBirth" to birthday, "height" to height, "weight" to weight, "gender" to gender) as Map<String, Any>).await()
            Firebase.auth.currentUser?.let {
                user.dateOfBirth = birthday
                user.height = height
                user.weight = weight
                user.gender = gender
                return Result.success(user)
            }
        }
        return Result.failure(Throwable())
    }

    suspend fun updateUser(data: HashMap<String, Any>): Result<Boolean> {
        AuthRepository().getUuid()?.let { uuid ->
            val doc = db.collection(Constants.usersCollection).document(uuid)
            doc.update(data).await()
            return Result.success(true)
        }
        return Result.failure(Throwable())
    }

    suspend fun getUserData(): Result<CustomUser> {
        AuthRepository().getUuid()?.let { uuid ->
            val doc = db.collection(Constants.usersCollection).document(uuid)
            val user: CustomUser? = doc.get().await().toObject<CustomUser>()?.let {
                return Result.success(it)
            }
        }
        return Result.failure(Throwable())
    }

    companion object {
        val TAG = FirestoreRepository::class.simpleName
    }
}