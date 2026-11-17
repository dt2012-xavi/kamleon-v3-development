package com.dynatech2012.kamleonuserapp.repositories

import android.content.Context
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What belongs to the signed-in user on this device, apart from the sign-in itself: the messaging
 * token in their profile, the local measurements and the saved preferences. Used by the logout in
 * Settings and when the session cannot be used (the server refuses the profile), so that whoever
 * signs in next on this phone does not inherit it.
 */
class SessionCleaner @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val firestoreRepo: FirestoreDataSource,
    private val databaseRepository: DatabaseDataSource
) {
    suspend fun clearLocalData() {
        // Firestore only confirms a write once the server has it: with no network, or with a refused
        // session, it never would, and the logout would hang. It is best effort.
        withTimeoutOrNull(REMOTE_STEP_TIMEOUT_MS) { firestoreRepo.deleteToken() }
        databaseRepository.deleteAllMeasures()
        SharedPrefUtil(appContext).removeUser()
    }

    private companion object {
        const val REMOTE_STEP_TIMEOUT_MS = 3_000L
    }
}
