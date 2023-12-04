package com.dynatech2012.kamleonuserapp.models

import android.util.Log
import com.dynatech2012.kamleonuserapp.extensions.isLastWeek
import com.dynatech2012.kamleonuserapp.extensions.isToday
import com.dynatech2012.kamleonuserapp.extensions.isYesterday
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class InvitationStatus(val rawValue: String) {
    ACCEPTED(rawValue = "accepted"),
    REJECTED(rawValue = "rejected"),
    SENT(rawValue = "sent");

    companion object {
        operator fun invoke(rawValue: String): InvitationStatus? = values().firstOrNull { it.rawValue == rawValue }
    }
}

enum class InvitationRole(val rawValue: String) {
    KAMLEON_OWNER(rawValue = "kamleon_owner"),
    KAMLEON_ADMIN(rawValue = "kamleon_admin"),
    KAMLEON_EDITOR(rawValue = "kamleon_editor"),
    KAMLEON_VIEWER(rawValue = "kamleon_viewer"),
    ORGSTAFF_ADMIN(rawValue = "orgstaff_admin"),
    ORGSTAFF_OWNER(rawValue = "orgstaff_owner"),
    CENTERSTAFF_OWNER(rawValue = "centerstaff_owner"),
    CENTERSTAFF_ADMIN(rawValue = "centerstaff_admin"),
    CENTERSTAFF_ADMIN_RESTRICTED(rawValue = "centerstaff_admin_restricted"),
    TEAMSTAFF_PRO(rawValue = "teamstaff_pro"),
    TEAMSTAFF_USER(rawValue = "teamstaff_user");
    companion object {
        operator fun invoke(rawValue: String): InvitationRole? = InvitationRole.values().firstOrNull { it.rawValue == rawValue }
    }
}

data class Invitation (
    var id: String,
    var organizationName: String,
    var centerName: String,
    var teamName: String,
    var dateRejected: Date?,
    var dateAccepted: Date?,
    var dateSent: Date,
    var status: InvitationStatus,
    var role: InvitationRole,
) {
    constructor(data: HashMap<String, Any>) : this(
        id = data["id"] as String,
        organizationName = data["organizationName"] as String,
        centerName = data["centerName"] as String,
        teamName = data["teamName"] as String,
        dateRejected = if ((data["dateRejected"] as? HashMap<String, Int> != null))
            Date((((data["dateRejected"] as HashMap<String, Int>)["_seconds"]) as Int) * 1000L)
        else null,//Date(),
        dateAccepted = if ((data["dateAccepted"] as? HashMap<String, Int> != null))
            Date((((data["dateAccepted"] as HashMap<String, Int>)["_seconds"]) as Int) * 1000L)
        else null,//Date(),
        dateSent = if ((data["dateSent"] as? HashMap<String, Int> != null))
            Date((((data["dateSent"] as HashMap<String, Int>)["_seconds"]) as Int) * 1000L)
        else Date(),
        status = InvitationStatus(data["status"] as String) ?: InvitationStatus.SENT,
        role = InvitationRole(data["role"] as String) ?: InvitationRole.KAMLEON_OWNER
    )

    val invitationText: String
        get() {
            when (role) {
                InvitationRole.KAMLEON_OWNER -> {
                    return "Kamleon has invited you to join Kamleon as Kamleon owner"
                }

                InvitationRole.KAMLEON_ADMIN -> {
                    return "Kamleon has invited you to join Kamleon as Kamleon admin"
                }

                InvitationRole.KAMLEON_EDITOR -> {
                    return "Kamleon has invited you to join Kamleon as Kamleon editor"
                }

                InvitationRole.KAMLEON_VIEWER -> {
                    return "Kamleon has invited you to join Kamleon as Kamleon viewer"
                }

                InvitationRole.ORGSTAFF_ADMIN, InvitationRole.ORGSTAFF_OWNER -> {
                    return "Kamleon has invited you as an administrator of $organizationName"
                }

                InvitationRole.CENTERSTAFF_OWNER -> {
                    return "$organizationName has invited you to $centerName"
                }

                InvitationRole.CENTERSTAFF_ADMIN_RESTRICTED, InvitationRole.CENTERSTAFF_ADMIN -> {
                    return "$organizationName has invited you to $centerName"
                }

                InvitationRole.TEAMSTAFF_PRO, InvitationRole.TEAMSTAFF_USER -> {
                    return "$organizationName has invited you to $teamName team"
                }
            }
        }
    val invitationTime: String
        get() {
            val date = dateSent
            // If it's today, display the time (e.g., "19:00").
            if (date.isToday) {
                val dateFormater = SimpleDateFormat("HH:mm", Locale.getDefault())
                return dateFormater.format(date)
            }
            // If it's yesterday, display "Yesterday".
            else if (date.isYesterday) {
                val dateFormater = SimpleDateFormat("HH:mm", Locale.getDefault())
                val stringDate = dateFormater.format(date)
                return "Yesterday $stringDate"
            }
            // If it's within the last week, display the day name (e.g., "Tuesday").
            else if (date.isLastWeek) {
                Log.d(TAG, "invitationTime: isLastWeek: $date")
                val dateFormater = SimpleDateFormat("EEEE HH:mm", Locale.getDefault())
                return dateFormater.format(date)
            }
            // If it's more than one week ago, display the full date.
            else {
                Log.d(TAG, "invitationTime: is old: $date")
                val dateFormater = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                return dateFormater.format(date)
            }

        }

    override fun equals(other: Any?): Boolean {
        return if (other is Invitation) {
            other.id == id &&
                    other.organizationName == organizationName &&
                    other.centerName == centerName &&
                    other.teamName == teamName &&
                    other.status == status &&
                    other.role == role
        } else false
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

    companion object {
        val TAG: String = Invitation::class.java.simpleName
    }
}