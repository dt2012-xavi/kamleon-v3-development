package com.dynatech2012.kamleonuserapp.utils

import timber.log.Timber
import com.dynatech2012.kamleonuserapp.BuildConfig

object DebugLog {
    fun isEnabled(debug: Boolean = BuildConfig.DEBUG): Boolean = debug

    fun d(tag: String?, message: () -> String) {
        if (isEnabled()) Timber.tag(tag ?: "DebugLog").d(message())
    }

    fun i(tag: String?, message: () -> String) {
        if (isEnabled()) Timber.tag(tag ?: "DebugLog").i(message())
    }

    fun w(tag: String?, message: () -> String) {
        if (isEnabled()) Timber.tag(tag ?: "DebugLog").w(message())
    }

    fun e(tag: String?, message: () -> String) {
        if (isEnabled()) Timber.tag(tag ?: "DebugLog").e(message())
    }

    fun e(tag: String?, throwable: Throwable, message: () -> String) {
        if (isEnabled()) Timber.tag(tag ?: "DebugLog").e(throwable, message())
    }
}
