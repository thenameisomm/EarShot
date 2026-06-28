package com.earshot.bluetooth

/**
 * UI State for the Bluetooth screen.
 *
 * This class consolidates all the state needed by the UI layer,
 * making it easy to observe and update the UI reactively.
 *
 * ## Usage
 *
 * ```kotlin
 * val uiState = BluetoothUiState()
 *
 * // Check state
 * when (uiState.status) {
 *     is UiStatus.Scanning -> showProgress()
 *     is UiStatus.Connected -> showDevice(uiState.connectedDevice)
 *     // ...
 * }
 * ```
 */
data class BluetoothUiState(
    // Overall status
    val status: UiStatus = UiStatus.Disconnected,

    // Device lists
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val discoveredDevices: List<BluetoothDevice> = emptyList(),

    // Connected device
    val connectedDevice: BluetoothDevice? = null,

    // Error state
    val errorMessage: String? = null,

    // Permission state
    val permissionRequired: Boolean = false,

    // Scanning state
    val isScanning: Boolean = false,

    // Bluetooth hardware state
    val isBluetoothAvailable: Boolean = true,
    val isBluetoothEnabled: Boolean = false
)

/**
 * UI status states.
 */
sealed class UiStatus {
    /** Initial state */
    data object Idle : UiStatus()

    /** Bluetooth is not available on this device */
    data object NotAvailable : UiStatus()

    /** Bluetooth is disabled */
    data object Disabled : UiStatus()

    /** Permission required */
    data object PermissionRequired : UiStatus()

    /** Ready to scan */
    data object Ready : UiStatus()

    /** Currently scanning */
    data object Scanning : UiStatus()

    /** Currently connecting */
    data class Connecting(val device: BluetoothDevice) : UiStatus()

    /** Connected to a device */
    data class Connected(val device: BluetoothDevice) : UiStatus()

    /** Disconnected */
    data object Disconnected : UiStatus()

    /** Error occurred */
    data class Error(val message: String) : UiStatus()
}

/**
 * Extension function to convert BluetoothState to UiStatus.
 */
fun BluetoothState.toUiStatus(): UiStatus {
    return when (this) {
        is BluetoothState.NotAvailable -> UiStatus.NotAvailable
        is BluetoothState.Disabled -> UiStatus.Disabled
        is BluetoothState.PermissionRequired -> UiStatus.PermissionRequired
        is BluetoothState.Ready -> UiStatus.Ready
        is BluetoothState.Scanning -> UiStatus.Scanning
        is BluetoothState.Connecting -> UiStatus.Connecting(this.device)
        is BluetoothState.Connected -> UiStatus.Connected(this.device)
        is BluetoothState.Disconnected -> UiStatus.Disconnected
        is BluetoothState.Error -> UiStatus.Error(this.message)
    }
}

/**
 * Extension function to convert UiStatus to BluetoothState.
 */
fun UiStatus.toBluetoothState(): BluetoothState {
    return when (this) {
        is UiStatus.Idle -> BluetoothState.Disconnected
        is UiStatus.NotAvailable -> BluetoothState.NotAvailable
        is UiStatus.Disabled -> BluetoothState.Disabled
        is UiStatus.PermissionRequired -> BluetoothState.PermissionRequired
        is UiStatus.Ready -> BluetoothState.Ready
        is UiStatus.Scanning -> BluetoothState.Scanning
        is UiStatus.Connecting -> BluetoothState.Connecting(this.device)
        is UiStatus.Connected -> BluetoothState.Connected(this.device)
        is UiStatus.Disconnected -> BluetoothState.Disconnected
        is UiStatus.Error -> BluetoothState.Error(this.message)
    }
}