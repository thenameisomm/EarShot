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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        CoroutineScope(Dispatchers.Main).launch {
            repository.bluetoothState.collectLatest { state ->
                updateConnectionStatus(state)
            }
        }
    }

    private fun updateConnectionStatus(state: BluetoothState) {
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