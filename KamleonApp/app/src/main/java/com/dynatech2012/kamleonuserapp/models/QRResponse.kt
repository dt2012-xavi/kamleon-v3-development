package com.dynatech2012.kamleonuserapp.models

data class QRResponse(
    val unitId: String = "",
    val sessionId: String = "",
    val data: HashMap<String, String> = hashMapOf()
    )

