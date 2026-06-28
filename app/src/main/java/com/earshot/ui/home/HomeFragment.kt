package com.earshot.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.earshot.R
import com.earshot.bluetooth.BluetoothRepository
import com.earshot.bluetooth.BluetoothState
import com.earshot.databinding.FragmentHomeBinding
import com.earshot.ui.base.BaseFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Home Screen Fragment.
 *
 * Displays the app overview, Bluetooth connection status, and quick action buttons.
 * Shows whether a Bluetooth device is connected.
 */
class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val repository by lazy { BluetoothRepository(requireContext()) }

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
        // Observe Bluetooth state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.bluetoothState.collectLatest { state ->
                    updateConnectionStatus(state)
                }
            }
        }
    }

    private fun updateConnectionStatus(state: BluetoothState) {
        // Guard against accessing binding after onDestroyView
        _binding ?: return

        binding.tvStatus.text = when (state) {
            is BluetoothState.Connected -> {
                getString(R.string.home_status_connected) + ": " + state.device.name
            }
            is BluetoothState.Connecting -> {
                getString(R.string.home_status_connected) + "..."
            }
            else -> {
                getString(R.string.home_status_disconnected)
            }
        }
    }

    private fun setupClickListeners() {
        // Navigate to Device screen
        binding.btnConnectDevice.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_deviceFragment)
        }

        // Navigate to Gesture Mapping screen
        binding.btnMapGestures.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_gestureFragment)
        }

        // Note: Camera Settings button was removed from layout.
        // Camera settings are now accessible via the built-in settings button
        // in the Camera fragment (bottom sheet).
    }

    override fun onResume() {
        super.onResume()
        // Refresh connection status
        if (repository.isReady()) {
            val state = repository.bluetoothState.value
            updateConnectionStatus(state)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}