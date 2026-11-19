package com.dynatech2012.kamleonuserapp.repositories

import android.net.Uri
import android.util.Log
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USERS_COLLECTION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USERS_TOKEN
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.kANALYSIS_TYPE_URINE
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.models.InvitationStatus
import com.dynatech2012.kamleonuserapp.models.KamleonLegal
import com.dynatech2012.kamleonuserapp.models.QRResponse
import com.dynatech2012.kamleonuserapp.models.RawMeasureData
import com.dynatech2012.kamleonuserapp.models.UserStatus
import com.dynatech2012.kamleonuserapp.network.CallKind
import com.dynatech2012.kamleonuserapp.network.NetworkGate
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.google.firebase.Firebase

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirestoreDataSource @Inject constructor(
    private val userRepository: UserRepository,
    private val gate: NetworkGate
) {
    private val db = Firebase.firestore
    private lateinit var user: CustomUser
    private val storage = Firebase.storage

    private val uuid: String?
        get() = if (Constants.DEBUG_MODE) FirebaseConstants.USER_UID_DEBUG else userRepository.uuid

    suspend fun createUserStep1(
        email: String, fName: String, lName: String, birthday: Date, healthConsent: Boolean, appPrivacy: Boolean
    ): Response<CustomUser> {
        uuid?.let { uuid ->
            val doc = db.collection(USERS_COLLECTION).document(uuid)
            val newProfile = CustomUser(
                uuid,
                email,
                fName,
                lName,
                UserStatus.active,
                dateOfBirth = birthday,
                legal = KamleonLegal(healthConsent = healthConsent, privacyPolicyApp = appPrivacy)
            )
            return try {
                val profile = db.runTransaction { transaction ->
                    val existing = transaction.get(doc)
                    if (existing.exists()) {
                        existing.toObject<CustomUser>()
                            ?: throw IllegalStateException("User profile could not be parsed")
                    } else {
                        transaction.set(doc, newProfile)
                        newProfile
                    }
                }.await()
                user = profile
                Response.Success(profile)
            } catch (e: Exception) {
                Response.Failure(e)
            }
        }
        return Response.Failure(Exception("User not logged in"))
    }

    suspend fun createUserStep2(
        height: Float?, weight: Float?, gender: Gender
    ): Response<CustomUser> {
        val userId = uuid ?: return Response.Failure(Exception("User not logged in"))
        return try {
            val doc = db.collection(USERS_COLLECTION).document(userId)
            doc.update(mapOf("height" to height, "weight" to weight, "gender" to gender)).await()
            val result = getUserData()
            if (result is Response.Success) result else Response.Failure(Exception("User profile unavailable"))
        } catch (e: Exception) {
            Response.Failure(e)
        }
    }

    suspend fun updateUser(data: HashMap<String, Any>): Response<Unit> {
        if (uuid == null) {
            return Response.Failure(Exception("User not logged in"))
        }
        DebugLog.i("PIN") { "Updating user profile" }
        DebugLog.i("PIN") { "User profile update requested" }
        val doc = db.collection(USERS_COLLECTION).document(uuid!!)
        return try {
            doc.update(data).await()
            Response.Success(Unit)
        } catch (e: Exception) {
            Response.Failure(e)
        }
    }

    /** [trustMonitor] false: a retry the user asked for, which must try for real even if the monitor says offline. */
    suspend fun getUserData(source: Source = Source.DEFAULT, trustMonitor: Boolean = true): Response<CustomUser> {
        val userId = uuid ?: return Response.Failure(Exception("User not logged in"))
        return gate.safeCall(CallKind.Read, trustMonitor) {
            val snapshot = db.collection(USERS_COLLECTION).document(userId).get(source).await()
            if (!snapshot.exists()) throw UserProfileMissingException()
            val profile = snapshot.toObject<CustomUser>()
                ?: throw IllegalStateException("User profile could not be parsed")
            user = profile
            DebugLog.d(TAG) { "User profile loaded" }
            profile
        }
    }

    suspend fun updateUserImage(uri: Uri): Response<Boolean> {
        uuid?.let { uuid ->
            val doc = db.collection(USERS_COLLECTION).document(uuid)
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
            val doc = db.collection(USERS_COLLECTION).document(uuid)
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

    suspend fun getUserMeasuresNoPag(
        userId: String?,
        lastDate: Long?,
        limit: Long?
    ): Response<ArrayList<MeasureData>> = suspendCoroutine { continuation ->
        val query: Query = db.collection(FirebaseConstants.MEASURES_COLLECTION)
        val date = Date(lastDate?.plus(1) ?: 0)
        val timestamp = com.google.firebase.Timestamp(date)
        DebugLog.d(TAG) { "Fetching measurements from Firestore" }
        val task = query
            .whereEqualTo(FirebaseConstants.kUSERID_FIELD, userId)
            .whereEqualTo(FirebaseConstants.kANALYSIS_TYPE, kANALYSIS_TYPE_URINE)
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
                val measuresRaw: List<RawMeasureData> =
                    snapshot.toObjects(RawMeasureData::class.java)
                Log.d(TAG, "got measures from FS parsed size: ${measuresRaw.size}")
                val measures: ArrayList<MeasureData> =
                    ArrayList(measuresRaw.map { MeasureData(it) })


                /*
                for (m in measures) {
                    val date = Date(m.analysisDate)
                    if (m.analysisDate in 170714827041..1707148270379) {
                        Log.d(TAG, "got measures dates: $m _ id: ${m.measureId} _ prec: ${m.precision} _ isPrec: ${m.isPrecise} _ date: $date")
                    }
                }

                 */

                Log.d(
                    TAG,
                    "got measures from FS 23 converted to MeasureData size: ${measures.size}"
                )
                continuation.resume(Response.Success(measures))
            }
            .addOnFailureListener { e ->
                continuation.resume(Response.Failure(e))
                DebugLog.e(TAG) { "Fetching measurements failed: ${e.javaClass.simpleName}" }
            }
    }

    fun getUserMeasures(
        userId: String?,
        lastDate: Long?,
        limit: Int
    ): Flow<Response<ArrayList<MeasureData>>> = flow {
        var lastPageSize = limit
        var countDebug = 0
        val query: Query = db.collection(FirebaseConstants.MEASURES_COLLECTION)
        DebugLog.d(TAG) { "Fetching paged measurements" }
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
                DebugLog.e(TAG) { "Fetching paged measurements failed: ${e.javaClass.simpleName}" }
                break
            }
        }
    }

    // Invitations

    suspend fun getNewInvitations(): Int {
        val doc = db.collection(USERS_COLLECTION)
            .whereEqualTo("email", userRepository.email)
            .whereEqualTo("status", InvitationStatus.SENT.rawValue)
            .count()
        return try {
            doc.get(AggregateSource.SERVER).await().count.toInt()
        } catch (e: Exception) {
            DebugLog.e(TAG) { "Fetching invitation count failed: ${e.javaClass.simpleName}" }
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
                    DebugLog.d(TAG) { "Updating FCM registration token" }
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

    suspend fun deleteToken(): Response<Boolean> = suspendCoroutine { continuation ->
        if (uuid == null) {
            continuation.resume(Response.Failure(Exception("User not logged in")))
            return@suspendCoroutine
        }
        db.collection(USERS_COLLECTION).document(uuid!!)
            .update(USERS_TOKEN, "")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Response.Success(true))
                } else {
                    continuation.resume(Response.Failure(Exception("Could not delete firebase messaging token")))
                }
            }
    }

    //suspend fun updateLegal(isAdmin: Boolean): Response<Boolean> {
    suspend fun updateLegal(
        policyAdmin: Boolean,
        policyApp: Boolean,
        consent: Boolean
    ): Response<Boolean> {
        if (uuid == null) {
            return Response.Failure(Exception("User not logged in"))
        }
        return try {
            val mutableMap = mutableMapOf<String, Boolean>()
            if (policyAdmin)
                mutableMap["legal.privacyPolicyAdmin"] = true
            if (policyApp)
                mutableMap["legal.privacyPolicyApp"] = true
            if (consent)
                mutableMap["legal.healthConsent"] = true
            val map = mutableMap.toMap()
            db.collection(USERS_COLLECTION).document(uuid!!)
                .update(map).await()
            Log.d(TAG, "update legal success")
            Response.Success(true)
        } catch (e: Exception) {
            DebugLog.e(TAG) { "Updating legal consent failed: ${e.javaClass.simpleName}" }
            Response.Failure(e)
        }
    }

    suspend fun uploadQrId(qrResponse: QRResponse?): Response<Boolean> {
        if (uuid == null) {
            return Response.Failure(Exception("User not logged in"))
        }
        if (qrResponse == null) {
            return Response.Failure(Exception("QR response is null"))
        }
        return try {
            db.collection(FirebaseConstants.QR_COLLECTION).document(qrResponse.unitId).set(
                hashMapOf(
                    "sessionId" to qrResponse.sessionId,
                    "userId" to uuid
                )
            ).await()
            Log.d(TAG, "qr upload success")
            Response.Success(true)
        } catch (e: Exception) {
            DebugLog.e(TAG) { "Uploading scan failed: ${e.javaClass.simpleName}" }
            Response.Failure(e)
        }
    }


    companion object {
        val TAG = FirestoreDataSource::class.simpleName
    }
}
