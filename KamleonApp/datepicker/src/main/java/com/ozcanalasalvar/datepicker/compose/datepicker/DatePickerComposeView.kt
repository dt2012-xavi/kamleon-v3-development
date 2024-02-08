package com.ozcanalasalvar.datepicker.compose.datepicker


import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.ozcanalasalvar.datepicker.model.Date
import com.ozcanalasalvar.datepicker.utils.DateUtils
import com.ozcanalasalvar.datepicker.view.datepicker.DateChangeListener
import com.ozcanalasalvar.datepicker.view.datepicker.DatePicker
import com.ozcanalasalvar.datepicker.view.datepicker.OnDatePickerTouchListener

class DatePickerComposeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {


    private val offsetState = mutableStateOf(4)
    private val yearsRangeState = mutableStateOf(IntRange(1923, 2121))
    private val startDateState = mutableStateOf(Date(DateUtils.getCurrentTime()))
    private val toDateState = mutableStateOf(Date(DateUtils.getCurrentTime()))
    private val maxDateState = mutableStateOf(Date(DateUtils.getCurrentTime()))
    private val selectorEffectEnabledState = mutableStateOf(true)
    private val textSizeState = mutableStateOf(17)
    private val textBoldState = mutableStateOf(false)
    private val darkModeEnabledState = mutableStateOf(true)

    var offset: Int
        get() = offsetState.value
        set(value) {
            offsetState.value = value
        }

    var yearsRange: IntRange
        get() = yearsRangeState.value
        set(value) {
            yearsRangeState.value = value
        }

    var startDate: Date
        get() = startDateState.value
        set(value) {
            startDateState.value = value
        }

    var maxDate: Date
        get() = maxDateState.value
        set(value) {
            maxDateState.value = value
        }
    var toDate: Date
        get() = toDateState.value
        set(value) {
            toDateState.value = value
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

    private var dateChangeListener: DateChangeListener? = null
    fun setDataChangeListener(dateChangeListener: DateChangeListener?) {
        this.dateChangeListener = dateChangeListener
    }

    private var datePickerTouchListener: OnDatePickerTouchListener? = null
    fun setDatePickerTouchListener(datePickerTouchListener: OnDatePickerTouchListener?) {
        this.datePickerTouchListener = datePickerTouchListener
    }

    @Suppress("RedundantVisibilityModifier")
    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        WheelDatePicker(
            /*
            modifier = Modifier

                .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.d("DatePicker", "ACTIONN_DOWN")
                        datePickerTouchListener?.onDatePickerTouchDown()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.d("DatePicker", "ACTIONN_UP")
                        datePickerTouchListener?.onDatePickerTouchUp()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        Log.d("DatePicker", "ACTIONN_CANCEL")
                        datePickerTouchListener?.onDatePickerTouchUp()
                        false
                    }
                    MotionEvent.ACTION_OUTSIDE -> {
                        Log.d("DatePicker", "ACTIONN_OUTSIDE")
                        false
                    }
                    else -> {
                        Log.d("DatePicker", "ACTIONN_ELSE")
                        false
                    }
                }
                //true


                .pointerInput(PointerEventType.Press) {
                    detectDragGestures { change, dragAmount ->
                        if (change.pressed) {
                            Log.d("DatePicker", "ACTIONN_DOWN")
                            datePickerTouchListener?.onDatePickerTouchDown()
                        }
                        else {
                            Log.d("DatePicker", "ACTIONN_UP")
                            datePickerTouchListener?.onDatePickerTouchUp()
                        }
                    }

                },
                */
            offset = offsetState.value,
            yearsRange = yearsRangeState.value,
            startDate = startDateState.value,
            maxDate = maxDateState.value,
            //toDate = toDateState.value,
            selectorEffectEnabled = selectorEffectEnabledState.value,
            textSize = textSizeState.value,
            textBold = textBoldState.value,
            onDateChanged = { day, month, year, date ->
                dateChangeListener?.onDateChanged(date, day, month, year)
            },
            darkModeEnabled = darkModeEnabledState.value)
            isClickable
    }


    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }



}