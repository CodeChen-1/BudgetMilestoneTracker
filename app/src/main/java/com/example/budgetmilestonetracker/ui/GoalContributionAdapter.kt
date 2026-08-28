package com.example.budgetmilestonetracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetmilestonetracker.data.db.GoalContribution
import com.example.budgetmilestonetracker.databinding.ItemGoalContributionBinding
import java.text.SimpleDateFormat
import java.util.*

class GoalContributionAdapter(private val onDeleteClick: (GoalContribution) -> Unit) :
    ListAdapter<GoalContribution, GoalContributionAdapter.ViewHolder>(GoalContributionDiffCallback()) {

    class ViewHolder(private val binding: ItemGoalContributionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(c: GoalContribution, onDeleteClick: (GoalContribution) -> Unit) {
            binding.tvAmount.text = "+ RM %.2f".format(c.amount)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(c.timestamp))
            binding.tvNote.text = c.note ?: ""
            binding.btnDelete.setOnClickListener { onDeleteClick(c) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGoalContributionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick)
    }
}

class GoalContributionDiffCallback : DiffUtil.ItemCallback<GoalContribution>() {
    override fun areItemsTheSame(old: GoalContribution, new: GoalContribution) = old.id == new.id
    override fun areContentsTheSame(old: GoalContribution, new: GoalContribution) = old == new
}