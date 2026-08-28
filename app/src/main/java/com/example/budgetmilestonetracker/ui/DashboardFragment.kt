package com.example.budgetmilestonetracker.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetmilestonetracker.R
import com.example.budgetmilestonetracker.databinding.FragmentDashboardBinding
import com.example.budgetmilestonetracker.viewmodels.DashboardViewModel

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: CategoryProgressAdapter

    companion object { private const val TAG = "DashboardFragment" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        // Greeting – comes from the ViewModel on a background thread
        viewModel.greeting.observe(viewLifecycleOwner) { username ->
            Log.d(TAG, "Greeting updated: $username")
            binding.tvGreeting.text = getString(R.string.greeting, username)
        }

        adapter = CategoryProgressAdapter { catWithSpent ->
            Log.i(TAG, "Category clicked: ${catWithSpent.name} (id=${catWithSpent.categoryId})")
            val action = DashboardFragmentDirections.actionDashboardFragmentToTransactionListFragment(catWithSpent.categoryId)
            findNavController().navigate(action)
        }
        binding.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCategories.adapter = adapter

        viewModel.categoriesWithSpent.observe(viewLifecycleOwner) { list ->
            Log.d(TAG, "Categories updated, count: ${list.size}")
            adapter.submitList(list)
        }

        viewModel.totalSpentThisMonth.observe(viewLifecycleOwner) { total ->
            Log.d(TAG, "Total spent this month: RM %.2f".format(total))
            binding.tvTotalSpent.text = "RM %.2f".format(total)
        }

        // Nudge card
        viewModel.nudge.observe(viewLifecycleOwner) { nudge ->
            if (nudge != null) {
                Log.i(TAG, "Nudge shown: ${nudge.categoryName} -> RM${nudge.suggestedAmount} to ${nudge.goalLabel}")
                binding.cardNudge.visibility = View.VISIBLE
                binding.tvNudgeMessage.text = getString(R.string.nudge_message,
                    nudge.categoryName,
                    String.format("%.2f", nudge.suggestedAmount),
                    nudge.goalLabel)
                binding.btnNudgeAction.setOnClickListener {
                    Log.i(TAG, "Nudge accepted: saving RM${nudge.suggestedAmount} to goal ${nudge.goalId}")
                    viewModel.acceptNudge(nudge.goalId, nudge.suggestedAmount)
                    Toast.makeText(requireContext(),
                        getString(R.string.nudge_saved, String.format("%.2f", nudge.suggestedAmount)),
                        Toast.LENGTH_SHORT).show()
                    binding.cardNudge.visibility = View.GONE
                }
            } else {
                Log.d(TAG, "No nudge to show")
                binding.cardNudge.visibility = View.GONE
            }
        }

        // FAB and button navigation
        binding.fabAddTransaction.setOnClickListener {
            Log.i(TAG, "FAB add transaction clicked")
            findNavController().navigate(DashboardFragmentDirections.actionDashboardFragmentToTransactionFormFragment())
        }
        binding.btnManageCategories.setOnClickListener {
            Log.i(TAG, "Navigate to Manage Categories")
            findNavController().navigate(DashboardFragmentDirections.actionDashboardFragmentToCategoryManageFragment())
        }
        binding.btnSavingsGoals.setOnClickListener {
            Log.i(TAG, "Navigate to Savings Goals")
            findNavController().navigate(DashboardFragmentDirections.actionDashboardFragmentToSavingsGoalsFragment())
        }
        binding.btnChart.setOnClickListener {
            Log.i(TAG, "Navigate to Spending Chart")
            findNavController().navigate(R.id.action_dashboardFragment_to_chartFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume – refreshing extras")
        viewModel.refreshExtras()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}