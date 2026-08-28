package com.example.budgetmilestonetracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.budgetmilestonetracker.MainActivity
import com.example.budgetmilestonetracker.R
import com.example.budgetmilestonetracker.databinding.FragmentProfileBinding
import com.example.budgetmilestonetracker.viewmodels.ProfileViewModel

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the current user's data
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.etUsername.setText(user.username)
                binding.switchDarkMode.isChecked = user.darkMode
                // Set language radio button
                when (user.language) {
                    "en" -> binding.radioEnglish.isChecked = true
                    "ms" -> binding.radioMalay.isChecked = true
                    "zh" -> binding.radioMandarin.isChecked = true
                }
            }
        }

        // Save profile changes
        binding.btnSave.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            if (username.isEmpty()) {
                binding.etUsername.error = getString(R.string.required)
                return@setOnClickListener
            }

            val darkMode = binding.switchDarkMode.isChecked
            val lang = when {
                binding.radioMalay.isChecked -> "ms"
                binding.radioMandarin.isChecked -> "zh"
                else -> "en"
            }

            viewModel.saveProfile(username, darkMode, lang)
            // Apply changes immediately
            MainActivity.savePreferences(requireContext(), darkMode, lang)
            requireActivity().recreate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}