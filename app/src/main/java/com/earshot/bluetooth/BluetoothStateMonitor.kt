package com.earshot.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors Bluetooth adapter state (ON/OFF/Permission).
 *
 * This class is responsible for:
 * - Detecting Bluetooth enable/disable state
 * - Broadcasting state changes to the app
 * - Providing current Bluetooth availability
 *
 * ## Usage
 *
 * ```kotlin
 * val stateMonitor = BluetoothStateMonitor(context)
 *
 * // Start monitoring
 * stateMonitor.start { state ->
 *     // Handle state change
 * }
 *
 * // Check current state
 * val currentState = stateMonitor.getCurrentState(adapter, permissionManager)
 *
 * // Stop monitoring
 * stateMonitor.stop()
 * ```
 */
class BluetoothStateMonitor(private val context: Context) {
    companion object {
        private const val TAG = "BluetoothStateMonitor"
    }

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val adapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    // Current state
    private val _state = MutableStateFlow<BluetoothAdapterState>(BluetoothAdapterState.Unknown)
    val state: StateFlow<BluetoothAdapterState> = _state.asStateFlow()

    // Callback for state changes
    private var onStateChanged: ((BluetoothAdapterState) -> Unit)? = null

    // Broadcast receiver for Bluetooth state changes
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    val adapterState = when (state) {
                        BluetoothAdapter.STATE_OFF -> BluetoothAdapterState.Off
                        BluetoothAdapter.STATE_TURNING_ON -> BluetoothAdapterState.TurningOn
                        BluetoothAdapter.STATE_ON -> BluetoothAdapterState.On
                        BluetoothAdapter.STATE_TURNING_OFF -> BluetoothAdapterState.TurningOff
                        else -> BluetoothAdapterState.Unknown
                    }
                    updateState(adapterState)
                }
            }
        }
    }

    /**
     * Start monitoring Bluetooth state changes.
     *
     * @param onStateChanged Callback when Bluetooth state changes
     */
    fun start(onStateChanged: (BluetoothAdapterState) -> Unit) {
        this.onStateChanged = onStateChanged

        // Register receiver
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        // Get initial state
        updateState(getCurrentAdapterState())
    }

    /**
     * Stop monitoring Bluetooth state changes.
     */
    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver", e)
        }
        onStateChanged = null
    }

    /**
     * Check if Bluetooth is available on this device.
     */
    fun isBluetoothAvailable(): Boolean = adapter != null

    /**
     * Check if Bluetooth is enabled.
     */
    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    /**
     * Check if Bluetooth is currently enabling.
     */
    fun isBluetoothTurningOn(): Boolean = _state.value == BluetoothAdapterState.TurningOn

    /**
     * Request to enable Bluetooth.
     * Note: This may not work on Android 13+ without user interaction.
     *
     * @return true if the request was made
     */
    @SuppressLint("MissingPermission")
    fun enableBluetooth(): Boolean {
        return try {
            // Use the non-deprecated method: startActivity with ACTION_REQUEST_ENABLE
            // Return true to indicate the intent was sent (the actual result comes via broadcast)
            adapter?.let {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(enableIntent)
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable Bluetooth", e)
            false
        }
    }

    /**
     * Get current adapter state.
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentAdapterState(): BluetoothAdapterState {
        return when {
            adapter == null -> BluetoothAdapterState.Unavailable
            !adapter!!.isEnabled -> BluetoothAdapterState.Off
            else -> BluetoothAdapterState.On
        }
    }

    /**
     * Get BluetoothAdapter instance.
     */
    fun retrieveAdapter(): BluetoothAdapter? = adapter

    /**
     * Clean up resources.
     */
    fun cleanup() {
        stop()
    }

    private fun updateState(newState: BluetoothAdapterState) {
        if (_state.value != newState) {
            _state.value = newState
            Log.d(TAG, "Bluetooth state: $newState")
            onStateChanged?.invoke(newState)
        }
    }
}

/**
 * Bluetooth adapter state.
 */
sealed class BluetoothAdapterState {
    data object Unavailable : BluetoothAdapterState()  // No Bluetooth
    data object Off : BluetoothAdapterState()           // Bluetooth is off
    data object TurningOn : BluetoothAdapterState()     // Bluetooth is turning on
    data object On : BluetoothAdapterState()           // Bluetooth is on
    data object TurningOff : BluetoothAdapterState()  // Bluetooth is turning off
    data object Unknown : BluetoothAdapterState()      // Unknown state
}