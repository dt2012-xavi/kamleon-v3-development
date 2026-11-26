package com.dynatech2012.kamleonuserapp.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.constants.Constants.NOTIFICATION_KEY_TITLE
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.PUSH_NOTIFICATION
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_NEW_MEASURE
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives the pushes the backend sends after a new sample: shows "New measure!", flags that new
 * measures are waiting and tells the open app to reload them.
 */
// With firebase_messaging_installation_id_enabled the address is the installation ID, which arrives in
// onRegistered; onNewToken is the old token API and is never called in this mode.
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class NotificationService : FirebaseMessagingService() {

    /**
     * This phone's Firebase installation ID, the address pushes are sent to, is new or changed: saved
     * in the signed-in user's [PushDevices] so they keep arriving. Signed out, it is saved at the next
     * sign-in.
     */
    override fun onRegistered(fid: String) {
        super.onRegistered(fid)
        DebugLog.d(TAG) { "Push installation ID registered" }
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        PushDevices.save(applicationContext, userId, fid)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        DebugLog.d(TAG) { "FCM message received" }
        if (remoteMessage.data[NOTIFICATION_KEY_TITLE] != null) showNewMeasure()
        SharedPrefUtil(applicationContext).saveBoolean(PREF_NEW_MEASURE, true)
        // The open app reloads the measures.
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(PUSH_NOTIFICATION))
    }

    private fun showNewMeasure() {
        createChannel(this)
        // Opens the app as its icon does: back to where it was, or through the startup checks.
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, getString(R.string.channel_id))
            .setSmallIcon(R.drawable.app_icon_foreground)
            .setColor(ContextCompat.getColor(this, R.color.kamleon_blue))
            .setContentTitle(getString(R.string.noti_newmeasure_title))
            .setContentText(getString(R.string.noti_newmeasure_text))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        // One at a time: a newer sample replaces the previous notification.
        getSystemService(NotificationManager::class.java).notify(NEW_MEASURE_ID, notification)
    }

    companion object {
        private val TAG = NotificationService::class.java.simpleName
        private const val NEW_MEASURE_ID = 101

        /**
         * The channel the new-score notifications use, also the one Firebase shows its own
         * notifications on. Its name and description are what the phone's settings show.
         */
        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                context.getString(R.string.channel_id),
                context.getString(R.string.noti_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.noti_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
