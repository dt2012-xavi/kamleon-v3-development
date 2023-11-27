package com.dynatech2012.kamleonuserapp.repositories

import android.net.Uri
import android.util.Log
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.RealtimeConstants
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.models.RawMeasureData
import com.dynatech2012.kamleonuserapp.models.UserStatus
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
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
        get() = if (Constants.DEBUG_MODE) RealtimeConstants.USER_UID_DEBUG else userRepository.uuid

    suspend fun createUserStep1(email: String, fName: String, lName: String
    ): Response<CustomUser> {
        uuid?.let { uuid ->
            val doc = db.collection(Constants.usersCollection).document(uuid)
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
            val doc = db.collection(Constants.usersCollection).document(uuid)
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
            val doc = db.collection(Constants.usersCollection).document(uuid)
            doc.update(data).await()
            return Response.Success(true)
        }
        return Response.Failure(Exception())
    }

    suspend fun getUserData(): Response<CustomUser> {
        uuid?.let { uuid ->
            val doc = db.collection(Constants.usersCollection).document(uuid)
            val user: CustomUser? = doc.get().await().toObject<CustomUser>()?.let {
                return Response.Success(it)
            }
        }
        return Response.Failure(Exception())
    }

    suspend fun updateUserImage(uri: Uri): Response<Boolean> {
        uuid?.let { uuid ->
            val doc = db.collection(Constants.usersCollection).document(uuid)
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
            val doc = db.collection(Constants.usersCollection).document(uuid)
            val ref = storage.reference.child("users/$uuid/profile.jpeg")
            ref.delete().await()
            doc.update(hashMapOf("imageUrl" to "") as Map<String, Any>).await()
            return Response.Success(true)
        }
        return Response.Failure(Exception())
    }


    /*
            val query: Query = db.collection(Constants.measuresCollection)
        Log.d(TAG, "got measures 2 last date: $lastDate")
        val task = query.whereEqualTo(Constants.kUSERID_FIELD, userId)
            .whereGreaterThan(
                Constants.kANALYSIS_DATE,
                lastDate
            )
            .orderBy(Constants.kANALYSIS_DATE,
                Query.Direction.DESCENDING)
            .limit(1)
        try {
            val snapshot: QuerySnapshot = task.get().await()
            Log.d(TAG, "got measures 3")
            val list = snapshot.toObjects(RawMeasureData::class.java)
            val measures = ArrayList(list.mapNotNull { MeasureData(it) })
            Log.d(TAG, "got measures 3 size ${list.size}")
            database.saveMeasures(measures)
            trySend(Response.Success(measures as ArrayList<MeasureData>))
        } catch (e: Exception) {
            throw e
            trySend(Response.Failure(e))
        }
     */

    suspend fun getUserLastMeasure(userId: String?): Response<ArrayList<MeasureData>> {
        return getUserMeasuresNoPag(userId, null, 1)
    }
    suspend fun getUserMeasuresNoPag(userId: String?, lastDate: Long?, limit: Long?): Response<ArrayList<MeasureData>> = suspendCoroutine { continuation ->
        val query: Query = db.collection(Constants.measuresCollection)
        val date = Date(lastDate ?: 0)
        val timestamp = com.google.firebase.Timestamp(date)
        Log.d(TAG, "got measures from FS last date: $date")
        val task = query
            .whereEqualTo(Constants.kUSERID_FIELD, userId)
            .whereGreaterThan(
                Constants.kANALYSIS_DATE,
                timestamp
            )
            .orderBy(Constants.kANALYSIS_DATE, Query.Direction.DESCENDING)
        if (limit != null)
            task.limit(limit)
        task.get()
            .addOnSuccessListener {
                Log.d(TAG, "got measures from FS size: ${it.documents.size}")
                val measuresRaw: List<RawMeasureData> = it.toObjects(RawMeasureData::class.java)
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
        val query: Query = db.collection(Constants.measuresCollection)
        Log.d(TAG, "got measures 2 last date: $lastDate")
        val date = lastDate ?: 0
        var lastVisible: DocumentSnapshot? = null
        val measures = arrayListOf<MeasureData>()
        while (lastPageSize == limit) {
            val task = query.whereEqualTo(Constants.kUSERID_FIELD, userId)
            if (lastDate != null) {
                task.whereGreaterThan(
                    Constants.kANALYSIS_DATE,
                    date
                )
            }
            task.orderBy(
                Constants.kANALYSIS_DATE,
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

    companion object {
        val TAG = FirestoreDataSource::class.simpleName
    }
}