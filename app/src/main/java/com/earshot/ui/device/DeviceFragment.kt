package com.earshot.ui.device

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.earshot.R
import com.earshot.databinding.FragmentDeviceBinding
import com.earshot.model.BluetoothDevice
import com.earshot.repository.DeviceRepository
import com.earshot.ui.base.BaseFragment
import com.earshot.viewmodel.DeviceViewModel

/**
 * Device Screen Fragment.
 * Displays list of paired/connected Bluetooth devices with scanning functionality.
 */
class DeviceFragment : BaseFragment() {

    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DeviceViewModel by viewModels {
        DeviceViewModel.Factory(DeviceRepository())
    }

    private lateinit var deviceAdapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(
            onDeviceClick = { device -> onDeviceSelected(device) },
            onActionClick = { device -> onActionClicked(device) }
        )

        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
    }

    private fun setupObservers() {
        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            deviceAdapter.submitList(devices)
            binding.tvEmpty.isVisible = devices.isEmpty()
            binding.rvDevices.isVisible = devices.isNotEmpty()
        }

        viewModel.isScanning.observe(viewLifecycleOwner) { isScanning ->
            binding.progressIndicator.isVisible = isScanning
            binding.btnScan.isEnabled = !isScanning
            binding.btnScan.text = if (isScanning) {
                getString(R.string.device_scanning)
            } else {
                getString(R.string.device_scan)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnScan.setOnClickListener {
            viewModel.startScanning()
        }
    }

    private fun onDeviceSelected(device: BluetoothDevice) {
        // Could show device details or connect directly
    }

    private fun onActionClicked(device: BluetoothDevice) {
        when {
            device.isConnected -> viewModel.disconnectDevice()
            device.isPaired -> viewModel.connectToDevice(device)
            else -> viewModel.pairDevice(device)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}