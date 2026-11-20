package com.dynatech2012.kamleonuserapp.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dynatech2012.kamleonuserapp.viewmodels.ErrorCode
import com.dynatech2012.kamleonuserapp.viewmodels.errorCodeOf

/**
 * Where the app learns that the server ended the signed-in session while the user was using it (see
 * [endsSession]). The network gate reports every such failure; many calls can fail at once, so only
 * the first is kept until [reset]. [ended] holds the code to show, or null while the session is fine;
 * it is a state, not an event, so a screen that opens later still learns about it.
 *
 * [reset] belongs to whoever decides about the session next: the sign-out after the user was told, and
 * the startup check, which is the authority on a session at launch.
 */
class SessionWatcher {
    private val _ended = MutableLiveData<ErrorCode?>(null)
    internal val ended: LiveData<ErrorCode?> = _ended

    // Reports come from background threads: the guard cannot be the LiveData, which updates later.
    @Volatile private var reported = false

    @Synchronized
    fun report(failure: Exception) {
        if (reported) return
        reported = true
        _ended.postValue(errorCodeOf(failure))
    }

    @Synchronized
    fun reset() {
        reported = false
        _ended.postValue(null)
    }
}
