package com.dynatech2012.kamleonuserapp.models

data class QRResponse(
    val unitId: String = "",
    val sessionId: String = "",
    val data: HashMap<String, String> = hashMapOf()
    )
{
    constructor(data: HashMap<String, Any>) : this(
        unitId = data["organizationName"] as? String ?: "",
        sessionId = data["centerName"] as? String ?: "",
        data = data as HashMap<String, String>
    )
}
