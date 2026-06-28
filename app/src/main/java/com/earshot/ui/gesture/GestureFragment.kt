package com.earshot.ui.gesture

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.earshot.R
import com.earshot.databinding.FragmentGestureBinding
import com.earshot.repository.SettingsRepository
import com.earshot.ui.base.BaseFragment
import com.earshot.viewmodel.GestureViewModel

/**
 * Gesture Mapping Screen Fragment.
 * Allows users to map earbud button gestures to camera actions.
 */
class GestureFragment : BaseFragment() {

    private var _binding: FragmentGestureBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GestureViewModel by viewModels {
        GestureViewModel.Factory(SettingsRepository(requireContext()))
    }

    private lateinit var gestureAdapter: GestureAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        gestureAdapter = GestureAdapter(
            availableActions = viewModel.availableActions,
            onActionSelected = { gestureType, action ->
                viewModel.updateGestureAction(gestureType, action)
            },
            context = requireContext()
        )

        binding.rvGestures.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = gestureAdapter
        }
    }

    private fun setupObservers() {
        viewModel.gestureMappings.observe(viewLifecycleOwner) { mappings ->
            gestureAdapter.submitList(mappings)
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                val message = if (it) {
                    getString(R.string.success)
                } else {
                    getString(R.string.error)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearSaveStatus()
            }
        }
    }

    private fun setupClickListeners() {
        // Back button navigation
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            viewModel.saveMappings()
            findNavController().navigateUp()
        }

        binding.btnReset.setOnClickListener {
            viewModel.resetMappings()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}