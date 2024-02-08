package com.dynatech2012.kamleonuserapp.views.graph.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphBarDrawData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataXY

class KmlnFooterView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    interface KmlnFooterViewListener {
        fun onFooterViewStreakChanged(isStreak: Boolean)
    }

    private var footerListener: KmlnFooterViewListener? = null

    private var view : View = LayoutInflater.from(context).inflate(R.layout.layout_kmln_footer, this, true)

    private var containerView: LinearLayout = view.findViewById(R.id.layoutContainer) as LinearLayout
    private var ivStreak: ImageView =  view.findViewById(R.id.ivIconStreak) as ImageView
    private var tvValue1: TextView =  view.findViewById(R.id.tvStreakValue1) as TextView
    private var tvValue2: TextView =  view.findViewById(R.id.tvStreakValue2) as TextView
    private var tvLabelStreak: TextView =  view.findViewById(R.id.tvLabelStreak) as TextView


    private var bStreak: Boolean = false

    init {
        containerView.isClickable = true
        containerView.setOnClickListener {
            onStreakClicked()
        }

        updateViews(false, 0, 0, KamleonGraphDataType.volume)
    }

    fun setData(data: KamleonGraphBarDrawData, type: KamleonGraphDataType) {
        if (data.values.isEmpty()) {
            return
        }
        tvLabelStreak.text = when (type) {
            KamleonGraphDataType.hydration -> context.getString(R.string.graph_footer_streak)
            KamleonGraphDataType.electrolytes -> context.getString(R.string.graph_footer_streak)
            KamleonGraphDataType.volume -> context.getString(R.string.graph_footer_limit)
        }
        val preciseData = ArrayList<KamleonGraphDataXY>()
        for (i in 0 until data.values.size) {
            if (data.arePrecise[i] && data.values[i].y > 0) {
                preciseData.add(data.values[i])
            }
        }
        val range: IntRange = when (type) {
            KamleonGraphDataType.hydration -> {
                KamleonGraphDataType.HydrationStreakValue.toInt()..Int.MAX_VALUE
            }
            KamleonGraphDataType.electrolytes -> {
                KamleonGraphDataType.ElectrolyteStreakValueLower.toInt()..KamleonGraphDataType.ElectrolyteStreakValueUpper.toInt()
            }
            KamleonGraphDataType.volume -> {
                0..0
            }
        }
        //val streakData = preciseData.filter { it.y >= range.first && it.y <= range.last }
        val streakData = preciseData.filter { it.y.toInt() in range }
        val active = streakData.size
        val total = preciseData.size
        updateViews(false, active, total, type)
    }

    private fun updateViews(isStreak: Boolean, active: Int, total: Int, type: KamleonGraphDataType) {
        if (type == KamleonGraphDataType.volume) {
            tvValue1.visibility = View.GONE
            tvValue2.visibility = View.GONE
        }
        else {
            tvValue1.text = active.toString()
            tvValue2.text = context.getString(R.string.graph_footer_number_all, total.toString())
            tvValue1.visibility = View.VISIBLE
            tvValue2.visibility = View.VISIBLE
        }
    }

    private fun onStreakClicked() {
        updateStreak(!bStreak)
    }

    fun updateStreak(new: Boolean) {
        bStreak = new

        ivStreak.setImageResource(if (bStreak) R.drawable.ic_eye_open else R.drawable.ic_eye_crossed)
        val color = if (bStreak) R.color.kmln_graph_color_white else R.color.kmln_graph_color_active
        ivStreak.setColorFilter(context.getColor(color), android.graphics.PorterDuff.Mode.SRC_IN)
        if (bStreak) {
            containerView.background = context.getDrawable(R.drawable.kmln_bg_streak_view)
            tvValue1.setTextColor(context.getColor(R.color.kmln_graph_color_white))
            tvValue2.setTextColor(context.getColor(R.color.kmln_graph_color_white))
            tvLabelStreak.setTextColor(context.getColor(R.color.kmln_graph_color_white))
        } else {
            containerView.background = context.getDrawable(R.drawable.kmln_bg_streak_view_off)
            tvValue1.setTextColor(context.getColor(R.color.kmln_graph_color_active))
            tvValue2.setTextColor(context.getColor(R.color.kmln_graph_color_active))
            tvLabelStreak.setTextColor(context.getColor(R.color.black))
        }

        footerListener?.onFooterViewStreakChanged(bStreak)
    }

    fun setListener(listener: KmlnFooterViewListener) {
        footerListener = listener
    }
}