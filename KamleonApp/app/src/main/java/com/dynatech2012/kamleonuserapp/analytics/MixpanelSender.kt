package com.dynatech2012.kamleonuserapp.analytics

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONArray
import org.json.JSONObject

/** Where the events go. Mixpanel in the builds that have a token; nothing otherwise. */
interface AnalyticsSender {
    val distinctId: String?
    fun register(props: Map<String, Any>)
    fun track(name: String, props: Map<String, Any>)
    fun identify(userId: String)
    fun setProfile(props: Map<String, Any>)
    fun reset()
}

/**
 * Mixpanel with the web's privacy defaults: the EU servers, no IP, and none of its automatic events,
 * so only the app's named events are sent.
 */
class MixpanelSender(context: Context, token: String) : AnalyticsSender {
    private val mixpanel: MixpanelAPI = MixpanelAPI.getInstance(context, token, false).apply {
        setServerURL(EU_API_HOST)
        setUseIpAddressForGeolocation(false)
    }

    override val distinctId: String? get() = mixpanel.distinctId

    override fun register(props: Map<String, Any>) = mixpanel.registerSuperProperties(json(props))

    override fun track(name: String, props: Map<String, Any>) = mixpanel.track(name, json(props))

    override fun identify(userId: String) = mixpanel.identify(userId)

    override fun setProfile(props: Map<String, Any>) = mixpanel.people.set(json(props))

    override fun reset() = mixpanel.reset()

    private fun json(props: Map<String, Any>) = JSONObject().apply {
        props.forEach { (key, value) ->
            put(
                key,
                when (value) {
                    is List<*> -> JSONArray(value)
                    AnalyticsProps.NULL -> JSONObject.NULL
                    else -> value
                }
            )
        }
    }

    companion object {
        const val EU_API_HOST = "https://api-eu.mixpanel.com"
    }
}
