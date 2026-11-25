package com.dynatech2012.kamleonuserapp.repositories

import android.net.Uri
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.CENTERS_COLLECTION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.ORGANIZATIONS_COLLECTION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.TEAMS_COLLECTION
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
import com.dynatech2012.kamleonuserapp.models.RoleEntity
import com.dynatech2012.kamleonuserapp.models.RoleGroup
import com.dynatech2012.kamleonuserapp.models.RolePermission
import com.dynatech2012.kamleonuserapp.models.UserRoles
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
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
            return gate.safeCall(CallKind.Action) {
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
                profile
            }
        }
        return Response.Failure(Exception("User not logged in"))
    }

    suspend fun createUserStep2(
        height: Float?, weight: Float?, gender: Gender
    ): Response<CustomUser> {
        val userId = uuid ?: return Response.Failure(Exception("User not logged in"))
        val doc = db.collection(USERS_COLLECTION).document(userId)
        val update = gate.safeCall(CallKind.Action) {
            doc.update(mapOf("height" to height, "weight" to weight, "gender" to gender)).await()
            Unit
        }
        if (update is Response.Failure) return update
        val result = getUserData()
        return if (result is Response.Success) result else Response.Failure(Exception("User profile unavailable"))
    }

    suspend fun updateUser(data: HashMap<String, Any>): Response<Unit> {
        val userId = uuid ?: return Response.Failure(Exception("User not logged in"))
        DebugLog.i("PIN") { "Updating user profile" }
        DebugLog.i("PIN") { "User profile update requested" }
        val doc = db.collection(USERS_COLLECTION).document(userId)
        return gate.safeCall(CallKind.Action) {
            doc.update(data).await()
            Unit
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

    /**
     * The user's roles as the web's account details list them: the permission maps of the user
     * document, with the name (and the organizations' logo) of what each one points at.
     */
    suspend fun getUserRoles(): Response<List<RoleGroup>> {
        val userId = uuid ?: return Response.Failure(Exception("User not logged in"))
        return gate.safeCall(CallKind.Read) {
            val snapshot = db.collection(USERS_COLLECTION).document(userId).get().await()
            val permissions = RolePermission.allFrom(snapshot.data ?: emptyMap())
            val ids = UserRoles.idsOf(permissions)
            coroutineScope {
                val organizations = async { roleEntities(ORGANIZATIONS_COLLECTION, ids.organizations) }
                val centers = async { roleEntities(CENTERS_COLLECTION, ids.centers) }
                val teams = async { roleEntities(TEAMS_COLLECTION, ids.teams) }
                UserRoles.build(permissions, organizations.await(), centers.await(), teams.await())
            }
        }
    }

    /** The documents of [collection] with these [ids], read one by one; the missing ones are left out. */
    private suspend fun roleEntities(collection: String, ids: Set<String>): Map<String, RoleEntity> =
        coroutineScope {
            ids.map { id -> async { id to db.collection(collection).document(id).get().await() } }
                .awaitAll()
                .filter { (_, doc) -> doc.exists() }
                .associate { (id, doc) -> id to RoleEntity(doc.getString("name").orEmpty(), doc.getString("imageUrl")) }
        }

    /**
     * Saves a new profile photo the way the web does: [full] to `profile.jpeg` and [thumbnail] to
     * `profile_thumbnail.jpeg`, uploaded at the same time and overwriting the previous ones, then
     * one update of `imageUrl` and `imageUrlThumbnail` (no other field). If the full photo fails,
     * nothing is written. If only the thumbnail fails, `imageUrl` is still saved and
     * `imageUrlThumbnail` is left as it was.
     */
    suspend fun updateUserImage(full: Uri, thumbnail: Uri): Response<Boolean> {
        val userId = uuid ?: return Response.Failure(Exception())
        val doc = db.collection(USERS_COLLECTION).document(userId)
        val fullRef = storage.reference.child(profilePhotoPath(userId))
        val thumbnailRef = storage.reference.child(profileThumbnailPath(userId))
        val metadata = storageMetadata { contentType = "image/jpeg" }
        // A photo can take longer than the default time limit on a slow connection.
        return gate.safeCall(CallKind.Action, timeoutMs = IMAGE_UPLOAD_TIMEOUT_MS) {
            coroutineScope {
                val thumbnailUrl = async {
                    try {
                        thumbnailRef.putFile(thumbnail, metadata).await()
                        thumbnailRef.downloadUrl.await().toString()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        DebugLog.e(TAG) { "Profile thumbnail upload failed: ${e.javaClass.simpleName}" }
                        null
                    }
                }
                fullRef.putFile(full, metadata).await()
                val fields = mutableMapOf<String, Any>(IMAGE_URL to fullRef.downloadUrl.await().toString())
                thumbnailUrl.await()?.let { fields[IMAGE_URL_THUMBNAIL] = it }
                doc.update(fields).await()
            }
            true
        }
    }

    /**
     * Removes the profile photo the way the web does: deletes both files (one already missing, or
     * one that cannot be deleted, does not stop it) and empties `imageUrl` and `imageUrlThumbnail`.
     */
    suspend fun removeUserImage(): Response<Boolean> {
        val userId = uuid ?: return Response.Failure(Exception())
        val doc = db.collection(USERS_COLLECTION).document(userId)
        return gate.safeCall(CallKind.Action) {
            coroutineScope {
                listOf(profilePhotoPath(userId), profileThumbnailPath(userId)).map { path ->
                    async { deleteIgnoringErrors(path) }
                }.awaitAll()
            }
            doc.update(mapOf<String, Any>(IMAGE_URL to "", IMAGE_URL_THUMBNAIL to "")).await()
            true
        }
    }

    private suspend fun deleteIgnoringErrors(path: String) {
        try {
            storage.reference.child(path).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val notFound = (e as? StorageException)?.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND
            if (!notFound) DebugLog.e(TAG) { "Profile photo delete failed: ${e.javaClass.simpleName}" }
        }
    }

    suspend fun getUserMeasuresNoPag(
        userId: String?,
        lastDate: Long?
    ): Response<ArrayList<MeasureData>> {
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
        return gate.safeCall(CallKind.Read) {
            val snapshot = task.get().await()
            DebugLog.d(TAG) { "got measures from FS size: ${snapshot.documents.size}" }
            val measuresRaw: List<RawMeasureData> = snapshot.toObjects(RawMeasureData::class.java)
            DebugLog.d(TAG) { "got measures from FS parsed size: ${measuresRaw.size}" }
            val measures: ArrayList<MeasureData> = ArrayList(measuresRaw.map { MeasureData(it) })
            DebugLog.d(TAG) { "got measures from FS 23 converted to MeasureData size: ${measures.size}" }
            measures
        }
    }

    // Invitations

    suspend fun getNewInvitations(): Int {
        val doc = db.collection(USERS_COLLECTION)
            .whereEqualTo("email", userRepository.email)
            .whereEqualTo("status", InvitationStatus.SENT.rawValue)
            .count()
        return gate.safeCall(CallKind.Read) { doc.get(AggregateSource.SERVER).await().count.toInt() }.dataValue ?: 0
    }

    suspend fun updateToken(): Response<Boolean> {
        val userId = uuid ?: return Response.Failure(Exception("User not logged in"))
        return gate.safeCall(CallKind.Action) {
            // Get new FCM registration token
            val token = FirebaseMessaging.getInstance().token.await()
            DebugLog.d(TAG) { "Updating FCM registration token" }
            db.collection(USERS_COLLECTION).document(userId).update(USERS_TOKEN, token).await()
            true
        }
    }

    suspend fun deleteToken(): Response<Boolean> {
        val userId = uuid ?: return Response.Failure(Exception("User not logged in"))
        return gate.safeCall(CallKind.Action) {
            db.collection(USERS_COLLECTION).document(userId).update(USERS_TOKEN, "").await()
            true
        }
    }

    //suspend fun updateLegal(isAdmin: Boolean): Response<Boolean> {
    suspend fun updateLegal(
        policyAdmin: Boolean,
        policyApp: Boolean,
        consent: Boolean
    ): Response<Boolean> {
        val userId = uuid ?: return Response.Failure(Exception("User not logged in"))
        return gate.safeCall(CallKind.Action) {
            val mutableMap = mutableMapOf<String, Boolean>()
            if (policyAdmin)
                mutableMap["legal.privacyPolicyAdmin"] = true
            if (policyApp)
                mutableMap["legal.privacyPolicyApp"] = true
            if (consent)
                mutableMap["legal.healthConsent"] = true
            val map = mutableMap.toMap()
            db.collection(USERS_COLLECTION).document(userId)
                .update(map).await()
            DebugLog.d(TAG) { "update legal success" }
            true
        }
    }

    suspend fun uploadQrId(qrResponse: QRResponse?): Response<Boolean> {
        val userId = uuid ?: return Response.Failure(Exception("User not logged in"))
        if (qrResponse == null) {
            return Response.Failure(Exception("QR response is null"))
        }
        // An action: with no network it fails at once (Firestore would queue the write and never answer).
        // A write that times out may still be applied later; it is a set() of the same document, so
        // repeating it is harmless.
        return gate.safeCall(CallKind.Action) {
            db.collection(FirebaseConstants.QR_COLLECTION).document(qrResponse.unitId).set(
                hashMapOf(
                    "sessionId" to qrResponse.sessionId,
                    "userId" to userId
                )
            ).await()
            DebugLog.d(TAG) { "qr upload success" }
            true
        }
    }


    companion object {
        val TAG = FirestoreDataSource::class.simpleName
        private const val IMAGE_URL = "imageUrl"
        private const val IMAGE_URL_THUMBNAIL = "imageUrlThumbnail"
        private fun profilePhotoPath(userId: String) = "users/$userId/profile.jpeg"
        private fun profileThumbnailPath(userId: String) = "users/$userId/profile_thumbnail.jpeg"
        private const val IMAGE_UPLOAD_TIMEOUT_MS = 60_000L
    }
}
