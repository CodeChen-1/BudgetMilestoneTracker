package com.example.budgetmilestonetracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetmilestonetracker.R
import com.example.budgetmilestonetracker.data.db.Category
import com.example.budgetmilestonetracker.databinding.FragmentCategoryManageBinding
import com.example.budgetmilestonetracker.viewmodels.CategoryManageViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CategoryManageFragment : Fragment() {
    private var _binding: FragmentCategoryManageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoryManageViewModel by viewModels()
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CategoryAdapter { category -> showCategoryOptionsDialog(category) }
        binding.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCategories.adapter = adapter

        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            adapter.submitList(categories)
            if (categories.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerCategories.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerCategories.visibility = View.VISIBLE
            }
        }

        binding.fabAddCategory.setOnClickListener { showAddCategoryDialog() }
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val nameEdit = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val limitEdit = dialogView.findViewById<EditText>(R.id.etCategoryLimit)
        val iconSpinner = dialogView.findViewById<Spinner>(R.id.spinnerIcon)
        val iconNames = resources.getStringArray(R.array.icon_names)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_category_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameEdit.text.toString().trim()
                val limit = limitEdit.text.toString().toDoubleOrNull() ?: 0.0
                val iconRes = iconNames[iconSpinner.selectedItemPosition]
                if (name.isNotEmpty()) viewModel.addCategory(name, limit, iconRes)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCategoryOptionsDialog(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(category.name)
            .setItems(arrayOf(getString(R.string.edit), getString(R.string.delete))) { _, which ->
                when (which) {
                    0 -> showEditCategoryDialog(category)
                    1 -> confirmDeleteCategory(category)
                }
            }
            .show()
    }

    // Ask for confirmation before deleting
    private fun confirmDeleteCategory(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete ${category.name}?")
            .setMessage("All transactions in this category will also be deleted.")
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteCategory(category) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        val nameEdit = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val limitEdit = dialogView.findViewById<EditText>(R.id.etCategoryLimit)
        val iconSpinner = dialogView.findViewById<Spinner>(R.id.spinnerIcon)
        val iconNames = resources.getStringArray(R.array.icon_names)
        nameEdit.setText(category.name)
        limitEdit.setText(category.monthlyLimit.toString())
        val currentIndex = iconNames.indexOfFirst { it == category.iconResName }
        if (currentIndex != -1) iconSpinner.setSelection(currentIndex)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_category_title)
            .setView(dialogView)
            .setPositiveButton(R.string.update) { _, _ ->
                val name = nameEdit.text.toString().trim()
                val limit = limitEdit.text.toString().toDoubleOrNull() ?: 0.0
                val iconRes = iconNames[iconSpinner.selectedItemPosition]
                if (name.isNotEmpty()) viewModel.updateCategory(category.copy(name = name, monthlyLimit = limit, iconResName = iconRes))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}