package com.earshot.bluetooth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for Bluetooth operations.
 *
 * This class provides a clean API for the ViewModel layer, abstracting away
 * the complexity of the Bluetooth system. It manages a single BluetoothManager
 * instance and exposes StateFlows for UI observation.
 *
 * ## Usage
 *
 * ```kotlin
 * val repository = BluetoothRepository(context)
 *
 * // Observe devices
 * repository.devices.collect { devices ->
 *     // Update UI
 * }
 *
 * // Start scanning
 * repository.startScan()
 * ```
 */
class BluetoothRepository(context: Context) {
    companion object {
        private const val TAG = "BluetoothRepository"
    }

    // Bluetooth manager (singleton pattern)
    private val bluetoothManager = BluetoothManager.getInstance(context)

    // Unified device list
    val devices: StateFlow<List<BluetoothDevice>> = bluetoothManager.devices

    // Paired devices
    val pairedDevices: StateFlow<List<BluetoothDevice>> = bluetoothManager.pairedDevices

    // Discovered (nearby) devices
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = bluetoothManager.discoveredDevices

    // Scanning state
    val isScanning: StateFlow<Boolean> = bluetoothManager.isScanning

    // Overall Bluetooth state
    val bluetoothState: StateFlow<BluetoothState> = bluetoothManager.bluetoothState

    // Error messages
    val errorMessage: StateFlow<String?> = bluetoothManager.errorMessage

    /**
     * Check if Bluetooth is ready to use.
     */
    fun isReady(): Boolean = bluetoothManager.isReady()

    /**
     * Check if Bluetooth is available.
     */
    fun isBluetoothAvailable(): Boolean = bluetoothManager.isBluetoothAvailable()

    /**
     * Check if Bluetooth is enabled.
     */
    fun isBluetoothEnabled(): Boolean = bluetoothManager.isBluetoothEnabled()

    /**
     * Request to enable Bluetooth.
     */
    fun enableBluetooth(): Boolean = bluetoothManager.enableBluetooth()

    /**
     * Get required scan permissions.
     */
    fun getScanPermissions(): List<String> = bluetoothManager.getScanPermissions()

    /**
     * Check if scan permissions are granted.
     */
    fun hasScanPermissions(): Boolean = bluetoothManager.hasScanPermissions()

    /**
     * Load paired devices from the system.
     */
    fun loadPairedDevices() {
        bluetoothManager.loadPairedDevices()
    }

    /**
     * Start scanning for nearby devices.
     */
    fun startScan() {
        bluetoothManager.startScan()
    }

    /**
     * Stop scanning.
     */
    fun stopScan() {
        bluetoothManager.stopScan()
    }

    /**
     * Connect to a device.
     */
    fun connect(device: BluetoothDevice) {
        bluetoothManager.connect(device)
    }

    /**
     * Disconnect from current device.
     */
    fun disconnect() {
        bluetoothManager.disconnect()
    }

    /**
     * Unpair from a device.
     */
    fun unpair(device: BluetoothDevice) {
        bluetoothManager.unpair(device)
    }

    /**
     * Get the currently connected device.
     */
    fun getConnectedDevice(): BluetoothDevice? = bluetoothManager.getConnectedDevice()

    /**
     * Clear error message.
     */
    fun clearError() {
        bluetoothManager.clearError()
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        bluetoothManager.cleanup()
    }
}