package com.example.budgetmilestonetracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetmilestonetracker.data.db.ExpenseTransaction
import com.example.budgetmilestonetracker.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onDeleteClick: (ExpenseTransaction) -> Unit,
    private val onItemClick: (ExpenseTransaction) -> Unit
) : ListAdapter<ExpenseTransaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {

    class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(t: ExpenseTransaction, onDelete: (ExpenseTransaction) -> Unit, onClick: (ExpenseTransaction) -> Unit) {
            binding.tvTitle.text = t.title
            binding.tvAmount.text = "RM %.2f".format(t.amount)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(t.timestamp))
            binding.tvNote.text = t.notes ?: ""
            binding.btnDelete.setOnClickListener { onDelete(t) }
            binding.root.setOnClickListener { onClick(t) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick, onItemClick)
    }
}

class TransactionDiffCallback : DiffUtil.ItemCallback<ExpenseTransaction>() {
    override fun areItemsTheSame(old: ExpenseTransaction, new: ExpenseTransaction) = old.id == new.id
    override fun areContentsTheSame(old: ExpenseTransaction, new: ExpenseTransaction) = old == new
}