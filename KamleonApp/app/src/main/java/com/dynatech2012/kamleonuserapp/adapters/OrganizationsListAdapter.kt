package com.dynatech2012.kamleonuserapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dynatech2012.kamleonuserapp.databinding.LayoutKamleonOrganizationItemBinding
import com.dynatech2012.kamleonuserapp.models.Organization

class OrganizationsListAdapter: ListAdapter<Organization, OrganizationsListAdapter.OrganizationViewHolder>(OrganizationsDiffCallback()) {
    lateinit var binding: LayoutKamleonOrganizationItemBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrganizationViewHolder {
        binding = LayoutKamleonOrganizationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrganizationViewHolder(binding.root)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: OrganizationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrganizationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private var tvOrg: TextView = binding.tvOrgOrgName
        private var tvCenter: TextView = binding.tvOrgCenterName
        private var tvTeam: TextView = binding.tvOrgTeamName

        fun bind(data: Organization){
            tvOrg.text = data.organizationName
            tvCenter.text = data.centerName
            tvTeam.text = data.teamName
        }
    }
}

class OrganizationsDiffCallback : DiffUtil.ItemCallback<Organization>() {
    override fun areItemsTheSame(oldItem: Organization, newItem: Organization): Boolean {
        return oldItem.organizationName == newItem.organizationName &&
                oldItem.centerName == newItem.centerName &&
                oldItem.teamName == newItem.teamName
    }
    override fun areContentsTheSame(oldItem: Organization, newItem: Organization): Boolean {
        return oldItem.organizationName == newItem.organizationName &&
                oldItem.centerName == newItem.centerName &&
                oldItem.teamName == newItem.teamName
    }
}
