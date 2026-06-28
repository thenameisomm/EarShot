package com.earshot.ui.device

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.earshot.R
import com.earshot.bluetooth.BluetoothDevice
import com.earshot.bluetooth.UiStatus
import com.earshot.databinding.FragmentDeviceBinding
import com.earshot.ui.base.BaseFragment
import com.earshot.viewmodel.BluetoothViewModel

/**
 * Device Screen Fragment.
 *
 * Displays list of paired/connected Bluetooth devices with scanning functionality.
 * Handles runtime permissions for Bluetooth scanning on Android 12+.
 *
 * ## Features
 *
 * - Scan for nearby Bluetooth devices
 * - Connect/disconnect from devices
 * - Display connection status
 * - Handle Bluetooth permissions
 */
class DeviceFragment : BaseFragment() {

    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BluetoothViewModel by viewModels {
        BluetoothViewModel.Factory(requireContext())
    }

    private lateinit var pairedDevicesAdapter: DeviceAdapter
    private lateinit var discoveredDevicesAdapter: DeviceAdapter

    // Permission launcher for Bluetooth scanning
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // Permission granted, start scanning
            startScanning()
        } else {
            // Show permission denied message
            Toast.makeText(
                requireContext(),
                getString(R.string.bluetooth_permission_denied),
                Toast.LENGTH_LONG
            ).show()
            viewModel.clearPermissionRequired()
        }
    }

    // Permission launcher for requesting Bluetooth to be enabled
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check Bluetooth state after returning from settings
        viewModel.checkBluetoothState()
        if (viewModel.bluetoothEnabled.value == true) {
            // Bluetooth is now enabled, start scanning
            startScanning()
        }
    }

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

        setupRecyclerViews()
        setupObservers()
        setupClickListeners()

        // Check initial state and request permissions if needed
        initializeBluetooth()
    }

    private fun setupRecyclerViews() {
        // Paired devices adapter
        pairedDevicesAdapter = DeviceAdapter(
            onDeviceClick = { device -> onDeviceSelected(device) },
            onActionClick = { device -> onActionClicked(device) }
        )

        binding.rvPairedDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pairedDevicesAdapter
        }

        // Discovered devices adapter
        discoveredDevicesAdapter = DeviceAdapter(
            onDeviceClick = { device -> onDeviceSelected(device) },
            onActionClick = { device -> onActionClicked(device) }
        )

        binding.rvDiscoveredDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = discoveredDevicesAdapter
        }
    }

    private fun setupObservers() {
        // Observe UI state
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            updateUi(state.status)
        }

        // Observe paired devices
        viewModel.pairedDevices.observe(viewLifecycleOwner) { devices ->
            pairedDevicesAdapter.submitList(devices)
            binding.tvPairedDevicesHeader.isVisible = devices.isNotEmpty()
            binding.rvPairedDevices.isVisible = devices.isNotEmpty()
            updateEmptyState()
        }

        // Observe discovered devices
        viewModel.discoveredDevices.observe(viewLifecycleOwner) { devices ->
            discoveredDevicesAdapter.submitList(devices)
            binding.tvDiscoveredDevicesHeader.isVisible = devices.isNotEmpty()
            binding.rvDiscoveredDevices.isVisible = devices.isNotEmpty()
            updateEmptyState()
        }

        // Observe scanning state
        viewModel.isScanning.observe(viewLifecycleOwner) { isScanning ->
            binding.progressIndicator.isVisible = isScanning
            binding.btnScan.isEnabled = true
            binding.btnScan.text = if (isScanning) {
                getString(R.string.device_stop_scan)
            } else {
                getString(R.string.device_scan)
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Observe permission required
        viewModel.permissionRequired.observe(viewLifecycleOwner) { required ->
            if (required == true) {
                requestBluetoothPermissions()
            }
        }
    }

    private fun setupClickListeners() {
        // Back button navigation
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnScan.setOnClickListener {
            if (viewModel.getIsScanning()) {
                viewModel.stopScanning()
            } else {
                checkAndStartScanning()
            }
        }

        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnect()
        }
    }

    /**
     * Update UI based on status.
     */
    private fun updateUi(status: UiStatus) {
        when (status) {
            is UiStatus.NotAvailable -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.bluetooth_not_available),
                    Toast.LENGTH_LONG
                ).show()
            }
            is UiStatus.Disabled -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.bluetooth_enable_manually),
                    Toast.LENGTH_LONG
                ).show()
            }
            is UiStatus.PermissionRequired -> {
                requestBluetoothPermissions()
            }
            is UiStatus.Connected -> {
                binding.cardConnectionStatus.isVisible = true
                binding.tvConnectionStatus.text = getString(
                    R.string.device_connected_to,
                    status.device.name
                )
                binding.tvConnectionState.text = getString(R.string.device_connected)
            }
            is UiStatus.Connecting -> {
                binding.cardConnectionStatus.isVisible = true
                binding.tvConnectionStatus.text = getString(R.string.device_connecting)
                binding.tvConnectionState.text = getString(R.string.device_connecting)
            }
            is UiStatus.Disconnected, is UiStatus.Ready -> {
                binding.cardConnectionStatus.isVisible = false
            }
            is UiStatus.Error -> {
                Toast.makeText(requireContext(), status.message, Toast.LENGTH_SHORT).show()
            }
            else -> { /* Handle other states */ }
        }
    }

    private fun updateEmptyState() {
        val pairedEmpty = pairedDevicesAdapter.currentList.isEmpty()
        val discoveredEmpty = discoveredDevicesAdapter.currentList.isEmpty()
        val isScanning = viewModel.isScanning.value ?: false

        binding.tvEmpty.isVisible = pairedEmpty && discoveredEmpty && !isScanning
    }

    /**
     * Initialize Bluetooth - check state and load devices.
     */
    private fun initializeBluetooth() {
        viewModel.checkBluetoothState()

        if (viewModel.bluetoothAvailable.value == false) {
            Toast.makeText(
                requireContext(),
                getString(R.string.bluetooth_not_available),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (viewModel.hasScanPermissions()) {
            viewModel.loadPairedDevices()
            // Automatically start scanning if Bluetooth is enabled
            if (viewModel.bluetoothEnabled.value == true) {
                startScanning()
            }
        } else {
            requestBluetoothPermissions()
        }
    }

    /**
     * Check state and start scanning.
     */
    private fun checkAndStartScanning() {
        viewModel.checkBluetoothState()

        if (viewModel.bluetoothAvailable.value == false) {
            Toast.makeText(
                requireContext(),
                getString(R.string.bluetooth_not_available),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (viewModel.bluetoothEnabled.value == false) {
            Toast.makeText(
                requireContext(),
                getString(R.string.bluetooth_enable_manually),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (viewModel.hasScanPermissions()) {
            startScanning()
        } else {
            requestBluetoothPermissions()
        }
    }

    /**
     * Start scanning for devices.
     */
    private fun startScanning() {
        viewModel.startScanning()
    }

    /**
     * Request Bluetooth permissions.
     */
    private fun requestBluetoothPermissions() {
        val permissions = viewModel.getRequiredPermissions()

        if (permissions.isEmpty()) {
            // No permissions needed
            startScanning()
            return
        }

        // Check which permissions we need to request
        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            bluetoothPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // All permissions already granted
            startScanning()
        }
    }

    private fun onDeviceSelected(device: BluetoothDevice) {
        // Show device details in a toast
        Toast.makeText(
            requireContext(),
            "${device.getDisplayName()}\n${device.address}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun onActionClicked(device: BluetoothDevice) {
        when {
            device.isConnected -> viewModel.disconnect()
            device.isPaired -> viewModel.connectToDevice(device)
            else -> viewModel.connectToDevice(device) // Pair and connect
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}