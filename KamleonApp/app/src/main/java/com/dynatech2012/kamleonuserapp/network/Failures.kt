package com.dynatech2012.kamleonuserapp.network

import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import java.io.IOException
import java.util.concurrent.TimeoutException

/** The monitor said there is no network, so the call was not even tried. */
class NetworkUnavailableException : IOException("No network connection")

/** The call did not answer in time. A write may still be applied later (see NetworkGate). */
class NetworkTimeoutException(timeoutMs: Long) : IOException("Timed out after $timeoutMs ms")

/** What a failure means for the user, the one definition every screen shares. */
enum class FailureKind {
    /** No connection or no answer in time: trying again later can work. */
    Network,

    /** The server answered that it is busy: trying again later can work. */
    Busy,

    /** The server refuses the signed-in user: only a new login can fix it. */
    SessionRejected,

    /** The thing asked for does not exist (a user without profile). */
    NotFound,

    /** The server answered with a failure that is not about the network or the session. */
    Server,

    Unknown
}

/**
 * Classifies [e]. Plain network failures are decided here; Firebase's own exceptions by [firebaseKind],
 * which tests replace because those exceptions need real Android classes to be built.
 */
internal fun failureKindOf(
    e: Exception,
    firebaseKind: (Exception) -> FailureKind? = ::firebaseFailureKind
): FailureKind = when (e) {
    is NetworkUnavailableException, is NetworkTimeoutException, is TimeoutException, is IOException ->
        FailureKind.Network
    is UserProfileMissingException -> FailureKind.NotFound
    else -> firebaseKind(e) ?: FailureKind.Unknown
}

/**
 * A failure about the connection or a busy server, where "check your connection and try again" is the
 * right thing to say and trying again later can work. The one criterion every screen shares.
 */
internal fun Exception.isConnectionProblem(): Boolean =
    failureKindOf(this).let { it == FailureKind.Network || it == FailureKind.Busy }

/**
 * The kind of a Firebase exception, or null when it is not one. Firestore's codes are matched inside
 * the function, not in a constant, because its enum does not load on the JVM.
 */
internal fun firebaseFailureKind(e: Exception): FailureKind? = when (e) {
    is FirebaseNetworkException -> FailureKind.Network
    is FirebaseAuthInvalidUserException -> FailureKind.SessionRejected
    is FirebaseFirestoreException -> when (e.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> FailureKind.Network
        FirebaseFirestoreException.Code.ABORTED,
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
        FirebaseFirestoreException.Code.CANCELLED -> FailureKind.Busy
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> FailureKind.SessionRejected
        else -> FailureKind.Server
    }
    is StorageException -> when (e.errorCode) {
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> FailureKind.Network
        StorageException.ERROR_NOT_AUTHENTICATED -> FailureKind.SessionRejected
        StorageException.ERROR_OBJECT_NOT_FOUND -> FailureKind.NotFound
        else -> FailureKind.Server
    }
    else -> null
}
