package com.ozcanalasalvar.datepicker.compose.datapicker

import android.content.Context
import android.os.Debug
import android.util.AttributeSet
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.ozcanalasalvar.datepicker.view.datapicker.DataChangeListener

class DataPickerComposeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {


    private val offsetState = mutableStateOf(1)

    private val selectorEffectEnabledState = mutableStateOf(true)
    private val textSizeState = mutableStateOf(17)
    private val textBoldState = mutableStateOf(false)
    private val darkModeEnabledState = mutableStateOf(true)
    private val valueUnitState = mutableStateOf("")
    private val selectedValueState = mutableStateOf("")
    private val startValueState = mutableStateOf("")

    private val valuesSourceState = mutableStateOf(arrayListOf("value1", "value2"))
    private val valueWidthState = mutableStateOf(250)
    private val showDecimalState = mutableStateOf(false)

    var offset: Int
        get() = offsetState.value
        set(value) {
            offsetState.value = value
        }
    var selectorEffectEnabled: Boolean
        get() = selectorEffectEnabledState.value
        set(value) {
            selectorEffectEnabledState.value = value
        }

    var textSize: Int
        get() = textSizeState.value
        set(value) {
            textSizeState.value = value
        }

    var textBold: Boolean
        get() = textBoldState.value
        set(value) {
            textBoldState.value = value
        }

    var darkModeEnabled: Boolean
        get() = darkModeEnabledState.value
        set(value) {
            darkModeEnabledState.value = value
        }

    var valueUnit: String
        get() = valueUnitState.value
        set(value) {
            valueUnitState.value = value
        }

    var startValue: String
        get() = selectedValueState.value
        set(value) {
            selectedValueState.value = value
        }

    var values: ArrayList<String>
        get() = valuesSourceState.value
        set(value) {
            valuesSourceState.value = value
        }

    var valueWidth: Int
        get() = valueWidthState.value
        set(value) {
            valueWidthState.value = value
        }

    var showDecimal: Boolean
        get() = showDecimalState.value
        set(value) {
            showDecimalState.value = value
        }

    private var dataChangeListener: DataChangeListener? = null
    fun setDataChangeListener(dataSelectListener: DataChangeListener?) {
        dataChangeListener = dataSelectListener
    }

    @Suppress("RedundantVisibilityModifier")
    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        WheelDataPicker(
            offset = offsetState.value,
            selectorEffectEnabled = selectorEffectEnabledState.value,
            valueUnit = valueUnit,
            startValue = startValue,
            showDecimal = showDecimal,
            values = values,
            textSize = textSizeState.value,
            textBold = textBoldState.value,
            darkModeEnabled = darkModeEnabledState.value,
            onValueChanged = { strVal, strUnit ->
                dataChangeListener?.onDataChanged(strVal, strUnit)
            },
            valueWidth = valueWidth
        )
//        ColorDrawable()
        setBackgroundColor(0xFFFF00)
//        background = ColorDrawable(android.graphics.Color.BLUE)
    }


    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

}