package com.earshot.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles Bluetooth device discovery and scanning.
 *
 * This class is responsible for:
 * - Scanning for nearby BLE and Classic Bluetooth devices
 * - Getting paired devices from the system
 * - Managing scan lifecycle (start/stop/timeout)
 *
 * ## Usage
 *
 * ```kotlin
 * val scanner = BluetoothScanner(context)
 *
 * // Start scanning with callbacks
 * scanner.startScan(
 *     onDeviceFound = { device -> /* handle new device */ },
 *     onScanComplete = { /* scan finished */ }
 * )
 *
 * // Get paired devices
 * val pairedDevices = scanner.getPairedDevices()
 *
 * // Stop scanning
 * scanner.stopScan()
 * ```
 */
class BluetoothScanner(
    private val adapter: BluetoothAdapter
) {
    companion object {
        private const val TAG = "BluetoothScanner"
        private const val SCAN_DURATION_MS = 10_000L // 10 seconds
        private const val MIN_RSSI = -100
    }

    private val bleScanner: BluetoothLeScanner? = adapter.bluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())

    private var isScanning = false
    private var bleScanCallback: BleScanCallback? = null

    // State flows for observing scan status
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // Callback for device discovery
    private var onDeviceFound: ((BluetoothDevice) -> Unit)? = null
    private var onScanComplete: (() -> Unit)? = null

    /**
     * Start scanning for nearby Bluetooth devices.
     * Scans for both BLE and Classic Bluetooth devices.
     *
     * @param onDeviceFound Callback when a new device is found
     * @param onScanComplete Callback when scanning stops
     * @param durationMs Scan duration in milliseconds (default: 10 seconds)
     */
    @SuppressLint("MissingPermission")
    fun startScan(
        onDeviceFound: (BluetoothDevice) -> Unit,
        onScanComplete: () -> Unit,
        durationMs: Long = SCAN_DURATION_MS
    ) {
        if (isScanning) {
            Log.w(TAG, "Already scanning")
            return
        }

        this.onDeviceFound = onDeviceFound
        this.onScanComplete = onScanComplete

        isScanning = true
        _scanState.value = ScanState.Scanning

        // Start BLE scanning
        startBleScan()

        // Start Classic Bluetooth discovery
        startClassicDiscovery()

        // Schedule automatic stop
        handler.postDelayed({
            stopScan()
        }, durationMs)

        Log.d(TAG, "Started scanning for $durationMs ms")
    }

    /**
     * Stop scanning for devices.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) {
            return
        }

        isScanning = false
        _scanState.value = ScanState.Idle

        // Stop BLE scanning
        stopBleScan()

        // Cancel Classic Bluetooth discovery
        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }

        // Remove pending callbacks
        handler.removeCallbacksAndMessages(null)

        onScanComplete?.invoke()
        onScanComplete = null

        Log.d(TAG, "Stopped scanning")
    }

    /**
     * Check if currently scanning.
     */
    fun isScanning(): Boolean = isScanning

    /**
     * Get all paired Bluetooth devices.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter.bondedDevices?.map { it.toOurDevice() } ?: emptyList()
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        stopScan()
        onDeviceFound = null
        onScanComplete = null
    }

    // -------------------------------------------------------------------------
    // Private Methods
    // -------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        val scanner = bleScanner ?: return

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        bleScanCallback = BleScanCallback { result ->
            result.device.toOurDevice(result.rssi).also { device ->
                onDeviceFound?.invoke(device)
            }
        }

        try {
            scanner.startScan(null, scanSettings, bleScanCallback!!)
            Log.d(TAG, "BLE scan started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scan", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        bleScanCallback?.let { callback ->
            try {
                bleScanner?.stopScan(callback)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping BLE scan", e)
            }
            bleScanCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun startClassicDiscovery() {
        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }

        try {
            val started = adapter.startDiscovery()
            Log.d(TAG, "Classic discovery started: $started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start classic discovery", e)
        }
    }

    /**
     * Extension function to convert Android BluetoothDevice to our model.
     */
    @SuppressLint("MissingPermission")
    private fun AndroidBluetoothDevice.toOurDevice(rssiValue: Int = 0): BluetoothDevice {
        val name = this.name ?: "Unknown Device"
        val address = this.address

        return BluetoothDevice(
            name = name,
            address = address,
            isPaired = this.bondState == AndroidBluetoothDevice.BOND_BONDED,
            isConnected = false, // Will be checked separately if needed
            rssi = rssiValue,
            deviceType = when (this.type) {
                AndroidBluetoothDevice.DEVICE_TYPE_CLASSIC -> DeviceType.CLASSIC
                AndroidBluetoothDevice.DEVICE_TYPE_LE -> DeviceType.BLE
                AndroidBluetoothDevice.DEVICE_TYPE_DUAL -> DeviceType.DUAL
                else -> DeviceType.UNKNOWN
            }
        )
    }

    /**
     * BLE Scan callback implementation.
     */
    private class BleScanCallback(
        private val onDeviceFound: (ScanResult) -> Unit
    ) : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Filter very weak signals
            if (result.rssi >= MIN_RSSI) {
                onDeviceFound(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed with error code: $errorCode")
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            results.filter { it.rssi >= MIN_RSSI }.forEach { result ->
                onDeviceFound(result)
            }
        }
    }
}

/**
 * Sealed class representing scan state.
 */
sealed class ScanState {
    data object Idle : ScanState()
    data object Scanning : ScanState()
    data class Error(val message: String) : ScanState()
}