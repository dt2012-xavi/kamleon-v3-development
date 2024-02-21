package com.dynatech2012.kamleonuserapp.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dynatech2012.kamleonuserapp.constants.UrlConstants
import com.dynatech2012.kamleonuserapp.databinding.LayoutInvitationPendingListItemBinding
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.InvitationRole

class NewInvitationListAdapter(
    //private var dataList: ArrayList<Invitation>
): ListAdapter<Invitation, NewInvitationListAdapter.InvitationViewHolder>(InvitationDiffCallback()) {
    lateinit var binding: LayoutInvitationPendingListItemBinding

    interface NotificationListItemViewListener {
        fun onClick(invitation: Invitation, accepted: Boolean, optional: Boolean)
        fun onLinkClick(url: String)
    }

    private var notiItemListener: NotificationListItemViewListener? = null
    fun setNotificationListItemViewListener(listener: NotificationListItemViewListener?) {
        notiItemListener = listener
    }

    /*
    fun setDataSource(list: ArrayList<Invitation>) {
        val diffCallback = InvitationDiffCallback(dataList, list)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        dataList.clear()
        dataList.addAll(list)
        diffResult.dispatchUpdatesTo(this)
        //dataList = list
        //notifyDataSetChanged()
    }
    */

    /*
    override fun getItemCount(): Int {
        return dataList.size
    }
    */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
        binding = LayoutInvitationPendingListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InvitationViewHolder(binding.root)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InvitationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private var tvTitle: TextView = binding.tvInvitationTitle
        private var tvSubtitle: TextView = binding.tvInvitationSubtitle
        private var tvDate: TextView = binding.tvInvitationDate
        private var tvConfirm: TextView = binding.tvInvitationConfirm
        private var tvDeny: TextView = binding.tvInvitationDeny
        private var progressBar: ProgressBar = binding.pbInvitation
        private var invitationRow: LinearLayout = binding.llInvitationRow

        private var llPolicy: LinearLayout = binding.llInvitationPolicy
        private var llConsent: LinearLayout = binding.llInvitationConsent
        private var llNameConsent: LinearLayout = binding.llInvitationNameConsent
        private var tvPolicy: TextView = binding.tvInvitationPolicy
        private var tvConsent: TextView = binding.tvInvitationConsent
        private var tvNameConsent: TextView = binding.tvInvitationNameConsent
        private var cbPolicy: CheckBox = binding.cbInvitationPolicy
        private var cbConsent: CheckBox = binding.cbInvitationConsent
        private var cbNameConsent: CheckBox = binding.cbInvitationNameConsent
        private var enableConfirm = false

        fun bind(data: Invitation){
            tvSubtitle.text = data.invitationText
            tvDate.text = data.dateSent.formatTime

            when(data.role)
            {
                InvitationRole.KAMLEON_VIEWER -> {
                    tvPolicy.setOnClickListener {
                        notiItemListener?.onLinkClick(UrlConstants.URL_POLICY)
                    }
                    cbPolicy.setOnCheckedChangeListener { _, isChecked ->
                        tvConfirm.isEnabled = isChecked && cbConsent.isChecked
                    }
                    cbConsent.setOnCheckedChangeListener { _, isChecked ->
                        tvConfirm.isEnabled = isChecked && cbPolicy.isChecked
                    }
                }
                else -> {   // Admin
                    tvPolicy.setOnClickListener {
                        notiItemListener?.onLinkClick(UrlConstants.URL_POLICY_ADMIN)
                    }
                    llConsent.visibility = View.GONE
                    tvNameConsent.visibility = View.GONE
                    cbPolicy.setOnCheckedChangeListener { _, isChecked ->
                        tvConfirm.isEnabled = isChecked
                    }
                }
            }



            tvConfirm.setOnClickListener {
                progressBar.visibility = View.VISIBLE
                invitationRow.visibility = View.INVISIBLE
                notiItemListener?.onClick(data, true, cbNameConsent.isChecked)
            }
            tvDeny.setOnClickListener {
                progressBar.visibility = View.VISIBLE
                invitationRow.visibility = View.INVISIBLE
                notiItemListener?.onClick(data, accepted = false, optional = false)
            }
            tvConsent.setOnClickListener {
                notiItemListener?.onLinkClick(UrlConstants.URL_CONSENT)
            }
        }
    }
}