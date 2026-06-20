package com.earshot.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.earshot.R
import com.earshot.databinding.FragmentHomeBinding
import com.earshot.repository.DeviceRepository
import com.earshot.ui.base.BaseFragment
import com.earshot.viewmodel.HomeViewModel

/**
 * Home Screen Fragment.
 * Displays the app overview, Bluetooth connection status, and quick action buttons.
 */
class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(DeviceRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            binding.tvStatus.text = if (isConnected) {
                getString(R.string.home_status_connected)
            } else {
                getString(R.string.home_status_disconnected)
            }
        }

        viewModel.connectedDeviceName.observe(viewLifecycleOwner) { deviceName ->
            if (deviceName != null) {
                binding.tvStatus.text = "${getString(R.string.home_status_connected)}: $deviceName"
            }
        }
    }

    private fun setupClickListeners() {
        // Navigate to Device screen
        binding.btnConnectDevice.setOnClickListener {
            findNavController().navigate(R.id.deviceFragment)
        }

        // Navigate to Gesture Mapping screen
        binding.btnMapGestures.setOnClickListener {
            findNavController().navigate(R.id.gestureFragment)
        }

        // Navigate to Camera Settings screen
        binding.btnCameraSettings.setOnClickListener {
            findNavController().navigate(R.id.cameraFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshConnectionStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}