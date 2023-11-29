package com.dynatech2012.kamleonuserapp.models

import android.content.Context
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.database.MeasureData

data class Recommendation(
    val kind: String?,
    val titleShort: String,
    val title: String,
    var text: String,
    val isRead: Boolean = false,
    var blocked: Boolean = false,
    val clickable: Boolean = true,
    val showModal: Boolean = true,
    val image: Int? = null
) {

    fun getHydrationText(hydrationLevel: MeasureData.HydrationLevel, context: Context): String {
        val resource: Int = when (hydrationLevel) {
            MeasureData.HydrationLevel.VERYDEHYDRATED -> R.string.recommendation_hydration_1
            MeasureData.HydrationLevel.DEHYDRATED -> R.string.recommendation_hydration_2
            MeasureData.HydrationLevel.HYDRATED -> R.string.recommendation_hydration_3
            MeasureData.HydrationLevel.VERYHYDRATED -> R.string.recommendation_hydration_4
        }
        return context.getString(resource)
    }
    companion object {
        fun fromTipType(recommendationType: RecommendationType, context: Context): List<Recommendation> {
            when (recommendationType) {
                RecommendationType.HOME -> return listOf(
                    Recommendation(
                        kind = context.getString(R.string.recommendation_analytics_h_title),
                        titleShort = context.getString(R.string.recommendation_hydration_title),
                        title = context.getString(R.string.recommendation_analytics_h_title),
                        text = "",
                        isRead = true,
                        clickable = false,
                    ),
                    Recommendation(
                        kind = "Pro Recommendation",
                        titleShort = "Banana booster",
                        title = "Banana booster",
                        text = "Bananas are a good source of essential nutrients, including potassium...",
                        isRead = true,
                        blocked = true,
                    )
                )

                RecommendationType.HYDRATION -> return listOf(
                    Recommendation(
                        kind = context.getString(R.string.recommendation_analytics_h_title),
                        titleShort = context.getString(R.string.recommendation_analytics_h_1_title_short),
                        title = context.getString(R.string.recommendation_analytics_h_1_title),
                        text = context.getString(R.string.recommendation_analytics_h_1_subtitle),
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        showModal = false
                    ),
                    Recommendation(
                        kind = context.getString(R.string.recommendation_analytics_h_title),
                        titleShort = context.getString(R.string.recommendation_analytics_h_2_title_short),
                        title = context.getString(R.string.recommendation_analytics_h_2_title),
                        text = context.getString(R.string.recommendation_analytics_h_2_subtitle),
                        isRead = true,
                        blocked = false,
                        clickable = true
                    ),
                    Recommendation(
                        kind = context.getString(R.string.recommendation_analytics_h_title),
                        titleShort = context.getString(R.string.recommendation_analytics_h_3_title_short),
                        title = context.getString(R.string.recommendation_analytics_h_3_title),
                        text = context.getString(R.string.recommendation_analytics_h_3_subtitle),
                        isRead = true,
                        blocked = false,
                        clickable = true
                    )
                )

                RecommendationType.ELECTROLYTE -> return listOf(
                    Recommendation(
                        kind = context.getString(R.string.recommendation_analytics_e_title),
                        titleShort = context.getString(R.string.recommendation_analytics_e_1_title_short),
                        title = context.getString(R.string.recommendation_analytics_e_1_title),
                        text = context.getString(R.string.recommendation_analytics_e_1_subtitle),
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        showModal = false
                    ),
                    Recommendation(
                        kind = context.getString(R.string.recommendation_analytics_e_title),
                        titleShort = context.getString(R.string.recommendation_analytics_e_2_title_short),
                        title = context.getString(R.string.recommendation_analytics_e_2_title),
                        text = context.getString(R.string.recommendation_analytics_e_2_subtitle),
                        isRead = true,
                        blocked = false,
                        clickable = true
                    ),
                    Recommendation(
                        kind = context.getString(R.string.recommendation_analytics_e_title),
                        titleShort = context.getString(R.string.recommendation_analytics_e_3_title_short),
                        title = context.getString(R.string.recommendation_analytics_e_3_title),
                        text = context.getString(R.string.recommendation_analytics_e_3_subtitle),
                        isRead = true,
                        blocked = false,
                        clickable = true
                    )
                )

                RecommendationType.VOLUME -> return listOf(
                    Recommendation(
                        kind = context.getString(R.string.recommendation_analytics_v_title),
                        titleShort = context.getString(R.string.recommendation_analytics_v_1_title_short),
                        title = context.getString(R.string.recommendation_analytics_v_1_title),
                        text = context.getString(R.string.recommendation_analytics_v_1_subtitle),
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        showModal = false
                    ),
                    Recommendation(
                        kind = context.getString(R.string.recommendation_analytics_v_title),
                        titleShort = context.getString(R.string.recommendation_analytics_v_2_title_short),
                        title = context.getString(R.string.recommendation_analytics_v_2_title),
                        text = context.getString(R.string.recommendation_analytics_v_2_subtitle),
                        isRead = true,
                        blocked = false,
                        clickable = true
                    ),
                    Recommendation(
                        kind = context.getString(R.string.recommendation_analytics_v_title),
                        titleShort = context.getString(R.string.recommendation_analytics_v_3_title_short),
                        title = context.getString(R.string.recommendation_analytics_v_3_title),
                        text = context.getString(R.string.recommendation_analytics_v_3_subtitle),
                        isRead = true,
                        blocked = false,
                        clickable = true
                    )
                )
            }
        }
    }
}

enum class RecommendationType(val title: String) {
    HOME("Home"),
    HYDRATION("Hydration"),
    ELECTROLYTE("Electrolytes"),
    VOLUME("Volume");

    val size: Int
        get() {
            return when (this@RecommendationType) {
                HOME -> 2
                HYDRATION -> 3
                ELECTROLYTE -> 3
                VOLUME -> 3
            }
        }

    companion object {
        fun from(id: Int): RecommendationType {
            return when (id) {
                0 -> HOME
                1 -> HYDRATION
                2 -> ELECTROLYTE
                3 -> VOLUME
                else -> HYDRATION
            }
        }
    }
}
