package com.example.graphviewdemo.customview.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.graphviewdemo.R

class KmlnOptionView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    interface KmlnOptionViewListener {
        fun onOptionSelected(optionView: KmlnOptionView)
    }

    private var view : View = LayoutInflater.from(context).inflate(R.layout.layout_kmln_option, this, true)
    private var tvText =  view.findViewById(R.id.idOptionText) as TextView
    private var layoutContent =  view.findViewById(R.id.idLayoutContent) as RelativeLayout
    private var layoutContainer =  view.findViewById(R.id.idLayoutContainer) as LinearLayout


    var listener: KmlnOptionViewListener? = null

    private var selectionState: Boolean = false

    init {
        layoutContent?.isClickable = true
        layoutContent.setOnClickListener {
            handleClick()
        }
    }

    fun setOptionText(text: String) {
        tvText.text = text
    }

    fun setSelection(selected: Boolean) {
        selectionState = selected
        updateView()
    }

    private fun updateView() {
        if (selectionState) {
            tvText.setTextColor(context.getColor(R.color.kmln_graph_header_item_text_active))
            layoutContainer.setBackgroundResource(R.drawable.kmln_bg_round_rect_shadow)
        } else {
            tvText.setTextColor(context.getColor(R.color.kmln_graph_header_item_text_normal))
            layoutContainer.background = null
        }

    }

    private fun handleClick() {
        if (selectionState) { return }

        setSelection(true)
        listener?.onOptionSelected(this)
    }
}
