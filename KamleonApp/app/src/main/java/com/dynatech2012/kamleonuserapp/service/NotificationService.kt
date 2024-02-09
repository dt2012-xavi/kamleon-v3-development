package com.dynatech2012.kamleonuserapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.activities.MainActivity
import com.dynatech2012.kamleonuserapp.constants.Constants.NOTIFICATION_KEY_TITLE
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.PUSH_NOTIFICATION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USERS_COLLECTION
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USERS_TOKEN
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_USER_ID
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class NotificationService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "token app refreshed: $token")
        val sharedPrefUtil = SharedPrefUtil(applicationContext)
        val userId: String? = sharedPrefUtil.getString(PREF_USER_ID, null)
        if (userId != null) {
            val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
            firestore.collection(USERS_COLLECTION).document(userId).update(USERS_TOKEN, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Notification Received: " + remoteMessage.data)
        createNotificationChannel(applicationContext, applicationContext.getString(
            R.string.channel_id
        ), "Analytics Notifications")
        createNewMeasureNotification(applicationContext, remoteMessage)
    }

    /*
     * private static int createID() {
     *     Date now = new Date();
     *     return Integer.parseInt(new SimpleDateFormat("ddHHmmss", Locale.US).format(now));
     * }
     */
    private fun createNewMeasureNotification(context: Context, remoteMessage: RemoteMessage) {
        val data: Map<String, String> = remoteMessage.data
        val title = data[NOTIFICATION_KEY_TITLE]
        var notifTitle = "Title"
        var notifMessage = "Message"
        var notifId = 0
        if (title != null) {
            //if (title == context.getString(R.string.notification_newmeasure_incoming_title)) {
                notifTitle = context.getString(R.string.noti_newmeasure_title)
                notifMessage = context.getString(R.string.noti_newmeasure_text)
                notifId = 101
            //}
        }
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL)
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(
            context, applicationContext.getString(
                R.string.channel_id
            )
        )
            .setLargeIcon(
                BitmapFactory.decodeResource(resources, R.drawable.app_icon_foreground)
            )
            .setSmallIcon(R.drawable.app_icon_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.app_icon_foreground))
            .setColor(ContextCompat.getColor(context, R.color.kmln_graph_header_item_text_active))
            .setContentTitle(notifTitle)
            .setContentText(notifMessage)
            .setWhen(System.currentTimeMillis())
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(defaultSoundUri)
            .setLights(Color.BLUE, 500, 500)
            .setVibrate(longArrayOf(0, 500, 200, 500)) //.setGroup(NOTIFICATION_GROUP)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //notificationManager.notify(createID(), builder.build());
        notificationManager.notify(notifId, builder.build())

        //App opened and in HomeFragment (broadcast the message so it can be received from the HomeFragment)
        val pushNotification = Intent(PUSH_NOTIFICATION)

        // Send data to the app if it is in foreground
        Log.d(TAG, "onGetNotification from Notification Service")
        LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification)
        //Notification.instance?.addOrderBoolean(true)
    }

    fun createNotificationChannel(context: Context, channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Analytics"
            }
            val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /*
    class Notification private constructor() {
        private val newOrderString: MutableLiveData<String> = MutableLiveData()
        private val newOrderBoolean: MutableLiveData<Boolean> = MutableLiveData()
        fun getNewOrder(): LiveData<String> {
            return newOrderString
        }

        fun getNewOrderBoolean(): LiveData<Boolean> {
            return newOrderBoolean
        }
        fun addOrderString(orderID: String) {
            newOrderString.postValue(orderID)
        }
        fun addOrderBoolean(received: Boolean) {
            newOrderBoolean.postValue(received)
        }

        fun reset() {
            newOrderString.postValue("")
            newOrderBoolean.postValue(false)
        }
        companion object {
            var instance: Notification? = null
                get() {
                    if (field == null) {
                        field = Notification()
                    }
                    return field
                }
                private set
        }
    }
     */

    companion object {
        private val TAG = NotificationService::class.java.simpleName
        fun getToken(context: Context): String? {
            return context.getSharedPreferences("_", MODE_PRIVATE).getString("fb", "empty")
        }
    }
}