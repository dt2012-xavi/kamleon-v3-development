package com.dynatech2012.kamleonuserapp.repositories

import android.net.Uri
import android.util.Log
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USERS_COLLECTION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USERS_TOKEN
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.models.InvitationStatus
import com.dynatech2012.kamleonuserapp.models.RawMeasureData
import com.dynatech2012.kamleonuserapp.models.UserStatus
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirestoreDataSource @Inject constructor(private val userRepository: UserRepository) {
    private val db = Firebase.firestore
    private lateinit var user: CustomUser
    private val storage = Firebase.storage

    private val uuid: String?
        get() = if (Constants.DEBUG_MODE) FirebaseConstants.USER_UID_DEBUG else userRepository.uuid

    suspend fun createUserStep1(email: String, fName: String, lName: String
    ): Response<CustomUser> {
        uuid?.let { uuid ->
            val doc = db.collection(FirebaseConstants.USERS_COLLECTION).document(uuid)
            user = CustomUser(uuid, email, fName, lName, UserStatus.active)
            doc.set(user).await()
            Firebase.auth.currentUser?.let {
                return Response.Success(user)
            }
        }
        return Response.Failure(Exception())
    }

    suspend fun createUserStep2(birthday: Date,
                                height: Float, weight: Float, gender: Gender
    ): Response<CustomUser> {
        uuid?.let { uuid ->
            val doc = db.collection(FirebaseConstants.USERS_COLLECTION).document(uuid)
            doc.update(hashMapOf("dateOfBirth" to birthday, "height" to height, "weight" to weight, "gender" to gender) as Map<String, Any>).await()
            Firebase.auth.currentUser?.let {
                user.dateOfBirth = birthday
                user.height = height
                user.weight = weight
                user.gender = gender
                return Response.Success(user)
            }
        }
        return Response.Failure(Exception())
    }

    suspend fun updateUser(data: HashMap<String, Any>): Response<Boolean> {
        uuid?.let { uuid ->
            val doc = db.collection(FirebaseConstants.USERS_COLLECTION).document(uuid)
            doc.update(data).await()
            return Response.Success(true)
        }
        return Response.Failure(Exception())
    }

    suspend fun getUserData(): Response<CustomUser> {
        if (uuid == null) {
            return Response.Failure(Exception("User not logged in"))
        }
        val doc = db.collection(FirebaseConstants.USERS_COLLECTION).document(uuid!!)
        doc.get().await().toObject<CustomUser>()?.let { u ->
            user = u
            Log.d(TAG, "getUserData token: ${u.token}")
            return Response.Success(u)
        }
        return Response.Failure(Exception("User parsing error"))
    }

    suspend fun updateUserImage(uri: Uri): Response<Boolean> {
        uuid?.let { uuid ->
            val doc = db.collection(FirebaseConstants.USERS_COLLECTION).document(uuid)
            val ref = storage.reference.child("users/$uuid/profile.jpeg")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await()
            doc.update(hashMapOf("imageUrl" to url.toString()) as Map<String, Any>).await()
            return Response.Success(true)
        }
        return Response.Failure(Exception())
    }

    suspend fun removeUserImage(): Response<Boolean> {
        uuid?.let { uuid ->
            val doc = db.collection(FirebaseConstants.USERS_COLLECTION).document(uuid)
            val ref = storage.reference.child("users/$uuid/profile.jpeg")
            ref.delete().await()
            doc.update(hashMapOf("imageUrl" to "") as Map<String, Any>).await()
            return Response.Success(true)
        }
        return Response.Failure(Exception())
    }

    suspend fun getUserLastMeasure(userId: String?): Response<ArrayList<MeasureData>> {
        return getUserMeasuresNoPag(userId, null, 1)
    }
    suspend fun getUserMeasuresNoPag(userId: String?, lastDate: Long?, limit: Long?): Response<ArrayList<MeasureData>> = suspendCoroutine { continuation ->
        val query: Query = db.collection(FirebaseConstants.MEASURES_COLLECTION)
        val date = Date(lastDate ?: 0)
        val timestamp = com.google.firebase.Timestamp(date)
        Log.d(TAG, "got measures from FS last date: $date")
        val task = query
            .whereEqualTo(FirebaseConstants.kUSERID_FIELD, userId)
            .whereGreaterThan(
                FirebaseConstants.kANALYSIS_DATE,
                timestamp
            )
            .orderBy(FirebaseConstants.kANALYSIS_DATE, Query.Direction.DESCENDING)
        if (limit != null)
            task.limit(limit)
        task.get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "got measures from FS size: ${snapshot.documents.size}")
                val measuresRaw: List<RawMeasureData> = snapshot.toObjects(RawMeasureData::class.java)
                Log.d(TAG, "got measures from FS parsed size: ${measuresRaw.size}")
                val measures: ArrayList<MeasureData> = ArrayList(measuresRaw.map { MeasureData(it) })

                Log.d(TAG, "got measures from FS 23 converted to MeasureData size: ${measures.size}")
                continuation.resume(Response.Success(measures))
            }
            .addOnFailureListener { e ->
                continuation.resume(Response.Failure(e))
                Log.e(TAG, "got measures from FS error: ${e.message}")
            }
    }

    fun getUserMeasures(userId: String?, lastDate: Long?, limit: Int): Flow<Response<ArrayList<MeasureData>>> = flow {
        var lastPageSize = limit
        var countDebug = 0
        val query: Query = db.collection(FirebaseConstants.MEASURES_COLLECTION)
        Log.d(TAG, "got measures 2 last date: $lastDate")
        val date = lastDate ?: 0
        var lastVisible: DocumentSnapshot? = null
        val measures = arrayListOf<MeasureData>()
        while (lastPageSize == limit) {
            val task = query.whereEqualTo(FirebaseConstants.kUSERID_FIELD, userId)
            if (lastDate != null) {
                task.whereGreaterThan(
                    FirebaseConstants.kANALYSIS_DATE,
                    date
                )
            }
            task.orderBy(
                FirebaseConstants.kANALYSIS_DATE,
                Query.Direction.DESCENDING
            )
            /*
            if (lastVisible != null)
                task.startAfter(lastVisible)
            */
            task.limit(limit.toLong())

            try {
                val documentSnapshot = task.get().await()
                Log.d(TAG, "got measures 66 size: ${documentSnapshot.documents.size}")
                Log.d(TAG, "got measures 21 counter of times requested: ${countDebug + 1}")
                //val newPage = documentSnapshot.toObjects(RawMeasureData::class.java) as ArrayList<MeasureData>

                val measuresRawPage: List<RawMeasureData> =
                    documentSnapshot.toObjects(RawMeasureData::class.java)
                Log.d(TAG, "got measures 99 size: ${measuresRawPage.size}")
                val measuresPage: ArrayList<MeasureData> =
                    ArrayList(measuresRawPage.map { MeasureData(it) })

                Log.d(TAG, "got measures 22 size of measures got: ${measuresRawPage.size}")
                measures.addAll(measuresPage)
                lastPageSize = 0//measuresRawPage.size ///0
                countDebug++
                if (lastPageSize < limit) {
                    emit(Response.Success(measures))
                    break
                }
                lastVisible = documentSnapshot.documents[documentSnapshot.size() - 1]
            } catch (e: Exception) {
                emit(Response.Failure(e))
                Log.e(TAG, "got measures 23 last date: ${e.message}")
                break
            }
        }
    }

    // Invitations

    suspend fun getNewInvitations(): Int {
        val doc = db.collection(FirebaseConstants.USERS_COLLECTION)
            .whereEqualTo("email", userRepository.email)
            .whereEqualTo("status", InvitationStatus.SENT.rawValue)
            .count()
        return try {
            doc.get(AggregateSource.SERVER).await().count.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "getNewInvitations error: ${e.message}")
            0
        }
    }

    suspend fun updateToken(): Response<Boolean> = suspendCoroutine { continuation ->
        if (uuid == null) {
            continuation.resume(Response.Failure(Exception("User not logged in")))
            return@suspendCoroutine
        }
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task1 ->
                if (!task1.isSuccessful) {
                    continuation.resume(Response.Failure(Exception("Could not get firebase messaging token")))
                } else {
                    // Get new FCM registration token
                    val token = task1.result
                    Log.d(TAG, "updateToken token: $token")
                    db.collection(USERS_COLLECTION).document(uuid!!)
                        .update(USERS_TOKEN, token)
                        .addOnCompleteListener { task2 ->
                            if (task2.isSuccessful) {
                                continuation.resume(Response.Success(true))
                            } else {
                                continuation.resume(Response.Failure(Exception("Could not update firebase messaging token")))
                            }
                        }
                }
            }
    }

    companion object {
        val TAG = FirestoreDataSource::class.simpleName
    }
}