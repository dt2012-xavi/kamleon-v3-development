package com.dynatech2012.kamleonuserapp.utils

import android.content.Context
import androidx.core.content.edit
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_USER_ASKED_FOR_PERMISSION

// "Shared Prefs" is the file MainActivity has always used for this flag.
private const val PERMISSION_PREFS = "Shared Prefs"

/**
 * Whether the notification permission has already been requested on this device. The onboarding
 * asks first (with an explanation); MainActivity only asks if nobody has, so a user who said no
 * is not prompted again right after logging in.
 */
var Context.notificationPermissionAsked: Boolean
    get() = getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_USER_ASKED_FOR_PERMISSION, false)
    set(value) {
        getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(PREF_USER_ASKED_FOR_PERMISSION, value) }
    }
