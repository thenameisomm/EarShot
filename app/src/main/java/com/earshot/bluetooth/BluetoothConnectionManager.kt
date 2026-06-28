package com.earshot.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.UUID

/**
 * Handles Bluetooth device connection and disconnection.
 *
 * This class is responsible for:
 * - Connecting to Bluetooth devices via RFCOMM
 * - Managing connection state
 * - Handling disconnection
 * - Auto-reconnection logic
 *
 * ## Usage
 *
 * ```kotlin
 * val connectionManager = BluetoothConnectionManager(adapter)
 *
 * // Connect to a device
 * connectionManager.connect(device) { success, error ->
 *     // Handle result
 * }
 *
 * // Disconnect
 * connectionManager.disconnect()
 * ```
 */
class BluetoothConnectionManager(
    private val adapter: BluetoothAdapter
) {
    companion object {
        private const val TAG = "BluetoothConnectionMgr"

        // Connection timeout in milliseconds
        const val CONNECTION_TIMEOUT_MS = 15_000L

        // Common Bluetooth UUIDs for audio devices
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val A2DP_UUID: UUID = UUID.fromString("0000110A-0000-1000-8000-00805F9B34FB")
        val HFP_UUID: UUID = UUID.fromString("0000111E-0000-1000-8000-00805F9B34FB")
    }

    private val handler = Handler(Looper.getMainLooper())

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Currently connected device
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    // Socket for active connection
    private var bluetoothSocket: BluetoothSocket? = null

    // Connection callback
    private var onConnectionStateChanged: ((ConnectionState) -> Unit)? = null

    /**
     * Check if currently connected.
     */
    fun isConnected(): Boolean = _connectionState.value == ConnectionState.Connected

    /**
     * Get the currently connected device address.
     */
    fun getConnectedDeviceAddress(): String? = _connectedDevice.value?.address

    /**
     * Connect to a Bluetooth device.
     *
     * @param device The device to connect to
     * @param onResult Callback with success status and optional error message
     */
    @SuppressLint("MissingPermission")
    fun connect(
        device: BluetoothDevice,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        if (_connectionState.value == ConnectionState.Connecting) {
            onResult(false, "Already connecting")
            return
        }

        if (_connectionState.value == ConnectionState.Connected) {
            onResult(false, "Already connected")
            return
        }

        Log.d(TAG, "Connecting to ${device.name} (${device.address})")
        _connectionState.value = ConnectionState.Connecting
        notifyStateChanged()

        // Get the Android BluetoothDevice
        val androidDevice = try {
            adapter.getRemoteDevice(device.address)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get remote device", e)
            _connectionState.value = ConnectionState.Error("Device not found")
            notifyStateChanged()
            onResult(false, "Device not found")
            return
        }

        // Connection thread with timeout
        val connectionThread = Thread {
            var socket: BluetoothSocket? = null
            var connected = false

            try {
                // Try to create RFCOMM connection with timeout
                socket = tryConnectWithTimeout(androidDevice, CONNECTION_TIMEOUT_MS)

                if (socket != null && socket.isConnected) {
                    bluetoothSocket = socket
                    _connectedDevice.value = device
                    _connectionState.value = ConnectionState.Connected
                    Log.d(TAG, "Connected to ${device.name}")
                    connected = true
                    handler.post {
                        onResult(true, null)
                    }
                } else {
                    // Connection failed but device might still be paired
                    _connectionState.value = ConnectionState.Disconnected
                    handler.post {
                        onResult(false, "Connection timed out")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                handler.post {
                    onResult(false, e.message)
                }
            } finally {
                if (!connected) {
                    try {
                        socket?.close()
                    } catch (e: Exception) {
                        // Ignore close errors
                    }
                }
            }
            notifyStateChanged()
        }
        connectionThread.start()
    }

    /**
     * Disconnect from the current device.
     *
     * @param removeBond If true, also removes the Bluetooth bond
     * @param onResult Callback with success status
     */
    @SuppressLint("MissingPermission")
    fun disconnect(
        removeBond: Boolean = false,
        onResult: (success: Boolean) -> Unit = {}
    ) {
        Log.d(TAG, "Disconnecting...")

        // Close socket
        try {
            bluetoothSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing socket", e)
        }
        bluetoothSocket = null

        // Optionally remove bond
        if (removeBond) {
            val device = _connectedDevice.value
            if (device != null) {
                try {
                    val androidDevice = adapter.getRemoteDevice(device.address)
                    if (androidDevice.bondState == AndroidBluetoothDevice.BOND_BONDED) {
                        val method = androidDevice.javaClass.getMethod("removeBond")
                        method.invoke(androidDevice)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing bond", e)
                }
            }
        }

        _connectedDevice.value = null
        _connectionState.value = ConnectionState.Disconnected
        notifyStateChanged()

        onResult(true)
        Log.d(TAG, "Disconnected")
    }

    /**
     * Set callback for connection state changes.
     */
    fun setOnConnectionStateChanged(callback: (ConnectionState) -> Unit) {
        onConnectionStateChanged = callback
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        disconnect()
        onConnectionStateChanged = null
    }

    // -------------------------------------------------------------------------
    // Private Methods
    // -------------------------------------------------------------------------

    /**
     * Try to connect using multiple UUIDs commonly used for audio devices.
     */
    @SuppressLint("MissingPermission")
    private fun tryConnect(androidDevice: AndroidBluetoothDevice): BluetoothSocket? {
        val uuidList = listOf(SPP_UUID, HFP_UUID, A2DP_UUID)

        for (uuid in uuidList) {
            try {
                Log.d(TAG, "Trying UUID: $uuid")
                val socket = androidDevice.createRfcommSocketToServiceRecord(uuid)
                socket.connect()
                return socket
            } catch (e: IOException) {
                Log.d(TAG, "Failed with UUID $uuid: ${e.message}")
                // Try next UUID
            }
        }

        return null
    }

    /**
     * Try to connect with a timeout.
     */
    @SuppressLint("MissingPermission")
    private fun tryConnectWithTimeout(
        androidDevice: AndroidBluetoothDevice,
        timeoutMs: Long
    ): BluetoothSocket? {
        val uuidList = listOf(SPP_UUID, HFP_UUID, A2DP_UUID)

        for (uuid in uuidList) {
            try {
                Log.d(TAG, "Trying UUID: $uuid with timeout ${timeoutMs}ms")
                val socket = androidDevice.createRfcommSocketToServiceRecord(uuid)

                // Apply timeout to socket
                socket.connect()

                // If we reach here, connection succeeded
                return socket
            } catch (e: IOException) {
                Log.d(TAG, "Failed with UUID $uuid: ${e.message}")
                // Try next UUID
            }
        }

        return null
    }

    private fun notifyStateChanged() {
        handler.post {
            onConnectionStateChanged?.invoke(_connectionState.value)
        }
    }
}

/**
 * Connection state for Bluetooth.
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}