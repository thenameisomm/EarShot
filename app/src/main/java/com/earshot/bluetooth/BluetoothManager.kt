package com.earshot.bluetooth

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main Bluetooth manager that coordinates all Bluetooth operations.
 *
 * This class provides a unified API for:
 * - Monitoring Bluetooth state (on/off)
 * - Scanning for devices
 * - Connecting to devices
 * - Managing device list
 *
 * ## Architecture
 *
 * ```
 * BluetoothManager
 * ├── BluetoothStateMonitor (adapter state)
 * ├── BluetoothPermissionManager (permissions)
 * ├── BluetoothScanner (device discovery)
 * └── BluetoothConnectionManager (connections)
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * val bluetoothManager = BluetoothManager(context)
 *
 * // Check if ready
 * if (bluetoothManager.isReady()) {
 *     bluetoothManager.startScan()
 * }
 *
 * // Observe all devices
 * bluetoothManager.devices.collect { deviceList ->
 *     // Update UI
 * }
 *
 * // Connect
 * bluetoothManager.connect(device)
 * ```
 */
class BluetoothManager(context: Context) {
    companion object {
        private const val TAG = "BluetoothManager"

        // Singleton instance
        @Volatile
        private var instance: BluetoothManager? = null

        fun getInstance(context: Context): BluetoothManager {
            return instance ?: synchronized(this) {
                instance ?: BluetoothManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Dependencies (all scoped to this manager)
    private val stateMonitor = BluetoothStateMonitor(context)
    private val permissionManager = BluetoothPermissionManager(context)

    // Get adapter from state monitor
    private val adapter = stateMonitor.retrieveAdapter()

    // Scanner and connection manager
    private val scanner = adapter?.let { BluetoothScanner(it) }
    private val connectionManager = adapter?.let { BluetoothConnectionManager(it) }

    // Unified device list combining paired and discovered
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    // Paired devices only
    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    // Discovered (nearby) devices only
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    // Currently scanning
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Combined Bluetooth state for UI
    private val _bluetoothState = MutableStateFlow<BluetoothState>(BluetoothState.Disconnected)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Internal device tracking
    private val devicesMap = mutableMapOf<String, BluetoothDevice>()

    init {
        // Initialize state monitoring
        stateMonitor.start { state ->
            handleAdapterStateChange(state)
        }

        // Initialize connection state monitoring
        connectionManager?.setOnConnectionStateChanged { connectionState ->
            handleConnectionStateChange(connectionState)
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Check if Bluetooth is ready to use (available, enabled, permissions granted).
     */
    fun isReady(): Boolean {
        return stateMonitor.isBluetoothAvailable() &&
                stateMonitor.isBluetoothEnabled() &&
                permissionManager.hasScanPermissions()
    }

    /**
     * Get current Bluetooth availability and state.
     */
    fun getBluetoothState(): BluetoothState {
        return when {
            !stateMonitor.isBluetoothAvailable() -> BluetoothState.NotAvailable
            !stateMonitor.isBluetoothEnabled() -> BluetoothState.Disabled
            !permissionManager.hasScanPermissions() -> BluetoothState.PermissionRequired
            _isScanning.value -> BluetoothState.Scanning
            connectionManager?.isConnected() == true -> {
                val device = connectionManager.connectedDevice.value
                if (device != null) BluetoothState.Connected(device)
                else BluetoothState.Ready
            }
            else -> BluetoothState.Ready
        }
    }

    /**
     * Check if Bluetooth is available.
     */
    fun isBluetoothAvailable(): Boolean = stateMonitor.isBluetoothAvailable()

    /**
     * Check if Bluetooth is enabled.
     */
    fun isBluetoothEnabled(): Boolean = stateMonitor.isBluetoothEnabled()

    /**
     * Request to enable Bluetooth.
     */
    fun enableBluetooth(): Boolean = stateMonitor.enableBluetooth()

    /**
     * Get required scan permissions.
     */
    fun getScanPermissions(): List<String> = permissionManager.getScanPermissions()

    /**
     * Check if scan permissions are granted.
     */
    fun hasScanPermissions(): Boolean = permissionManager.hasScanPermissions()

    /**
     * Load paired devices from the system.
     */
    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        if (!permissionManager.hasScanPermissions()) {
            _errorMessage.value = "Bluetooth permission required"
            return
        }

        val paired = scanner?.getPairedDevices() ?: emptyList()
        _pairedDevices.value = paired

        // Update combined devices list
        paired.forEach { device ->
            devicesMap[device.address] = device
        }
        _devices.value = devicesMap.values.toList().sortedBy { it.name.lowercase() }
    }

    /**
     * Start scanning for nearby Bluetooth devices.
     */
    fun startScan() {
        // Check prerequisites
        if (!stateMonitor.isBluetoothAvailable()) {
            _errorMessage.value = "Bluetooth is not available"
            _bluetoothState.value = BluetoothState.Error("Bluetooth not available")
            return
        }

        if (!stateMonitor.isBluetoothEnabled()) {
            _errorMessage.value = "Please enable Bluetooth"
            _bluetoothState.value = BluetoothState.Disabled
            return
        }

        if (!permissionManager.hasScanPermissions()) {
            _errorMessage.value = "Bluetooth permission required"
            _bluetoothState.value = BluetoothState.PermissionRequired
            return
        }

        _errorMessage.value = null
        _isScanning.value = true
        _bluetoothState.value = BluetoothState.Scanning

        // Start with paired devices
        val paired = scanner?.getPairedDevices() ?: emptyList()
        paired.forEach { device ->
            devicesMap[device.address] = device
        }
        _pairedDevices.value = paired

        // Start scanning
        scanner?.startScan(
            onDeviceFound = { device ->
                addDiscoveredDevice(device)
            },
            onScanComplete = {
                _isScanning.value = false
                val isConnected = connectionManager?.isConnected() == true
                val device = connectionManager?.connectedDevice?.value
                _bluetoothState.value = if (isConnected && device != null) {
                    BluetoothState.Connected(device)
                } else {
                    BluetoothState.Ready
                }
                Log.d(TAG, "Scan complete. Found ${devicesMap.size} devices")
            }
        )
    }

    /**
     * Stop scanning for devices.
     */
    fun stopScan() {
        scanner?.stopScan()
        _isScanning.value = false
        val isConnected = connectionManager?.isConnected() == true
        val device = connectionManager?.connectedDevice?.value
        _bluetoothState.value = if (isConnected && device != null) {
            BluetoothState.Connected(device)
        } else {
            BluetoothState.Ready
        }
    }

    /**
     * Connect to a Bluetooth device.
     */
    fun connect(device: BluetoothDevice) {
        if (!permissionManager.hasConnectPermissions()) {
            _errorMessage.value = "Bluetooth connect permission required"
            return
        }

        _bluetoothState.value = BluetoothState.Connecting(device)

        connectionManager?.connect(device) { success, error ->
            if (success) {
                updateDeviceConnectionState(device.address, true)
                _bluetoothState.value = BluetoothState.Connected(device)
                _errorMessage.value = null
            } else {
                updateDeviceConnectionState(device.address, false)
                _bluetoothState.value = BluetoothState.Error(error ?: "Connection failed")
                _errorMessage.value = error
            }
        }
    }

    /**
     * Disconnect from the current device.
     */
    fun disconnect() {
        connectionManager?.disconnect(removeBond = false) { _ ->
            val disconnectedAddress = connectionManager.connectedDevice.value?.address
            if (disconnectedAddress != null) {
                updateDeviceConnectionState(disconnectedAddress, false)
            }
            _bluetoothState.value = BluetoothState.Ready
        }
    }

    /**
     * Unpair and disconnect from a device.
     */
    fun unpair(device: BluetoothDevice) {
        connectionManager?.disconnect(removeBond = true) { _ ->
            // Remove from our list
            devicesMap.remove(device.address)
            _devices.value = devicesMap.values.toList()
            _pairedDevices.value = _pairedDevices.value.filter { it.address != device.address }
            _discoveredDevices.value = _discoveredDevices.value.filter { it.address != device.address }
            _bluetoothState.value = BluetoothState.Ready
        }
    }

    /**
     * Get the currently connected device.
     */
    fun getConnectedDevice(): BluetoothDevice? = connectionManager?.connectedDevice?.value

    /**
     * Get current connection state.
     */
    fun getConnectionState(): ConnectionState = connectionManager?.connectionState?.value
        ?: ConnectionState.Disconnected

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clean up all resources.
     */
    fun cleanup() {
        stopScan()
        scanner?.cleanup()
        connectionManager?.cleanup()
        stateMonitor.cleanup()
    }

    // -------------------------------------------------------------------------
    // Private Methods
    // -------------------------------------------------------------------------

    private fun addDiscoveredDevice(device: BluetoothDevice) {
        val existingDevice = devicesMap[device.address]

        // Update with better signal strength if available
        val updatedDevice = if (existingDevice != null && device.rssi > existingDevice.rssi) {
            device.copy(isPaired = existingDevice.isPaired, isConnected = existingDevice.isConnected)
        } else {
            device
        }

        devicesMap[device.address] = updatedDevice
        _devices.value = devicesMap.values.toList().sortedBy { it.name.lowercase() }

        // Update discovered devices list (not paired)
        _discoveredDevices.value = devicesMap.values
            .filter { !it.isPaired }
            .toList()
            .sortedBy { it.name.lowercase() }

        // Update paired devices list
        _pairedDevices.value = devicesMap.values
            .filter { it.isPaired }
            .toList()
            .sortedBy { it.name.lowercase() }
    }

    private fun updateDeviceConnectionState(address: String, isConnected: Boolean) {
        devicesMap[address]?.let { device ->
            devicesMap[address] = device.copy(isConnected = isConnected)
            _devices.value = devicesMap.values.toList()
        }
    }

    private fun handleAdapterStateChange(state: BluetoothAdapterState) {
        when (state) {
            BluetoothAdapterState.Unavailable -> {
                _bluetoothState.value = BluetoothState.NotAvailable
            }
            BluetoothAdapterState.Off -> {
                _bluetoothState.value = BluetoothState.Disabled
                _devices.value = emptyList()
                _pairedDevices.value = emptyList()
                _discoveredDevices.value = emptyList()
            }
            BluetoothAdapterState.On -> {
                if (_isScanning.value) {
                    _bluetoothState.value = BluetoothState.Scanning
                } else if (connectionManager?.isConnected() == true) {
                    val device = connectionManager.connectedDevice.value
                    if (device != null) {
                        _bluetoothState.value = BluetoothState.Connected(device)
                    } else {
                        _bluetoothState.value = BluetoothState.Ready
                    }
                } else {
                    _bluetoothState.value = BluetoothState.Ready
                }
                // Load paired devices when Bluetooth turns on
                if (permissionManager.hasScanPermissions()) {
                    loadPairedDevices()
                }
            }
            else -> { /* Ignore transitional states */ }
        }
    }

    private fun handleConnectionStateChange(state: ConnectionState) {
        when (state) {
            is ConnectionState.Connected -> {
                val device = connectionManager?.connectedDevice?.value
                if (device != null) {
                    _bluetoothState.value = BluetoothState.Connected(device)
                }
            }
            ConnectionState.Connecting -> {
                // Already handled in connect()
            }
            ConnectionState.Disconnected -> {
                _bluetoothState.value = if (_isScanning.value) {
                    BluetoothState.Scanning
                } else {
                    BluetoothState.Ready
                }
            }
            is ConnectionState.Error -> {
                _bluetoothState.value = BluetoothState.Error(state.message)
                _errorMessage.value = state.message
            }
        }
    }
}