package com.dynatech2012.kamleonuserapp.network

import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.ResponseNullable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * What a remote call is for, which decides how it may be retried.
 * - [Read]: a read; whoever shows its result can retry it when the network returns.
 * - [Action]: anything the user triggers or that changes state; it is never retried on its own.
 */
enum class CallKind(val defaultTimeoutMs: Long) {
    Read(10_000L),
    Action(15_000L)
}

/**
 * The single place where remote repository calls turn into a [Response]: the network monitor and a
 * time limit are applied, and exceptions come back as failures whose [FailureKind] is read later with
 * [failureKindOf].
 *
 * A write to Firestore with no network is queued by the SDK and never completes nor fails, so the
 * monitor is checked first and there is a time limit; a write that times out may still be applied
 * later, which is why the writes behind [CallKind.Action] must be idempotent.
 */
class NetworkGate(val monitor: NetworkMonitor) {

    /**
     * Runs [block] as a [kind] call. With [trustMonitor] and the monitor offline it fails at once without
     * calling; pass `false` for a retry the user asked for, so a wrong "offline" never stops them.
     */
    suspend fun <T : Any> safeCall(
        kind: CallKind,
        trustMonitor: Boolean = true,
        timeoutMs: Long = kind.defaultTimeoutMs,
        block: suspend () -> T
    ): Response<T> = attempt(kind, trustMonitor, timeoutMs, block).fold(
        onSuccess = { Response.Success(it) },
        onFailure = { Response.Failure(it as Exception) }
    )

    /** [safeCall] for calls that return nothing, as a [ResponseNullable]. */
    suspend fun safeCallNullable(
        kind: CallKind,
        trustMonitor: Boolean = true,
        timeoutMs: Long = kind.defaultTimeoutMs,
        block: suspend () -> Unit
    ): ResponseNullable<Nothing> = attempt(kind, trustMonitor, timeoutMs, block).fold(
        onSuccess = { ResponseNullable.Success() },
        onFailure = { ResponseNullable.Failure(it as Exception) }
    )

    private suspend fun <T : Any> attempt(
        kind: CallKind,
        trustMonitor: Boolean,
        timeoutMs: Long,
        block: suspend () -> T
    ): Result<T> {
        if (trustMonitor && !monitor.isOnline) {
            Timber.w("%s call skipped: no network", kind)
            return Result.failure(NetworkUnavailableException())
        }
        return try {
            val value = withTimeoutOrNull(timeoutMs) { block() } ?: throw NetworkTimeoutException(timeoutMs)
            Result.success(value)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Only the kind of call and the class of the failure: its message may carry user data.
            Timber.w("%s call failed: %s", kind, e.javaClass.simpleName)
            Result.failure(e)
        }
    }
}
