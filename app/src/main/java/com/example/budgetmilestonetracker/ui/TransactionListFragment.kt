package com.example.budgetmilestonetracker.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetmilestonetracker.databinding.FragmentTransactionListBinding
import com.example.budgetmilestonetracker.viewmodels.TransactionListViewModel
import com.google.android.material.snackbar.Snackbar

class TransactionListFragment : Fragment() {
    private var _binding: FragmentTransactionListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionListViewModel by viewModels()
    private val args: TransactionListFragmentArgs by navArgs()
    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setCategoryId(args.categoryId)

        adapter = TransactionAdapter(
            onDeleteClick = { transaction ->
                viewModel.deleteTransaction(transaction)
                Snackbar.make(binding.root, "Deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") { viewModel.insertTransaction(transaction) }
                    .show()
            },
            onItemClick = { transaction ->
                val action = TransactionListFragmentDirections.actionTransactionListFragmentToTransactionFormFragment(transactionId = transaction.id)
                findNavController().navigate(action)
            }
        )
        binding.recyclerTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTransactions.adapter = adapter

        // Observe the filtered list and toggle empty state
        viewModel.transactions.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            if (list.isNullOrEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerTransactions.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerTransactions.visibility = View.VISIBLE
            }
        }

        // Search listener
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { viewModel.setSearchQuery(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Sort spinner
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setSortMode(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // FAB
        binding.fabAddTransaction.setOnClickListener {
            val action = TransactionListFragmentDirections.actionTransactionListFragmentToTransactionFormFragment(categoryId = args.categoryId)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}