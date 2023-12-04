package com.dynatech2012.kamleonuserapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dynatech2012.kamleonuserapp.databinding.LayoutInvitationPendingListItemBinding
import com.dynatech2012.kamleonuserapp.models.Invitation

class NewInvitationListAdapter(
    //private var dataList: ArrayList<Invitation>
): ListAdapter<Invitation, NewInvitationListAdapter.InvitationViewHolder>(InvitationDiffCallback()) {
    lateinit var binding: LayoutInvitationPendingListItemBinding

    interface NotificationListItemViewListener {
        fun onClick(accepted: Boolean)
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

        fun bind(data: Invitation){
            tvSubtitle.text = data.invitationText
            tvDate.text = data.invitationTime
            tvConfirm.setOnClickListener {
                notiItemListener?.onClick(true)
            }
            tvDeny.setOnClickListener {
                notiItemListener?.onClick(false)
            }
        }
    }
}