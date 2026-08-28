package com.example.budgetmilestonetracker.ui

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetmilestonetracker.data.db.Category
import com.example.budgetmilestonetracker.databinding.ItemCategoryBinding

class CategoryAdapter(private val onClick: (Category) -> Unit) :
    ListAdapter<Category, CategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    class ViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category, onClick: (Category) -> Unit) {
            binding.tvName.text = category.name
            binding.tvLimit.text = "RM %.2f".format(category.monthlyLimit)

            // Load icon drawable
            val resId = binding.root.context.resources.getIdentifier(
                category.iconResName, "drawable", binding.root.context.packageName
            )
            if (resId != 0) binding.ivIcon.setImageResource(resId)
            else binding.ivIcon.setImageResource(android.R.drawable.ic_menu_manage)

            // White icon on dark surface (dark mode), black icon on light surface (light mode)
            val isDarkMode = (binding.root.context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val iconTint = if (isDarkMode) Color.WHITE else Color.BLACK
            ImageViewCompat.setImageTintList(binding.ivIcon, ColorStateList.valueOf(iconTint))

            binding.root.setOnClickListener { onClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }
}

class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
    override fun areItemsTheSame(old: Category, new: Category) = old.id == new.id
    override fun areContentsTheSame(old: Category, new: Category) = old == new
}