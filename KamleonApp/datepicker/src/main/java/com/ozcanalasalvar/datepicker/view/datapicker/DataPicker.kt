package com.ozcanalasalvar.datepicker.view.datapicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.compose.ui.graphics.Color
import com.google.android.material.timepicker.TimeFormat
import com.ozcanalasalvar.datepicker.compose.datapicker.DataPickerComposeView
import com.ozcanalasalvar.library.R
import com.ozcanalasalvar.datepicker.model.Time
import com.ozcanalasalvar.datepicker.utils.DateUtils

class DataPicker : LinearLayout {

    private var context: Context? = null
    private var pickerView: DataPickerComposeView? = null
    private var offset = 3
    private var textSize = 19
    private var textBold = false
    private var darkModeEnabled = true

    private var selectedValue = ""
    private var values = arrayListOf("value1", "value2")
    private var valueUnit = ""
    private var valueWidth = 250
    private var showDecimal = false

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setAttributes(context, attrs)
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setAttributes(context, attrs)
        init(context, attrs, defStyleAttr)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        setAttributes(context, attrs)
        init(context, attrs, defStyleAttr)
    }

    @SuppressLint("NonConstantResourceId")
    private fun setAttributes(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Picker)
        val N = a.indexCount
        for (i in 0 until N) {
            val attr = a.getIndex(i)
            if (attr == R.styleable.Picker_offset) {
                offset = a.getInteger(attr, 1)
            } else if (attr == R.styleable.Picker_darkModeEnabled) {
                darkModeEnabled = a.getBoolean(attr, true)
            } else if (attr == R.styleable.Picker_textSize) {
                textSize = a.getInt(attr, 20)
            } else if (attr == R.styleable.Picker_textBold) {
                textBold = a.getBoolean(attr, false)
            }
        }
        a.recycle()
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        this.context = context

        pickerView = DataPickerComposeView(
            context = context,
            attrs = attrs,
            defStyle = defStyleAttr,
        )

        setAttributes()
        this.addView(pickerView)
    }


    private fun setAttributes() {
        pickerView?.offset = offset
        pickerView?.selectorEffectEnabled = true
        pickerView?.textSize = textSize
        pickerView?.textBold = textBold
        pickerView?.darkModeEnabled = darkModeEnabled
        pickerView?.startValue = selectedValue
        pickerView?.valueUnit = valueUnit
        pickerView?.values = values
        pickerView?.valueWidth = valueWidth
        pickerView?.showDecimal = showDecimal

        pickerView?.setDataChangeListener(dataChangeListener)
        pickerView?.background = ColorDrawable(0x00FF00)
        background = ColorDrawable(0xFF0000)
    }

    interface DataChangeListener {
        fun onDataChanged(strValue: String, unitValue: String?)
    }

    private var dataChangeListener: DataChangeListener? = null
    fun setDataChangeListener(dataSelectListener: DataChangeListener) {
        dataChangeListener = dataSelectListener
        setAttributes()
    }


    fun setOffset(offset: Int) {
        this.offset = offset
        setAttributes()
    }

    fun setTextSize(textSize: Int) {
        this.textSize = textSize
        setAttributes()
    }

    /**
     * @param darkModeEnabled
     */
    fun setDarkModeEnabled(darkModeEnabled: Boolean) {
        this.darkModeEnabled = darkModeEnabled
        setAttributes()
    }

    fun setValue(value: String) {
        selectedValue = value
        setAttributes()
    }

    fun setValues(ary: ArrayList<String>) {
        values = ary
        setAttributes()
    }

    fun setValueUnit(unit: String) {
        valueUnit = unit
        setAttributes()
    }

    fun setValueWidth(width: Int) {
        valueWidth = width
        setAttributes()
    }

    fun setShowDecimal(show: Boolean) {
        showDecimal = show
        setAttributes()
    }
}