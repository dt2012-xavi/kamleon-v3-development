package com.dynatech2012.kamleonuserapp.service

import android.content.Context
import com.dynatech2012.kamleonuserapp.BuildConfig
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USERS_COLLECTION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USERS_TOKEN
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_PUSH_FID
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Where a user's pushes go: one entry per device in `users/{uid}.pushDevices`, keyed by the device's
 * Firebase installation ID (FID), so a user signed in on several devices gets them on all:
 *
 * ```
 * pushDevices: { "<fid>": { platform: "android", appVersion: "1.0.15 (2028)", updatedAt: <time> } }
 * ```
 *
 * The backend sends to every key and removes the ones FCM reports as no longer registered;
 * `updatedAt` lets it also drop devices not seen for a long time.
 */
object PushDevices {
    const val FIELD = "pushDevices"

    /** What is saved about this device. */
    fun entry(): Map<String, Any> = mapOf(
        "platform" to "android",
        "appVersion" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        "updatedAt" to FieldValue.serverTimestamp()
    )

    /**
     * Saves this device, with installation ID [fid], on user [userId]. If the phone had another ID
     * before, that entry is removed in the same write.
     */
    fun save(context: Context, userId: String, fid: String): Task<Void> {
        val prefs = SharedPrefUtil(context)
        val previous = prefs.getString(PREF_PUSH_FID, null)?.takeIf { it.isNotBlank() && it != fid }
        val user = FirebaseFirestore.getInstance().collection(USERS_COLLECTION).document(userId)
        val task = if (previous != null) {
            user.update(FieldPath.of(FIELD, fid), entry(), FieldPath.of(FIELD, previous), FieldValue.delete())
        } else {
            user.update(FieldPath.of(FIELD, fid), entry())
        }
        return task.addOnSuccessListener { prefs.saveString(PREF_PUSH_FID, fid) }
    }

    /**
     * Logging out: removes only this device, so the user's other devices keep their pushes. The old
     * single `token` of earlier app versions is cleared too.
     */
    fun remove(userId: String, fid: String): Task<Void> =
        FirebaseFirestore.getInstance().collection(USERS_COLLECTION).document(userId)
            .update(FieldPath.of(FIELD, fid), FieldValue.delete(), FieldPath.of(USERS_TOKEN), "")
}
