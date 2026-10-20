package com.timetabletracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.timetabletracker.R
import com.timetabletracker.data.local.ChangeEvent
import com.timetabletracker.databinding.ItemChangeHistoryBinding

class ChangeHistoryAdapter :
    ListAdapter<ChangeEvent, ChangeHistoryAdapter.ChangeViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChangeViewHolder {
        val binding = ItemChangeHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChangeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChangeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChangeViewHolder(private val binding: ItemChangeHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: ChangeEvent) {
            binding.tvCheckTime.text = event.checkTime
            binding.tvSummary.text = event.summary.lines().firstOrNull() ?: event.summary

            val (colorRes, statusText) = if (event.hasChanges) {
                Pair(R.color.status_changes, "Changes Found")
            } else {
                Pair(R.color.status_ok, "No Changes")
            }

            binding.tvStatus.text = statusText
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )
            binding.viewStatusDot.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ChangeEvent>() {
        override fun areItemsTheSame(oldItem: ChangeEvent, newItem: ChangeEvent) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChangeEvent, newItem: ChangeEvent) =
            oldItem == newItem
    }
}
