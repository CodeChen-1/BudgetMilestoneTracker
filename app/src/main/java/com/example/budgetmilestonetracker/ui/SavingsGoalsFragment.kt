package com.example.budgetmilestonetracker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetmilestonetracker.R
import com.example.budgetmilestonetracker.data.db.SavingsGoal
import com.example.budgetmilestonetracker.data.db.SavingsGoalWithForecast
import com.example.budgetmilestonetracker.databinding.FragmentSavingsGoalsBinding
import com.example.budgetmilestonetracker.viewmodels.SavingsGoalsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class SavingsGoalsFragment : Fragment() {
    private var _binding: FragmentSavingsGoalsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SavingsGoalsViewModel by viewModels()
    private lateinit var adapter: SavingsGoalAdapter

    companion object { private const val TAG = "SavingsGoalsFragment" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentSavingsGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        adapter = SavingsGoalAdapter(
            onItemClick = { item ->
                Log.i(TAG, "Goal clicked: ${item.goal.label} (id=${item.goal.id})")
                val action = SavingsGoalsFragmentDirections.actionSavingsGoalsFragmentToGoalDetailsFragment(item.goal.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { item ->
                Log.i(TAG, "Delete requested for goal: ${item.goal.label}")
                confirmDeleteGoal(item)
            },
            onEditClick = { item ->
                Log.i(TAG, "Edit requested for goal: ${item.goal.label}")
                showEditGoalDialog(item)
            }
        )
        binding.recyclerGoals.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGoals.adapter = adapter

        viewModel.goalsWithForecast.observe(viewLifecycleOwner) { goals ->
            Log.d(TAG, "Goals updated, count: ${goals.size}")
            adapter.submitList(goals)
            if (goals.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerGoals.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerGoals.visibility = View.VISIBLE
            }
        }

        binding.fabAddGoal.setOnClickListener {
            Log.i(TAG, "Add goal FAB clicked")
            showAddGoalDialog()
        }
    }

    private fun confirmDeleteGoal(item: SavingsGoalWithForecast) {
        Log.d(TAG, "Showing delete confirmation for goal: ${item.goal.label}")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete ${item.goal.label}?")
            .setMessage("All contributions for this goal will also be deleted.")
            .setPositiveButton(R.string.delete) { _, _ ->
                Log.i(TAG, "Deleting goal: ${item.goal.label} (id=${item.goal.id})")
                viewModel.deleteGoal(item.goal)
            }
            .setNegativeButton(R.string.cancel) { _, _ -> Log.d(TAG, "Delete cancelled") }
            .show()
    }

    private fun showAddGoalDialog() {
        Log.d(TAG, "Opening add goal dialog")
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_goal, null)
        val labelEdit = dialogView.findViewById<EditText>(R.id.etGoalLabel)
        val targetEdit = dialogView.findViewById<EditText>(R.id.etTargetAmount)
        val btnDeadline = dialogView.findViewById<Button>(R.id.btnPickDeadline)
        val checkRoundUp = dialogView.findViewById<CheckBox>(R.id.checkRoundUp)

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        var deadlineMillis = cal.timeInMillis
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        btnDeadline.text = sdf.format(Date(deadlineMillis))
        btnDeadline.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                deadlineMillis = cal.timeInMillis
                Log.d(TAG, "Deadline selected: %02d/%02d/%04d".format(d, m+1, y))
                btnDeadline.text = sdf.format(Date(deadlineMillis))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_savings_goal_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val label = labelEdit.text.toString().trim()
                val target = targetEdit.text.toString().toDoubleOrNull() ?: 0.0
                if (label.isNotEmpty() && target > 0) {
                    Log.i(TAG, "Adding goal: $label, target=$target, roundUp=${checkRoundUp.isChecked}")
                    viewModel.addGoal(label, target, deadlineMillis, checkRoundUp.isChecked)
                } else {
                    Log.w(TAG, "Add goal cancelled – empty label or invalid target")
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> Log.d(TAG, "Add goal dialog cancelled") }
            .show()
    }

    private fun showEditGoalDialog(item: SavingsGoalWithForecast) {
        Log.d(TAG, "Opening edit goal dialog for: ${item.goal.label}")
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_goal, null)
        val labelEdit = dialogView.findViewById<EditText>(R.id.etGoalLabel)
        val targetEdit = dialogView.findViewById<EditText>(R.id.etTargetAmount)
        val btnDeadline = dialogView.findViewById<Button>(R.id.btnPickDeadline)
        val checkRoundUp = dialogView.findViewById<CheckBox>(R.id.checkRoundUp)

        labelEdit.setText(item.goal.label)
        targetEdit.setText(item.goal.targetAmount.toString())
        checkRoundUp.isChecked = item.goal.roundUpEnabled

        val cal = Calendar.getInstance()
        cal.timeInMillis = item.goal.deadline
        var newDeadline = item.goal.deadline
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        btnDeadline.text = sdf.format(Date(newDeadline))
        btnDeadline.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                newDeadline = cal.timeInMillis
                Log.d(TAG, "New deadline selected: %02d/%02d/%04d".format(d, m+1, y))
                btnDeadline.text = sdf.format(Date(newDeadline))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_goal)
            .setView(dialogView)
            .setPositiveButton(R.string.update) { _, _ ->
                val label = labelEdit.text.toString().trim()
                val target = targetEdit.text.toString().toDoubleOrNull() ?: 0.0
                if (label.isNotEmpty() && target > 0) {
                    Log.i(TAG, "Updating goal: ${item.goal.id}, new label=$label, target=$target, roundUp=${checkRoundUp.isChecked}")
                    val updated = item.goal.copy(label = label, targetAmount = target,
                        deadline = newDeadline, roundUpEnabled = checkRoundUp.isChecked)
                    viewModel.updateGoal(updated)
                } else {
                    Log.w(TAG, "Edit goal cancelled – empty label or invalid target")
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> Log.d(TAG, "Edit goal dialog cancelled") }
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadGoals()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}