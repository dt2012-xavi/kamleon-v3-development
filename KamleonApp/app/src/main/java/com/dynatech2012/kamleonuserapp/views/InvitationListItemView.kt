package com.dynatech2012.kamleonuserapp.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.models.Invitation

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
        tvTitle = view.findViewById(R.id.tv_invitation_title)
        tvDate = view.findViewById(R.id.tv_invitation_date)
        tvDesc = view.findViewById(R.id.tv_invitation_subtitle)
        tvAction = view.findViewById(R.id.tv_invitation_status)
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

class InvitationDiffCallback2(
    private val oldList: List<Invitation>, private val newList: List<Invitation>): DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return (oldList[oldItemPosition].id == newList[newItemPosition].id)
    }
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return (oldList[oldItemPosition] == newList[newItemPosition])
    }
}
