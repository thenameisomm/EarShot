package com.earshot.ui.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.earshot.R
import com.earshot.databinding.FragmentCameraBinding
import com.earshot.model.CameraMode
import com.earshot.model.CameraSelection
import com.earshot.repository.SettingsRepository
import com.earshot.ui.base.BaseFragment
import com.earshot.viewmodel.CameraViewModel

/**
 * Camera Settings Screen Fragment.
 * Allows users to configure camera preferences (not implemented - UI only).
 */
class CameraFragment : BaseFragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels {
        CameraViewModel.Factory(SettingsRepository(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTimerDropdown()
        setupObservers()
        setupClickListeners()
    }

    private fun setupTimerDropdown() {
        val timerNames = viewModel.timerOptions.map { it.displayName }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            timerNames
        )
        binding.actvTimer.setAdapter(adapter)

        binding.actvTimer.setOnItemClickListener { _, _, position, _ ->
            viewModel.setTimerOption(viewModel.timerOptions[position])
        }
    }

    private fun setupObservers() {
        viewModel.cameraSelection.observe(viewLifecycleOwner) { selection ->
            when (selection) {
                CameraSelection.REAR -> binding.rbRear.isChecked = true
                CameraSelection.FRONT -> binding.rbFront.isChecked = true
                else -> {}
            }
        }

        viewModel.cameraMode.observe(viewLifecycleOwner) { mode ->
            when (mode) {
                CameraMode.PHOTO -> binding.toggleMode.check(R.id.btnPhoto)
                CameraMode.VIDEO -> binding.toggleMode.check(R.id.btnVideo)
                else -> {}
            }
        }

        viewModel.gridOverlayEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchGrid.isChecked = enabled
        }

        viewModel.timerOption.observe(viewLifecycleOwner) { option ->
            binding.actvTimer.setText(option.displayName, false)
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                val message = if (it) {
                    getString(R.string.camera_saved)
                } else {
                    getString(R.string.error)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearSaveStatus()
            }
        }
    }

    private fun setupClickListeners() {
        // Camera selection radio group
        binding.rgCamera.setOnCheckedChangeListener { _, checkedId ->
            val selection = when (checkedId) {
                R.id.rbRear -> CameraSelection.REAR
                R.id.rbFront -> CameraSelection.FRONT
                else -> CameraSelection.REAR
            }
            viewModel.setCameraSelection(selection)
        }

        // Camera mode toggle
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.btnPhoto -> CameraMode.PHOTO
                    R.id.btnVideo -> CameraMode.VIDEO
                    else -> CameraMode.PHOTO
                }
                viewModel.setCameraMode(mode)
            }
        }

        // Grid overlay switch
        binding.switchGrid.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setGridOverlayEnabled(isChecked)
        }

        // Save button
        binding.btnSave.setOnClickListener {
            viewModel.saveSettings()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}