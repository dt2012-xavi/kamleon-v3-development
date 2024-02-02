package com.dynatech2012.kamleonuserapp.models

import com.dynatech2012.kamleonuserapp.extensions.sha256
import java.security.MessageDigest
import java.util.Date

data class CustomUser(
    val userID: String = "",
    val email: String = "",
    val name: String = "",
    val lastName: String = "",
    val userStatus: UserStatus = UserStatus.undefined,
    var dateOfBirth: Date = Date(),// = LocalDate.MIN,
    var height: Float = -1f,
    var weight: Float = -1f,
    var gender: Gender = Gender.none,

    var address: String = "",
    var centersPermissions: Map<String, KamleonPermissions> = mapOf(),
    var city: String = "",
    var country: String = "",
    var createdAt: Date = Date(),
    var imageUrl: String = "",
    var kamleonPermissions: KamleonPermissions = KamleonPermissions(),
    var organizationsPermissions: Map<String, KamleonPermissions> = mapOf(),
    var phone: String = "",
    var pin: String = "123456".sha256(),
    var rfid: String = "",
    var teamsPermissions: Map<String, KamleonPermissions> = mapOf(),
    var token: String? = null,

    var notifications: Map<String, Boolean> = mapOf()
    )

enum class Gender (val raw: String)
{
    male("Male"),
    female("Female"),
    other("Other"),
    none("Prefer not to say");

    companion object {
        fun fromRaw(raw: String?): Gender {
            return when (raw) {
                "Male" -> male
                "Female" -> female
                "Other" -> other
                "Prefer not to say" -> none
                else -> none
            }
        }
    }
}

enum class UserStatus
{
    active, inactive, undefined
}

data class KamleonPermissions(
    var centerID: String = "",
    var organizationID: String = "",
    var permissions: ArrayList<String> = arrayListOf(),
    var status: String = "",
    var teamID: String = "",
    var userRole: String = "",
    var userType: String = ""
    )
