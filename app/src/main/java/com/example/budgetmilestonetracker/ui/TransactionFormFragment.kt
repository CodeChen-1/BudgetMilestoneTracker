package com.example.budgetmilestonetracker.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.budgetmilestonetracker.R
import com.example.budgetmilestonetracker.data.db.Category
import com.example.budgetmilestonetracker.data.db.ExpenseTransaction
import com.example.budgetmilestonetracker.databinding.FragmentTransactionFormBinding
import com.example.budgetmilestonetracker.viewmodels.TransactionFormViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.*

class TransactionFormFragment : Fragment() {
    private var _binding: FragmentTransactionFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionFormViewModel by viewModels()
    private val args: TransactionFormFragmentArgs by navArgs()
    private var selectedTimestamp = System.currentTimeMillis()
    private var categoryList: List<Category> = emptyList()
    private var transactionLoaded = false

    companion object { private const val TAG = "TransactionFormFragment" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentTransactionFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated – transactionId=${args.transactionId}, categoryId=${args.categoryId}")

        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            Log.d(TAG, "Categories loaded, count: ${categories.size}")
            categoryList = categories
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories.map { it.name }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter

            if (args.transactionId != 0L && !transactionLoaded) {
                transactionLoaded = true
                Log.d(TAG, "Loading existing transaction for editing")
                loadTransaction()
            } else if (args.categoryId != 0L) {
                val index = categories.indexOfFirst { it.id == args.categoryId }
                if (index != -1) {
                    Log.d(TAG, "Pre‑selecting category: ${categories[index].name}")
                    binding.spinnerCategory.setSelection(index)
                }
            }
        }

        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedTimestamp
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                selectedTimestamp = cal.timeInMillis
                Log.d(TAG, "Date picked: %02d/%02d/%04d".format(d, m+1, y))
                binding.btnPickDate.text = "%02d/%02d/%04d".format(d, m + 1, y)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show()
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val notes = binding.etNotes.text.toString().takeIf { it.isNotBlank() }

            // Validate inputs
            if (title.isEmpty()) {
                Log.w(TAG, "Save attempted with empty title")
                binding.tilTitle.error = getString(R.string.required)
                return@setOnClickListener
            } else {
                binding.tilTitle.error = null
            }
            if (amount <= 0) {
                Log.w(TAG, "Save attempted with amount <= 0")
                binding.tilAmount.error = getString(R.string.must_be_positive)
                return@setOnClickListener
            } else {
                binding.tilAmount.error = null
            }
            if (categoryList.isEmpty()) {
                Log.w(TAG, "Save attempted with no categories available")
                Toast.makeText(requireContext(), R.string.no_categories_toast, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val catId = categoryList[binding.spinnerCategory.selectedItemPosition].id
            Log.i(TAG, "Checking budget for categoryId=$catId, amount=$amount")

            viewLifecycleOwner.lifecycleScope.launch {
                val wouldExceed = viewModel.wouldExceedLimit(catId, amount)
                if (wouldExceed) {
                    Log.w(TAG, "Transaction would exceed monthly limit – showing warning")
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Budget Warning")
                        .setMessage("This transaction will exceed the monthly limit for this category. Do you want to proceed?")
                        .setPositiveButton("Yes") { _, _ ->
                            Log.i(TAG, "User confirmed overspend – saving transaction")
                            saveAndFinish(catId, title, amount, notes)
                        }
                        .setNegativeButton("No") { _, _ -> Log.d(TAG, "User cancelled overspend") }
                        .show()
                } else {
                    Log.i(TAG, "Within budget – saving transaction directly")
                    saveAndFinish(catId, title, amount, notes)
                }
            }
        }
    }

    private fun saveAndFinish(catId: Long, title: String, amount: Double, notes: String?) {
        Log.i(TAG, "Saving transaction: title=$title, amount=$amount, categoryId=$catId")
        viewModel.saveTransaction(
            if (args.transactionId != 0L) args.transactionId else null,
            catId, title, amount, selectedTimestamp, notes
        )
        viewModel.performRoundUp(
            ExpenseTransaction(categoryId = catId, title = title, amount = amount,
                timestamp = selectedTimestamp, notes = notes)
        )
        Log.d(TAG, "Transaction saved, popping back")
        findNavController().popBackStack()
    }

    private fun loadTransaction() {
        viewLifecycleOwner.lifecycleScope.launch {
            val t = viewModel.getTransactionById(args.transactionId)
            t?.let {
                Log.d(TAG, "Loaded existing transaction: ${it.title}")
                binding.etTitle.setText(it.title)
                binding.etAmount.setText(it.amount.toString())
                selectedTimestamp = it.timestamp
                binding.btnPickDate.text = java.text.SimpleDateFormat(
                    "dd/MM/yyyy", Locale.getDefault()
                ).format(java.util.Date(it.timestamp))
                binding.etNotes.setText(it.notes)
                val index = categoryList.indexOfFirst { cat -> cat.id == it.categoryId }
                if (index != -1) binding.spinnerCategory.setSelection(index)
            }
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}