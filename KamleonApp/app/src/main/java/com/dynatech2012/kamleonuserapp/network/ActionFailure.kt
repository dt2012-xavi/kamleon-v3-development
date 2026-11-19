package com.dynatech2012.kamleonuserapp.network

import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.ResponseNullable

/**
 * The outcome of something the user asked for, keeping the failure when there was one so that every
 * screen can say the same thing about it (see [actionFailureText]) instead of only "it failed".
 */
data class ActionResult(val failure: Exception? = null) {
    val success: Boolean get() = failure == null
}

fun actionResultOf(response: Response<*>): ActionResult = ActionResult(response.error)

fun actionResultOf(response: ResponseNullable<*>): ActionResult =
    ActionResult((response as? ResponseNullable.Failure)?.exception)

/** The same words for a connection problem everywhere: the problem is the network, and trying again can work. */
private val NO_CONNECTION = R.string.login_error_title_network to R.string.startup_error_message

/** Title and message for a failed action: the connection one when that is the cause, a generic one otherwise. */
internal fun actionFailureText(failure: Exception?): Pair<Int, Int> =
    if (failure?.isConnectionProblem() == true) NO_CONNECTION
    else R.string.action_failed_title to R.string.action_failed_message

/** Like [actionFailureText] for a scan that could not be uploaded, which keeps its own text for other causes. */
internal fun scanFailureText(failure: Exception?): Pair<Int, Int> =
    if (failure?.isConnectionProblem() == true) NO_CONNECTION
    else R.string.dialog_scan_failed_title to R.string.dialog_scan_failed_message
