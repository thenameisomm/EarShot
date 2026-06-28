package com.earshot.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.earshot.bluetooth.BluetoothDevice
import com.earshot.bluetooth.BluetoothRepository
import com.earshot.bluetooth.BluetoothState
import com.earshot.bluetooth.BluetoothUiState
import com.earshot.bluetooth.UiStatus
import com.earshot.bluetooth.toUiStatus
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for Bluetooth device management.
 *
 * Provides a clean interface for the UI layer to:
 * - Scan for devices
 * - Connect/disconnect from devices
 * - View device lists and connection status
 *
 * ## Architecture
 *
 * This ViewModel uses the new BluetoothRepository which provides:
 * - StateFlows for reactive UI updates
 * - Clean API for all Bluetooth operations
 * - Proper lifecycle management
 */
class BluetoothViewModel(
    private val repository: BluetoothRepository
) : ViewModel() {

    // Combined UI state
    private val _uiState = MutableLiveData(BluetoothUiState())
    val uiState: LiveData<BluetoothUiState> = _uiState

    // Device lists
    val devices: LiveData<List<BluetoothDevice>> = MutableLiveData(emptyList())
    val pairedDevices: LiveData<List<BluetoothDevice>> = MutableLiveData(emptyList())
    val discoveredDevices: LiveData<List<BluetoothDevice>> = MutableLiveData(emptyList())

    // Scanning state
    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    // Connection state
    private val _isConnecting = MutableLiveData(false)
    val isConnecting: LiveData<Boolean> = _isConnecting

    // Error messages
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // Permission required flag
    private val _permissionRequired = MutableLiveData(false)
    val permissionRequired: LiveData<Boolean> = _permissionRequired

    // Bluetooth hardware state
    private val _bluetoothAvailable = MutableLiveData(true)
    val bluetoothAvailable: LiveData<Boolean> = _bluetoothAvailable

    private val _bluetoothEnabled = MutableLiveData(false)
    val bluetoothEnabled: LiveData<Boolean> = _bluetoothEnabled

    init {
        observeRepository()
    }

    private fun observeRepository() {
        // Observe Bluetooth state
        viewModelScope.launch {
            repository.bluetoothState.collect { state ->
                updateUiStateFromBluetoothState(state)
            }
        }

        // Observe paired devices
        viewModelScope.launch {
            repository.pairedDevices.collect { devices ->
                (pairedDevices as MutableLiveData).value = devices
                updatePairedDevicesInUiState(devices)
            }
        }

        // Observe discovered devices
        viewModelScope.launch {
            repository.discoveredDevices.collect { devices ->
                (discoveredDevices as MutableLiveData).value = devices
                updateDiscoveredDevicesInUiState(devices)
            }
        }

        // Observe scanning state
        viewModelScope.launch {
            repository.isScanning.collect { scanning ->
                _isScanning.value = scanning
                updateScanningInUiState(scanning)
            }
        }

        // Observe error messages
        viewModelScope.launch {
            repository.errorMessage.collect { error ->
                _errorMessage.value = error
                updateErrorInUiState(error)
            }
        }
    }

    private fun updateUiStateFromBluetoothState(state: BluetoothState) {
        _bluetoothAvailable.value = when (state) {
            is BluetoothState.NotAvailable -> false
            else -> true
        }

        _bluetoothEnabled.value = when (state) {
            is BluetoothState.Disabled -> false
            is BluetoothState.NotAvailable -> false
            else -> true
        }

        _permissionRequired.value = state is BluetoothState.PermissionRequired

        _isConnecting.value = state is BluetoothState.Connecting

        val currentState = _uiState.value ?: BluetoothUiState()
        _uiState.value = currentState.copy(
            status = state.toUiStatus(),
            isScanning = state is BluetoothState.Scanning,
            connectedDevice = (state as? BluetoothState.Connected)?.device,
            errorMessage = (state as? BluetoothState.Error)?.message,
            permissionRequired = state is BluetoothState.PermissionRequired,
            isBluetoothAvailable = _bluetoothAvailable.value ?: true,
            isBluetoothEnabled = _bluetoothEnabled.value ?: false
        )
    }

    private fun updatePairedDevicesInUiState(devices: List<BluetoothDevice>) {
        val currentState = _uiState.value ?: BluetoothUiState()
        _uiState.value = currentState.copy(pairedDevices = devices)
    }

    private fun updateDiscoveredDevicesInUiState(devices: List<BluetoothDevice>) {
        val currentState = _uiState.value ?: BluetoothUiState()
        _uiState.value = currentState.copy(discoveredDevices = devices)
    }

    private fun updateScanningInUiState(scanning: Boolean) {
        val currentState = _uiState.value ?: BluetoothUiState()
        _uiState.value = currentState.copy(isScanning = scanning)
    }

    private fun updateErrorInUiState(error: String?) {
        val currentState = _uiState.value ?: BluetoothUiState()
        _uiState.value = currentState.copy(errorMessage = error)
    }

    // -------------------------------------------------------------------------
    // Public API for UI
    // -------------------------------------------------------------------------

    /**
     * Get required permissions for scanning.
     */
    fun getRequiredPermissions(): List<String> = repository.getScanPermissions()

    /**
     * Check if scan permissions are granted.
     */
    fun hasScanPermissions(): Boolean = repository.hasScanPermissions()

    /**
     * Check Bluetooth availability.
     */
    fun checkBluetoothState() {
        _bluetoothAvailable.value = repository.isBluetoothAvailable()
        _bluetoothEnabled.value = repository.isBluetoothEnabled()
    }

    /**
     * Enable Bluetooth.
     */
    fun enableBluetooth(): Boolean = repository.enableBluetooth()

    /**
     * Load paired devices.
     */
    fun loadPairedDevices() {
        if (!repository.hasScanPermissions()) {
            _permissionRequired.value = true
            return
        }
        _permissionRequired.value = false
        repository.loadPairedDevices()
    }

    /**
     * Start scanning for devices.
     */
    fun startScanning() {
        // Check Bluetooth availability
        checkBluetoothState()

        if (!repository.isBluetoothAvailable()) {
            _errorMessage.value = "Bluetooth is not available"
            return
        }

        if (!repository.isBluetoothEnabled()) {
            _errorMessage.value = "Please enable Bluetooth"
            return
        }

        if (!repository.hasScanPermissions()) {
            _permissionRequired.value = true
            return
        }

        _permissionRequired.value = false
        _errorMessage.value = null
        repository.startScan()
    }

    /**
     * Stop scanning.
     */
    fun stopScanning() {
        repository.stopScan()
    }

    /**
     * Connect to a device.
     */
    fun connectToDevice(device: BluetoothDevice) {
        try {
            repository.connect(device)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to connect: ${e.message}"
        }
    }

    /**
     * Disconnect from current device.
     */
    fun disconnect() {
        try {
            repository.disconnect()
        } catch (e: Exception) {
            _errorMessage.value = "Failed to disconnect: ${e.message}"
        }
    }

    /**
     * Unpair from a device.
     */
    fun unpairDevice(device: BluetoothDevice) {
        try {
            repository.unpair(device)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to unpair: ${e.message}"
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
        repository.clearError()
    }

    /**
     * Clear permission required flag.
     */
    fun clearPermissionRequired() {
        _permissionRequired.value = false
    }

    /**
     * Get connected device.
     */
    fun getConnectedDevice(): BluetoothDevice? {
        return (pairedDevices.value?.firstOrNull { it.isConnected }
            ?: discoveredDevices.value?.firstOrNull { it.isConnected })
    }

    /**
     * Check if currently scanning.
     */
    fun getIsScanning(): Boolean = _isScanning.value ?: false

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }

    /**
     * Factory for creating BluetoothViewModel.
     */
    class Factory(
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
                return BluetoothViewModel(BluetoothRepository(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}