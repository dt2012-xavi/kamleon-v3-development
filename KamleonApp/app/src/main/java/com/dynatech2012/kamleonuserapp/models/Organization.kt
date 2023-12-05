package com.dynatech2012.kamleonuserapp.models

data class Organization(
    val organizationName: String = "",
    val centerName: String = "",
    val teamName: String = ""
    )
{
    constructor(data: HashMap<String, Any>) : this(
        organizationName = data["organizationName"] as? String ?: "",
        centerName = data["centerName"] as? String ?: "",
        teamName = data["teamName"] as? String ?: ""
    )
}
