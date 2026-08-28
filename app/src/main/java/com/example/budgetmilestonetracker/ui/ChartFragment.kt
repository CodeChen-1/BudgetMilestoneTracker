package com.example.budgetmilestonetracker.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.budgetmilestonetracker.R
import com.example.budgetmilestonetracker.databinding.FragmentChartBinding
import com.example.budgetmilestonetracker.viewmodels.DashboardViewModel

class ChartFragment : Fragment() {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    // Share the same ViewModel with the parent activity so data is already there
    private val viewModel: DashboardViewModel by viewModels(ownerProducer = { requireActivity() })

    // A palette of 16 distinct colors for the slices
    private val palette = intArrayOf(
        0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(), 0xFF673AB7.toInt(),
        0xFF3F51B5.toInt(), 0xFF2196F3.toInt(), 0xFF00BCD4.toInt(), 0xFF009688.toInt(),
        0xFF4CAF50.toInt(), 0xFF8BC34A.toInt(), 0xFFCDDC39.toInt(), 0xFFFFC107.toInt(),
        0xFFFF9800.toInt(), 0xFFFF5722.toInt(), 0xFF795548.toInt(), 0xFF607D8B.toInt()
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Provide the translated "No data" text to the custom view
        binding.pieChart.setNoDataText(getString(R.string.no_data))

        viewModel.categoriesWithSpent.observe(viewLifecycleOwner) { list ->
            // Build slices with distinct colors
            val slices = list.mapIndexed { index, cat ->
                PieChartView.Slice(
                    name = cat.name,
                    amount = cat.totalSpent,
                    color = palette[index % palette.size]
                )
            }
            binding.pieChart.setData(slices)

            // Build legend
            val legendContainer = binding.legendContainer
            legendContainer.removeAllViews()
            for ((i, cat) in list.withIndex()) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)
                }
                // Color swatch
                val swatch = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(24, 24).apply { marginEnd = 8 }
                    setBackgroundColor(palette[i % palette.size])
                }
                row.addView(swatch)
                // Category name + amount – use a simple built‑in text appearance
                val tv = TextView(requireContext()).apply {
                    text = "${cat.name}  RM %.2f".format(cat.totalSpent)
                    setTextAppearance(android.R.style.TextAppearance_Medium)
                }
                row.addView(tv)
                legendContainer.addView(row)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}