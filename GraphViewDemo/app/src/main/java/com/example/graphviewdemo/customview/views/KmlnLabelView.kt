package com.example.graphviewdemo.customview.views

import android.content.Context
import android.util.AttributeSet
import android.util.SizeF
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.graphviewdemo.R

class KmlnLabelView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private var view : View = LayoutInflater.from(context).inflate(R.layout.layout_kmln_label, this, true)

    private var tvValueAvg =  view.findViewById(R.id.textViewAvgValue) as TextView
    private var tvValueUnit =  view.findViewById(R.id.textViewAvgUnit) as TextView
    private var tvTimstamp =  view.findViewById(R.id.textViewAvgTimestamp) as TextView
    private var vIndicator =  view.findViewById(R.id.labelIndicator) as View

    fun setIndicatorPos(deltaX: Float) {
        vIndicator.x = (this.width / 2.0 - deltaX - vIndicator.width / 2.0).toFloat()
    }

    fun setData(value: String, unit: String, label: String) {
        tvValueAvg.text = value
        tvValueUnit.text = unit
        tvTimstamp.text = label
    }
}