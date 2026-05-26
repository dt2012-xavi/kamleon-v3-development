package com.dynatech2012.kamleonuserapp.utils

import android.util.Log
import com.dynatech2012.kamleonuserapp.BuildConfig

object DebugLog {
    fun isEnabled(debug: Boolean = BuildConfig.DEBUG): Boolean = debug

    fun d(tag: String?, message: () -> String) {
        if (isEnabled()) Log.d(tag, message())
    }

    fun i(tag: String?, message: () -> String) {
        if (isEnabled()) Log.i(tag, message())
    }

    fun e(tag: String?, message: () -> String) {
        if (isEnabled()) Log.e(tag, message())
    }

    fun e(tag: String?, throwable: Throwable, message: () -> String) {
        if (isEnabled()) Log.e(tag, message(), throwable)
    }
}
