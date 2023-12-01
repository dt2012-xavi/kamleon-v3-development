package com.dynatech2012.kamleonuserapp.constants

/**
 *
 * <font color="teal">
 * Edu:<br></br>
 * Constant class. It is final to prevent be extended <br></br>
 * It has a private constructor to prevent being instantiated
</font> */
object FirebaseConstants {
    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * It is an user's uid with many values already!
    </font> */

    // Firestore
    const val USERS_COLLECTION = "users"
    const val MEASURES_COLLECTION = "measures"
    const val INVITATIONS_COLLECTION = "invitations"
    const val kAVERAGES = "Averages"
    const val kUSERID_FIELD = "userID"
    const val kANALYSIS_DATE = "analysisDate"

    // Realtime
    const val USER_UID_DEBUG = "AuBdJ7CpSNdm2nDyOSg0GroK8R83"
    const val REALTIME_COLLECTION_AVERAGES = "Averages"
    const val REALTIME_COLLECTION_QR_LOGIN = "DEVICE_QR_LOGIN"
    const val REALTIME_FOLDER_MONTHLY = "monthly"
    const val REALTIME_FOLDER_DAILY = "daily"

    // Notifications
    const val PUSH_NOTIFICATION: String = "pushNotification"
    const val USERS_TOKEN = "token"

}