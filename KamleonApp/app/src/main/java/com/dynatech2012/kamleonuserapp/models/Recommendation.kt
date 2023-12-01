package com.dynatech2012.kamleonuserapp.models

import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.database.MeasureData

data class Recommendation(
    val kind: Int?,
    val titleShort: Int,
    val title: Int,
    var text: Int?,
    val isRead: Boolean = false,
    var blocked: Boolean = false,
    val clickable: Boolean = true,
    val showModal: Boolean = true,
    val image: Int? = null
) {

    fun getHydrationText(hydrationLevel: MeasureData.HydrationLevel): Int {
        val resource: Int = when (hydrationLevel) {
            MeasureData.HydrationLevel.VERYDEHYDRATED -> R.string.recommendation_hydration_1
            MeasureData.HydrationLevel.DEHYDRATED -> R.string.recommendation_hydration_2
            MeasureData.HydrationLevel.HYDRATED -> R.string.recommendation_hydration_3
            MeasureData.HydrationLevel.VERYHYDRATED -> R.string.recommendation_hydration_4
        }
        return resource
    }
    companion object {
        fun fromTipType(recommendationType: RecommendationType): List<Recommendation> {
            when (recommendationType) {
                RecommendationType.HOME -> return listOf(
                    Recommendation(
                        kind = R.string.recommendation_analytics_h_title,
                        titleShort = R.string.recommendation_hydration_title,
                        title = R.string.recommendation_analytics_h_title,
                        text = null,
                        isRead = true,
                        clickable = false,
                    ),
                    Recommendation(
                        kind = R.string.recomendation_home_kind,
                        titleShort = R.string.recomendation_home_title,
                        title = R.string.recomendation_home_title,
                        text = R.string.recomendation_home_subtitle,//"Bananas are a good source of essential nutrients, including potassium...",
                        isRead = true,
                        blocked = true,
                        image = R.drawable.recom4
                    )
                )

                RecommendationType.HYDRATION -> return listOf(
                    Recommendation(
                        kind = R.string.recommendation_analytics_h_title,
                        titleShort = R.string.recommendation_analytics_h_1_title_short,
                        title = R.string.recommendation_analytics_h_1_title,
                        text = R.string.recommendation_analytics_h_1_subtitle,
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        showModal = false,
                        image = R.drawable.recom1
                    ),
                    Recommendation(
                        kind = R.string.recommendation_analytics_h_title,
                        titleShort = R.string.recommendation_analytics_h_2_title_short,
                        title = R.string.recommendation_analytics_h_2_title,
                        text = R.string.recommendation_analytics_h_2_subtitle,
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        image = R.drawable.recom2
                    ),
                    Recommendation(
                        kind = R.string.recommendation_analytics_h_title,
                        titleShort = R.string.recommendation_analytics_h_3_title_short,
                        title = R.string.recommendation_analytics_h_3_title,
                        text = R.string.recommendation_analytics_h_3_subtitle,
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        image = R.drawable.recom8
                    )
                )

                RecommendationType.ELECTROLYTE -> return listOf(
                    Recommendation(
                        kind = R.string.recommendation_analytics_e_title,
                        titleShort = R.string.recommendation_analytics_e_1_title_short,
                        title = R.string.recommendation_analytics_e_1_title,
                        text = R.string.recommendation_analytics_e_1_subtitle,
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        showModal = false,
                        image = R.drawable.recom7
                    ),
                    Recommendation(
                        kind = R.string.recommendation_analytics_e_title,
                        titleShort = R.string.recommendation_analytics_e_2_title_short,
                        title = R.string.recommendation_analytics_e_2_title,
                        text = R.string.recommendation_analytics_e_2_subtitle,
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        image = R.drawable.recom3
                    ),
                    Recommendation(
                        kind = R.string.recommendation_analytics_e_title,
                        titleShort = R.string.recommendation_analytics_e_3_title_short,
                        title = R.string.recommendation_analytics_e_3_title,
                        text = R.string.recommendation_analytics_e_3_subtitle,
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        image = R.drawable.recom9
                    )
                )

                RecommendationType.VOLUME -> return listOf(
                    Recommendation(
                        kind = R.string.recommendation_analytics_v_title,
                        titleShort = R.string.recommendation_analytics_v_1_title_short,
                        title = R.string.recommendation_analytics_v_1_title,
                        text = R.string.recommendation_analytics_v_1_subtitle,
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        showModal = false,
                        image = R.drawable.recom5
                    ),
                    Recommendation(
                        kind = R.string.recommendation_analytics_v_title,
                        titleShort = R.string.recommendation_analytics_v_2_title_short,
                        title = R.string.recommendation_analytics_v_2_title,
                        text = R.string.recommendation_analytics_v_2_subtitle,
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        image = R.drawable.recom10
                    ),
                    Recommendation(
                        kind = R.string.recommendation_analytics_v_title,
                        titleShort = R.string.recommendation_analytics_v_3_title_short,
                        title = R.string.recommendation_analytics_v_3_title,
                        text = R.string.recommendation_analytics_v_3_subtitle,
                        isRead = true,
                        blocked = false,
                        clickable = true,
                        image = R.drawable.recom6
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
