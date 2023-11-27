package com.dynatech2012.kamleonuserapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.databinding.LayoutHomeListItemBinding

class HomeListAdapter(
    var dataList: ArrayList<HomeListItemModel>
): RecyclerView.Adapter<HomeListAdapter.HomeListHViewHolder>() {
    lateinit var binding: LayoutHomeListItemBinding
    data class HomeListItemModel(
        val name: String,
        var desc: String
    ) {

    }

    fun setDataSource(list: ArrayList<HomeListItemModel>) {
        dataList = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeListHViewHolder {
        binding = LayoutHomeListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val view = binding.root
        return HomeListHViewHolder(view)
        //return HomeListHViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_home_list_item, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: HomeListHViewHolder, position: Int) {
        holder.bind(dataList[position])
        val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
        params.marginStart = if (position == 0) 60 else 0
        holder.itemView.layoutParams = params
    }

    inner class HomeListHViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        /*private var tvName: TextView = itemView.findViewById(R.id.tvName)
        private var tvDesc: TextView = itemView.findViewById(R.id.tvDesc)*/

        /*
        private var tvType = binding.tvType
        private var tvName = binding.tvTitle
        private var tvDesc = binding.tvDesc

        */


        fun bind(data: HomeListItemModel){
            /*
            tvType.text = data.name
            tvName.text = data.name
            tvDesc.text = data.desc

            */
        }


    }
}