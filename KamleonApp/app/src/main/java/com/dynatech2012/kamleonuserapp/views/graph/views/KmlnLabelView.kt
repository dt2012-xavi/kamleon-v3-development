package com.dynatech2012.kamleonuserapp.views.graph.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.database.AveragesData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphViewMode

class KmlnLabelView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private var view : View = LayoutInflater.from(context).inflate(R.layout.layout_kmln_label, this, true)

    private var tvValueAvg =  view.findViewById(R.id.textViewAvgValue) as TextView
    private var tvValueUnit =  view.findViewById(R.id.textViewAvgUnit) as TextView
    private var tvTimstamp =  view.findViewById(R.id.textViewAvgTimestamp) as TextView
    private var vIndicator =  view.findViewById(R.id.labelIndicator) as View
    private var vDivider =  view.findViewById(R.id.v_graphics_label_divider) as View
    private var rvItems =  view.findViewById(R.id.rv_graphics_label_bar_clicked) as RecyclerView

    fun setIndicatorPos(deltaX: Float) {
        vIndicator.x = (this.width / 2.0 - deltaX - vIndicator.width / 2.0).toFloat()
    }

    fun setData(
        value: String,
        unit: String,
        label: String,
        items: ArrayList<AveragesData>,
        type: KamleonGraphDataType,
        viewMode: KamleonGraphViewMode
    ) {
        tvValueAvg.text = value
        tvValueUnit.text = unit
        tvTimstamp.text = label
        if (viewMode == KamleonGraphViewMode.Daily) {
            vDivider.visibility = View.VISIBLE
            rvItems.visibility = View.VISIBLE
            setAdapter(items, type)
        } else {
            vDivider.visibility = View.GONE
            rvItems.visibility = View.GONE
        }
    }

    private fun setAdapter(items: List<AveragesData>, type: KamleonGraphDataType) {
        val adapter = KmlnLabelAdapter()
        adapter.type = type
        rvItems.adapter = adapter
        rvItems.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter.submitList(items)
    }
}