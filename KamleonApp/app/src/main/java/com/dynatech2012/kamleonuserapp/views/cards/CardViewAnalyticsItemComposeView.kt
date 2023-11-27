package com.dynatech2012.kamleonuserapp.views.cards

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.withStyledAttributes
import com.dynatech2012.kamleonuserapp.R

class CardViewAnalyticsItemComposeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    private val valueState = mutableIntStateOf(0)
    var analyticType: AnalyticType = AnalyticType.HYDRATION
    private val subtitleSate = mutableStateOf("")
    private val descriptionState = mutableStateOf("")
    var onClick: () -> Unit = {}

    var value: Int
        get() = valueState.intValue
        set(value) {
            valueState.intValue = value
        }

    var subtitle: String
        get() = subtitleSate.value
        set(value) {
            subtitleSate.value = value
        }
    var description: String
        get() = descriptionState.value
        set(value) {
            descriptionState.value = value
        }

    init {
        // See the footnote
        context.withStyledAttributes(attrs, R.styleable.AnalyticsComposeItemViewStyle) {
            if (hasValue(R.styleable.AnalyticsComposeItemViewStyle_analyticsType)) {
                val value = getInt(R.styleable.AnalyticsComposeItemViewStyle_analyticsType, 0)
                analyticType = AnalyticType.from(value)
            }
        }
    }


    @Suppress("RedundantVisibilityModifier")
    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        CardViewAnalyticsItem(
            analyticType,
            subtitleSate.value,
            descriptionState.value,
            valueState.intValue,
            onClick
        )
    }


    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

}