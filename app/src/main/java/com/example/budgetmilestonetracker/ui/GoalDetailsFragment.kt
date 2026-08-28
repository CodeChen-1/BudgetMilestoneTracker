package com.example.budgetmilestonetracker.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetmilestonetracker.R
import com.example.budgetmilestonetracker.databinding.FragmentGoalDetailsBinding
import com.example.budgetmilestonetracker.viewmodels.GoalDetailsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GoalDetailsFragment : Fragment() {
    private var _binding: FragmentGoalDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GoalDetailsViewModel by viewModels()
    private val args: GoalDetailsFragmentArgs by navArgs()

    companion object { private const val TAG = "GoalDetailsFragment" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentGoalDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated – goalId=${args.goalId}")
        viewModel.setGoalId(args.goalId)

        val adapter = GoalContributionAdapter { contribution ->
            Log.i(TAG, "Deleting contribution id=${contribution.id}, amount=${contribution.amount}")
            viewModel.deleteContribution(contribution)
        }
        binding.recyclerContributions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContributions.adapter = adapter

        viewModel.contributions.observe(viewLifecycleOwner) { contributions ->
            Log.d(TAG, "Contributions updated, count: ${contributions.size}")
            adapter.submitList(contributions)
        }
        viewModel.goal.observe(viewLifecycleOwner) { goal ->
            goal?.let {
                Log.d(TAG, "Goal details: ${it.label}, target=${it.targetAmount}")
                binding.tvGoalLabel.text = it.label
                binding.tvGoalTarget.text = "Target: RM %.2f".format(it.targetAmount)
            }
        }

        viewModel.goalAchieved.observe(viewLifecycleOwner) { achieved ->
            if (achieved) {
                Log.i(TAG, "Goal achieved!")
                Toast.makeText(requireContext(), "\uD83C\uDF89 Goal achieved! \uD83C\uDF89", Toast.LENGTH_LONG).show()
                viewModel.resetGoalAchieved()
            }
        }

        binding.fabAddContribution.setOnClickListener {
            Log.i(TAG, "Add contribution FAB clicked")
            showAddContributionDialog()
        }
    }

    private fun showAddContributionDialog() {
        Log.d(TAG, "Opening add contribution dialog")
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_contribution, null)
        val amountEdit = dialogView.findViewById<EditText>(R.id.etContributionAmount)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_contribution_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val amount = amountEdit.text.toString().toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    Log.i(TAG, "Adding contribution: amount=$amount")
                    viewModel.addContribution(amount, null)
                } else {
                    Log.w(TAG, "Add contribution cancelled – invalid amount")
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> Log.d(TAG, "Add contribution dialog cancelled") }
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.setGoalId(args.goalId)   // re‑trigger the LiveData queries
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}