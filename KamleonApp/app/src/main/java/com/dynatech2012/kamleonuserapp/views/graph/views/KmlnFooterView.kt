package com.dynatech2012.kamleonuserapp.views.graph.views

import android.content.Context
import android.graphics.ColorFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphBarDrawData

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

        updateViews(false, 0, 0)
    }

    fun setData(data: KamleonGraphBarDrawData) {
        if (data.values.isEmpty()) {
            return
        }

        updateViews(false, 0, data.values.size)
    }

    private fun updateViews(isStreak: Boolean, active: Int, total: Int) {
        tvValue1.text = active.toString()
        tvValue2.text = "/$total"
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