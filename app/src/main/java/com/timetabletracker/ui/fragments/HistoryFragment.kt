package com.timetabletracker.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.timetabletracker.databinding.FragmentHistoryBinding
import com.timetabletracker.ui.MainViewModel
import com.timetabletracker.ui.adapters.ChangeHistoryAdapter

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val adapter = ChangeHistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHistory.adapter = adapter

        viewModel.changeHistory.observe(viewLifecycleOwner) { history ->
            adapter.submitList(history)
            if (history.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvHistory.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvHistory.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
