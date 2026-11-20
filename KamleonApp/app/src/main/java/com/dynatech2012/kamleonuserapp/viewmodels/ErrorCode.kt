package com.dynatech2012.kamleonuserapp.viewmodels

import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.StorageException

/**
 * Codes shown to the user when an error has no way out except contacting Kamleon, so support can tell
 * the cause without asking the user to describe it. The numbers name the CAUSE and are shared with the
 * other clients (iOS, web): the same number means the same thing everywhere, only the platform prefix
 * differs. Never reuse or change the meaning of a number; retire it instead. Ranges: 1xx session and
 * startup, 2xx login and registration, 3xx data and sync.
 *
 * The shared table lives in docs/runbooks/error-codes.md.
 */
internal enum class ErrorCode(val number: Int) {
    /** The server says the session is not authenticated (expired or revoked). */
    SessionUnauthenticated(101),

    /** The server refuses the signed-in user (rules, App Check, account state). */
    SessionPermissionDenied(102),

    /** The account was disabled by an administrator. */
    AccountDisabled(103),

    /** The account no longer exists, or its credentials were invalidated. */
    AccountNotFound(104),

    /** The server answered but the account could not be loaded, for a reason that is not the network. */
    ServerError(110);

    /** What the user reads out, for example `KL-AND-102`. */
    val label: String get() = "$PLATFORM_PREFIX-%03d".format(number)

    private companion object {
        const val PLATFORM_PREFIX = "KL-AND"
    }
}

/** The code for a failure of the signed-in session or account: anything unrecognized is [ErrorCode.ServerError]. */
internal fun errorCodeOf(e: Exception): ErrorCode = when (e) {
    is FirebaseAuthInvalidUserException ->
        if (e.errorCode == "ERROR_USER_DISABLED") ErrorCode.AccountDisabled else ErrorCode.AccountNotFound
    is FirebaseFirestoreException -> when (e.code) {
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> ErrorCode.SessionUnauthenticated
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> ErrorCode.SessionPermissionDenied
        else -> ErrorCode.ServerError
    }
    is FirebaseFunctionsException ->
        if (e.code == FirebaseFunctionsException.Code.UNAUTHENTICATED) ErrorCode.SessionUnauthenticated
        else ErrorCode.ServerError
    is StorageException ->
        if (e.errorCode == StorageException.ERROR_NOT_AUTHENTICATED) ErrorCode.SessionUnauthenticated
        else ErrorCode.ServerError
    else -> ErrorCode.ServerError
}
