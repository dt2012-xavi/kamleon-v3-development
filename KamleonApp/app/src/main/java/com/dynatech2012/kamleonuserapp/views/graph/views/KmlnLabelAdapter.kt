package com.dynatech2012.kamleonuserapp.views.graph.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.database.AveragesData
import com.dynatech2012.kamleonuserapp.databinding.LayoutKmlnLabelItemBinding
import com.dynatech2012.kamleonuserapp.extensions.formatTime
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import java.time.Instant
import java.util.Date


class KmlnLabelAdapter : ListAdapter<AveragesData, KmlnLabelAdapter.ViewHolder>(KmlnLabelDiffCallback()) {
    lateinit var binding: LayoutKmlnLabelItemBinding
    var type = KamleonGraphDataType.hydration
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = LayoutKmlnLabelItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var tvTimestamp = binding.tvGraphicsLabelItemTimestamp
        private var tvValue = binding.tvGraphicsLabelItemValue
        fun bind(item: AveragesData) {
            val date = Date(item.timestamp)
            tvTimestamp.text = date.formatTime
            val isPrecise = item.isPrecise
            // TODO: delete "adapterPosition == 0 &&" -> DONE
            val value = if (/*adapterPosition == 0 && */isPrecise) item.scoreValue(type).toInt() else "Low Volume"
            val un = if (/*adapterPosition == 0 && */isPrecise) itemView.context.getString(type.getUnit()) else ""
            tvValue.text = itemView.context.getString(
                R.string.graph_label_item_value, value, un)
        }
    }
}

class KmlnLabelDiffCallback : DiffUtil.ItemCallback<AveragesData>() {
    override fun areItemsTheSame(oldItem: AveragesData, newItem: AveragesData): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: AveragesData, newItem: AveragesData): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }
}