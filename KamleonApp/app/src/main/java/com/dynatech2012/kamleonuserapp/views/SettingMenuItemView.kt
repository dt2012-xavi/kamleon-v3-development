package com.dynatech2012.kamleonuserapp.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.dynatech2012.kamleonuserapp.R

class SettingMenuItemView : LinearLayout {

    private var view: View =
        LayoutInflater.from(context).inflate(R.layout.layout_setting_menu_item, this, true)
    private var ivRight: ImageView? = null
    private var switch: SwitchCompat? = null
    private var tvLabel: TextView? = null
    private var tvValue: TextView? = null

    interface SettingMenuItemViewListener {
        fun onSwitchChanged(menuView: SettingMenuItemView, isOn: Boolean)
        fun onMenuItemClicked(menuView: SettingMenuItemView)
    }

    private var menuItemListener: SettingMenuItemViewListener? = null
    fun setSettingMenuItemListener(listener: SettingMenuItemViewListener?) {
        menuItemListener = listener
        Log.d("SettingMenuItemView", "onMenuItemClicked")
        if (switch?.visibility == VISIBLE) {
            switch?.isClickable = true
            switch?.setOnClickListener {
                //switch?.isChecked = !(switch?.isChecked ?: false)
                menuItemListener?.onSwitchChanged(this, switch?.isChecked ?: false)
            }
        } else {
            view.isClickable = true
            view.setOnClickListener {
                menuItemListener?.onMenuItemClicked(this)
            }
        }
    }

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
        tvLabel = view.findViewById(R.id.tvLabel)
        tvValue = view.findViewById(R.id.tvValue)
        switch = view.findViewById(R.id.settingItemSwitch)
        ivRight = view.findViewById(R.id.settingItemRightArrow)
    }

    private fun parseAttributes(attrs: AttributeSet) {
        val attrEditText =
            context.obtainStyledAttributes(attrs, R.styleable.SettingMenuItemViewStyle)

        try {

            val attrText =
                attrEditText.getString(R.styleable.SettingMenuItemViewStyle_settingTypeLabel)

            attrText?.let { attrStr ->
                tvLabel?.text = attrStr
            }

            val attrDesc =
                attrEditText.getString(R.styleable.SettingMenuItemViewStyle_settingTypeValue)

            attrDesc?.let { descStr ->
                if (descStr.isNotEmpty()) {
                    tvValue?.text = descStr
                    tvValue?.visibility = VISIBLE
                } else {
                    tvValue?.text = ""
                    tvValue?.visibility = INVISIBLE
                }

            }

            val attrBool1 =
                attrEditText.getBoolean(R.styleable.SettingMenuItemViewStyle_settingRightArrow, false)
            attrBool1?.let { bool ->
                ivRight?.visibility = if (bool) VISIBLE else GONE
            }

            val attrBool2 =
                attrEditText.getBoolean(R.styleable.SettingMenuItemViewStyle_settingRightSwitch, false)

            attrBool2?.let { bool ->
                setupWith(bool)
            }
        } finally {
            attrEditText.recycle()
        }
    }

    fun getSwitchComp() : SwitchCompat? {
        return switch
    }

    private fun setupWith(switchState: Boolean) {
        switch?.visibility = if (switchState) VISIBLE else GONE

        if (switchState) {
            switch?.isEnabled = true
            /*switch?.isClickable = true
            switch?.setOnClickListener {
                switch?.isChecked = !(switch?.isChecked ?: false)
                menuItemListener?.onSwitchChanged(this, switch?.isChecked ?: false)
            }*/
        }
    }

    fun setValue(value: String) {
        tvValue?.text = value
        if (value.isNotEmpty() && tvValue?.visibility != VISIBLE) {
            tvValue?.visibility = VISIBLE
        }
    }
}