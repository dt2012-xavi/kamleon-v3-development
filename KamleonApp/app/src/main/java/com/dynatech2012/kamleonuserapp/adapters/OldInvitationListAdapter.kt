package com.dynatech2012.kamleonuserapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dynatech2012.kamleonuserapp.databinding.LayoutInvitationListItemBinding
import com.dynatech2012.kamleonuserapp.extensions.capitalize
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.InvitationStatus

class OldInvitationListAdapter(
    //private var dataList: ArrayList<Invitation>
): ListAdapter<Invitation, OldInvitationListAdapter.InvitationViewHolder>(InvitationDiffCallback()) {
    lateinit var binding: LayoutInvitationListItemBinding

    interface NotificationListItemViewListener {
        fun onClick(accepted: Boolean)
    }

    private var notiItemListener: NotificationListItemViewListener? = null
    fun setNotificationListItemViewListener(listener: NotificationListItemViewListener?) {
        notiItemListener = listener
    }

    /*
    fun setDataSource(list: ArrayList<Invitation>) {
        val diffCallback =
            InvitationDiffCallback2(dataList, list)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        dataList.clear()
        dataList.addAll(list)
        diffResult.dispatchUpdatesTo(this)
        //dataList = list
        //notifyDataSetChanged()
    }
    fun addDataSource(list: ArrayList<Invitation>) {
        val newList = dataList + list
        val diffCallback =
            InvitationDiffCallback2(dataList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        //dataList.clear()
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
        binding = LayoutInvitationListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InvitationViewHolder(binding.root)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int, playloads: MutableList<Any>) {
        when (val latestPlayload = playloads.lastOrNull()) {
            is InvitationChangePlayload.StatusChanged -> {
                holder.bindStatus(latestPlayload.status)
            }
            else -> onBindViewHolder(holder, position)
        }
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InvitationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private var tvTitle: TextView = binding.tvInvitationTitle
        private var tvSubtitle: TextView = binding.tvInvitationSubtitle
        private var tvDate: TextView = binding.tvInvitationDate
        private var tvStatus: TextView = binding.tvInvitationStatus

        fun bind(data: Invitation){
            tvSubtitle.text = data.invitationText
            tvDate.text = data.dateSent.formatTime
            tvStatus.text = data.status.rawValue.capitalize()
        }

        fun bindStatus(status: InvitationStatus) {
            tvStatus.text = status.rawValue.capitalize()
        }
    }
}

class InvitationDiffCallback : DiffUtil.ItemCallback<Invitation>() {
    override fun areItemsTheSame(oldItem: Invitation, newItem: Invitation): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: Invitation, newItem: Invitation): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: Invitation, newItem: Invitation): Any? {
        return when {
            oldItem.status != newItem.status ->
                InvitationChangePlayload.StatusChanged(newItem.status)
            else -> super.getChangePayload(oldItem, newItem)
        }
    }
}

interface InvitationChangePlayload {
    data class StatusChanged(val status: InvitationStatus): InvitationChangePlayload
}