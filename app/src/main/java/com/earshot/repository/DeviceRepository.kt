package com.earshot.repository

import android.content.Context
import com.earshot.bluetooth.BluetoothDevice
import com.earshot.bluetooth.BluetoothRepository
import com.earshot.bluetooth.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository for managing Bluetooth devices.
 *
 * @deprecated Use [com.earshot.bluetooth.BluetoothRepository] instead.
 * This class is kept for backward compatibility and delegates to the new implementation.
 */
@Deprecated(
    message = "Use com.earshot.bluetooth.BluetoothRepository instead",
    replaceWith = ReplaceWith(
        "com.earshot.bluetooth.BluetoothRepository",
        "com.earshot.bluetooth.BluetoothRepository"
    )
)
class DeviceRepository(context: Context) {

    companion object {
        private const val TAG = "DeviceRepository"
    }

    // Delegate to new implementation
    private val bluetoothRepository = BluetoothRepository(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // All devices (combined list)
    val devices: StateFlow<List<BluetoothDevice>> = bluetoothRepository.devices

    // Separated device lists
    val pairedDevices: StateFlow<List<BluetoothDevice>> = bluetoothRepository.pairedDevices

    val discoveredDevices: StateFlow<List<BluetoothDevice>> = bluetoothRepository.discoveredDevices

    // Scanning state
    val isScanning: StateFlow<Boolean> = bluetoothRepository.isScanning

    // Connected device
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Observe Bluetooth state and sync - using collect instead of collectLatest
        scope.launch {
            bluetoothRepository.bluetoothState.collect { state ->
                val newConnectionState: ConnectionState = when (state) {
                    is com.earshot.bluetooth.BluetoothState.Connected -> ConnectionState.Connected
                    is com.earshot.bluetooth.BluetoothState.Connecting -> ConnectionState.Connecting
                    is com.earshot.bluetooth.BluetoothState.Disconnected -> ConnectionState.Disconnected
                    is com.earshot.bluetooth.BluetoothState.Error -> ConnectionState.Error(state.message)
                    else -> ConnectionState.Disconnected
                }
                _connectionState.value = newConnectionState

                // Also update connected device
                _connectedDevice.value = (state as? com.earshot.bluetooth.BluetoothState.Connected)?.device
            }
        }
    }

    /**
     * Check if Bluetooth is available on this device.
     */
    fun isBluetoothAvailable(): Boolean = bluetoothRepository.isBluetoothAvailable()

    /**
     * Check if Bluetooth is enabled.
     */
    fun isBluetoothEnabled(): Boolean = bluetoothRepository.isBluetoothEnabled()

    /**
     * Request to enable Bluetooth.
     */
    fun enableBluetooth(): Boolean = bluetoothRepository.enableBluetooth()

    /**
     * Check if we have permission to scan.
     */
    fun hasScanPermissions(): Boolean = bluetoothRepository.hasScanPermissions()

    /**
     * Get required scan permissions.
     */
    fun getScanPermissions(): List<String> = bluetoothRepository.getScanPermissions()

    /**
     * Load cached/paired devices from the system.
     */
    fun loadDevices() {
        bluetoothRepository.loadPairedDevices()
    }

    /**
     * Start scanning for nearby Bluetooth devices.
     */
    fun startScanning() {
        bluetoothRepository.startScan()
    }

    /**
     * Start continuous scanning.
     */
    fun startContinuousScanning() {
        bluetoothRepository.startScan()
    }

    /**
     * Stop scanning for devices.
     */
    fun stopScanning() {
        bluetoothRepository.stopScan()
    }

    /**
     * Connect to a specific device.
     */
    fun connectToDevice(device: BluetoothDevice) {
        bluetoothRepository.connect(device)
    }

    /**
     * Disconnect from the current device.
     */
    fun disconnectDevice() {
        bluetoothRepository.disconnect()
    }

    /**
     * Pair with a device.
     */
    fun pairDevice(device: BluetoothDevice) {
        bluetoothRepository.connect(device)
    }

    /**
     * Unpair from a device.
     */
    fun unpairDevice(device: BluetoothDevice) {
        bluetoothRepository.unpair(device)
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        bluetoothRepository.clearError()
    }

    /**
     * Get current connection state.
     */
    fun getConnectionState(): ConnectionState = _connectionState.value

    /**
     * Check if currently connecting.
     */
    fun isConnecting(): Boolean = _connectionState.value is ConnectionState.Connecting

    /**
     * Clean up resources.
     */
    fun cleanup() {
        bluetoothRepository.cleanup()
    }
}