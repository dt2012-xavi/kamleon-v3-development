package com.dynatech2012.kamleonuserapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dynatech2012.kamleonuserapp.R

class TipListAdapter(
    var dataList: Array<TipListItemModel>
): RecyclerView.Adapter<TipListAdapter.TipListHViewHolder>() {
    data class TipListItemModel(
        val name: String,
        var desc: String
    ) {

    }

    fun setDataSource(list: Array<TipListItemModel>) {
        dataList = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipListHViewHolder {
        return TipListHViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_viewholder_tip_list, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: TipListHViewHolder, position: Int) {
        holder.bind(dataList[position])
        val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
        params.marginStart = if (position == 0) 60 else 0
        holder.itemView.layoutParams = params
    }

    inner class TipListHViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private var tvName: TextView = itemView.findViewById(R.id.tvName)
        private var tvDesc: TextView = itemView.findViewById(R.id.tvDesc)

        fun bind(data: TipListItemModel){
            tvName.text = data.name
            tvDesc.text = data.desc
        }
    }
}