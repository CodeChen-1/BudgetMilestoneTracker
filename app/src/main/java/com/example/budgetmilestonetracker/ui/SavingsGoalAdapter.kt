package com.example.budgetmilestonetracker.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetmilestonetracker.data.db.SavingsGoalWithForecast
import com.example.budgetmilestonetracker.databinding.ItemSavingsGoalBinding

class SavingsGoalAdapter(
    private val onItemClick: (SavingsGoalWithForecast) -> Unit,
    private val onDeleteClick: (SavingsGoalWithForecast) -> Unit,
    private val onEditClick: (SavingsGoalWithForecast) -> Unit
) : ListAdapter<SavingsGoalWithForecast, SavingsGoalAdapter.ViewHolder>(SavingsGoalDiffCallback()) {

    class ViewHolder(private val binding: ItemSavingsGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SavingsGoalWithForecast,
                 onItemClick: (SavingsGoalWithForecast) -> Unit,
                 onDeleteClick: (SavingsGoalWithForecast) -> Unit,
                 onEditClick: (SavingsGoalWithForecast) -> Unit) {
            binding.tvLabel.text = item.goal.label
            binding.tvProgress.text = "RM %.2f / RM %.2f".format(item.totalSaved, item.goal.targetAmount)

            val remaining = item.goal.targetAmount - item.totalSaved
            binding.tvRemaining.text = if (remaining > 0) "Left: RM %.2f".format(remaining) else "Achieved!"

            // Deadline countdown
            val now = System.currentTimeMillis()
            val daysLeft = ((item.goal.deadline - now) / (1000 * 60 * 60 * 24)).toInt()
            binding.tvDeadline.text = when {
                item.goal.isAchieved -> "Completed"
                daysLeft < 0 -> "Overdue"
                daysLeft == 0 -> "Today is the deadline!"
                else -> "$daysLeft days left"
            }

            // Forecast label (new)
            binding.tvForecast.text = item.forecastLabel

            val percent = if (item.goal.targetAmount > 0) (item.totalSaved / item.goal.targetAmount * 100).toInt().coerceAtMost(100) else 0
            binding.progressBar.progress = percent

            val goalColor = when {
                percent >= 80 -> android.R.color.holo_green_dark
                percent >= 50 -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            binding.progressBar.progressTintList = ColorStateList.valueOf(binding.root.context.getColor(goalColor))

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnEdit.setOnClickListener { onEditClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavingsGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, onDeleteClick, onEditClick)
    }
}

class SavingsGoalDiffCallback : DiffUtil.ItemCallback<SavingsGoalWithForecast>() {
    override fun areItemsTheSame(old: SavingsGoalWithForecast, new: SavingsGoalWithForecast) = old.goal.id == new.goal.id
    override fun areContentsTheSame(old: SavingsGoalWithForecast, new: SavingsGoalWithForecast) = old == new
}