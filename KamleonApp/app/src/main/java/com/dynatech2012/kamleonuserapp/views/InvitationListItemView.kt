package com.dynatech2012.kamleonuserapp.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.dynatech2012.kamleonuserapp.R

class InvitationListItemView : LinearLayout {

    private var view: View =
        LayoutInflater.from(context).inflate(R.layout.layout_invitation_list_item, this, true)
    private var tvTitle: TextView? = null
    private var tvDate: TextView? = null
    private var tvDesc: TextView? = null
    private var tvAction: TextView? = null

    interface NotificationListItemViewListener {
    }

    private var notiItemListener: NotificationListItemViewListener? = null
    fun setNotificationListItemViewListener(listener: NotificationListItemViewListener?) {
        notiItemListener = listener
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
        tvTitle = view.findViewById(R.id.tvTitle)
        tvDate = view.findViewById(R.id.tvDate)
        tvDesc = view.findViewById(R.id.tvDesc)
        tvAction = view.findViewById(R.id.tvAction)
    }

    private fun parseAttributes(attrs: AttributeSet) {
        val attrNoti =
            context.obtainStyledAttributes(attrs, R.styleable.NotificationListItemViewStyle)

        try {

            var attrText =
                attrNoti.getString(R.styleable.NotificationListItemViewStyle_notiType)

            attrText?.let { attrStr ->
                tvTitle?.text = attrStr
            }

            attrText =
                attrNoti.getString(R.styleable.NotificationListItemViewStyle_notiDate)
            attrText?.let { attrStr ->
                tvDate?.text = attrStr
            }

            attrText =
                attrNoti.getString(R.styleable.NotificationListItemViewStyle_notiDesc)
            attrText?.let { attrStr ->
                tvDesc?.text = attrStr
            }

            attrText =
                attrNoti.getString(R.styleable.NotificationListItemViewStyle_notiAction)
            attrText?.let { attrStr ->
                tvAction?.text = attrStr
            }

        } finally {
            attrNoti.recycle()
        }
    }
}