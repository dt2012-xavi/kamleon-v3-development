package com.dynatech2012.kamleonuserapp.application

import android.app.Application
import android.os.Build
import com.dynatech2012.kamleonuserapp.R
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("package_name", packageName)
            setCustomKey("version_code", versionCode)
            setCustomKey("version_name", packageInfo.versionName ?: getString(R.string.app_version))
            setCustomKey("app_version", getString(R.string.app_version))
        }
    }
}
