package com.example.budgetmilestonetracker.ui

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetmilestonetracker.data.db.CategoryWithSpent
import com.example.budgetmilestonetracker.databinding.ItemCategoryProgressBinding

class CategoryProgressAdapter(private val onClick: (CategoryWithSpent) -> Unit) :
    ListAdapter<CategoryWithSpent, CategoryProgressAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemCategoryProgressBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryWithSpent, onClick: (CategoryWithSpent) -> Unit) {
            binding.tvName.text = item.name

            val progress = if (item.monthlyLimit > 0)
                (item.totalSpent / item.monthlyLimit * 100).toInt() else 0
            binding.progressBar.progress = progress.coerceAtMost(100)
            binding.tvProgress.text = "RM %.2f / RM %.2f".format(item.totalSpent, item.monthlyLimit)

            val remaining = item.monthlyLimit - item.totalSpent
            binding.tvRemaining.text = if (remaining > 0)
                "Left: RM %.2f".format(remaining)
            else
                "Over by RM %.2f".format(-remaining)

            val remainingColor = when {
                progress >= 100 -> android.R.color.holo_red_dark
                progress >= 80  -> android.R.color.holo_orange_dark
                else            -> android.R.color.holo_green_dark
            }
            binding.tvRemaining.setTextColor(
                ContextCompat.getColor(binding.root.context, remainingColor)
            )

            val barColor = when {
                progress >= 100 -> android.R.color.holo_red_dark
                progress >= 80  -> android.R.color.holo_orange_dark
                else            -> android.R.color.holo_green_dark
            }
            binding.progressBar.progressTintList =
                ColorStateList.valueOf(binding.root.context.getColor(barColor))

            // Load icon drawable
            val resId = binding.root.context.resources.getIdentifier(
                item.iconResName, "drawable", binding.root.context.packageName
            )
            if (resId != 0) binding.ivIcon.setImageResource(resId)
            else binding.ivIcon.setImageResource(android.R.drawable.ic_menu_manage)

            // White icon on dark surface (dark mode), black icon on light surface (light mode)
            val isDarkMode = (binding.root.context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val iconTint = if (isDarkMode) Color.WHITE else Color.BLACK
            ImageViewCompat.setImageTintList(binding.ivIcon, ColorStateList.valueOf(iconTint))

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryProgressBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryWithSpent>() {
        override fun areItemsTheSame(old: CategoryWithSpent, new: CategoryWithSpent) =
            old.categoryId == new.categoryId
        override fun areContentsTheSame(old: CategoryWithSpent, new: CategoryWithSpent) =
            old == new
    }
}