package com.dynatech2012.kamleonuserapp.constants

object Constants {

    // DEBUG
    const val DEBUG_MODE = false

    // Errors
    const val UNMATCHING_PIN = "Unmatching PIN"

    // Results
    const val PICK_IMAGE = "pickImage"
    const val PICK_IMAGE_BUNDLE = "pickImageBundle"

    const val TAKE_IMAGE = "takeImage"
    const val TAKE_IMAGE_BUNDLE = "takeImageBundle"

    //const val GRANT_LOCATION = "location"
    const val GRANT_LOCATION_BUNDLE = "locationBundle"

    const val GRANT_NOTIFICATION = "notification"
    const val GRANT_NOTIFICATION_BUNDLE = "notificationBundle"

    // Notifications
    const val NOTIFICATION_KEY_TITLE = "title"

    // Scan links: query parameters of https://<host>/scan, reused as MainActivity extras.
    const val SCAN_ACTION = "scan"
    const val SCAN_UNIT_ID = "unitId"
    const val SCAN_SESSION_ID = "sessionId"
    /** MainActivity extra: a scan link arrived but cannot be used (incomplete, or it waited too long). */
    const val SCAN_INVALID = "scanInvalid"
}