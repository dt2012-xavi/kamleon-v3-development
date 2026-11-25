package com.dynatech2012.kamleonuserapp.repositories

import android.content.Context
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_REALTIMEDAILY
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_REALTIMEMONTHLY
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil

object InternalStorage {
    //Firestore

    //Realtime Daily
    fun writeLastRealtimeDailyDateOnInternalStorage(mcoContext: Context, sBody: String?) {
        SharedPrefUtil(mcoContext).saveString(PREF_REALTIMEDAILY, sBody)
    }

    fun readLastRealtimeDailyDateOnInternalStorage(mcoContext: Context): String? {
        return SharedPrefUtil(mcoContext).getString(PREF_REALTIMEDAILY, null)
    }

    //Realtime Monthly
    fun writeLastRealtimeMonthlyDateOnInternalStorage(mcoContext: Context, sBody: String?) {
        SharedPrefUtil(mcoContext).saveString(PREF_REALTIMEMONTHLY, sBody)
    }

    fun readLastRealtimeMonthlyDateOnInternalStorage(mcoContext: Context): String? {
        return SharedPrefUtil(mcoContext).getString(PREF_REALTIMEMONTHLY, null)
    } 
}