package com.dynatech2012.kamleonuserapp.views.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.dynatech2012.kamleonuserapp.R

class AnalyticListItemView : LinearLayout {
    private var view: View =
        LayoutInflater.from(context).inflate(R.layout.layout_analytics_list_item, this, true)

    private var ivRight: ImageView? = null
    private var tvType: TextView? = null
    private var tvName: TextView? = null
    private var tvDesc: TextView? = null

    constructor(context: Context?) : super(context!!) {
        commonInit()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        commonInit()
        attrs?.let {
            parseAttributes(it)
        }
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
        commonInit()
        attrs?.let {
            parseAttributes(it)
        }
    }

    private fun commonInit() {
        tvType = view.findViewById(R.id.tvType)
        tvName = view.findViewById(R.id.tvTitle)
        tvDesc = view.findViewById(R.id.tvDesc)
        ivRight = view.findViewById(R.id.ivRight)
    }

    private fun parseAttributes(attrs: AttributeSet) {
        val attrEditText =
            context.obtainStyledAttributes(attrs, R.styleable.AnalyticListItemViewStyle)

        try {
            tvType?.text = attrEditText.getString(R.styleable.AnalyticListItemViewStyle_typeText) ?: ""
            tvName?.text = attrEditText.getString(R.styleable.AnalyticListItemViewStyle_titleText) ?: ""
            tvDesc?.text = attrEditText.getString(R.styleable.AnalyticListItemViewStyle_descText) ?: ""

            val nType = attrEditText.getInteger(R.styleable.AnalyticListItemViewStyle_type, 0)
            if (nType == 0) {
                ivRight?.setImageResource(R.drawable.image_profile)
            } else if (nType == 1) {
                ivRight?.setImageResource(R.drawable.image_profile)
            } else if (nType == 2) {
                ivRight?.setImageResource(R.drawable.image_profile)
            }
        } finally {
            attrEditText.recycle()
        }
    }
}