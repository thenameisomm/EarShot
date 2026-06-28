package com.earshot.bluetooth

/**
 * Sealed class representing the overall Bluetooth state.
 * Used for UI representation and state management.
 */
sealed class BluetoothState {
    /** Bluetooth is not available on this device */
    data object NotAvailable : BluetoothState()

    /** Bluetooth is available but turned off */
    data object Disabled : BluetoothState()

    /** Bluetooth is enabled but no permission granted */
    data object PermissionRequired : BluetoothState()

    /** Bluetooth is ready to use */
    data object Ready : BluetoothState()

    /** Currently scanning for devices */
    data object Scanning : BluetoothState()

    /** Currently connecting to a device */
    data class Connecting(val device: BluetoothDevice) : BluetoothState()

    /** Connected to a device */
    data class Connected(val device: BluetoothDevice) : BluetoothState()

    /** Disconnected from a device */
    data object Disconnected : BluetoothState()

    /** Connection error occurred */
    data class Error(val message: String) : BluetoothState()
}

/**
 * Data class representing a Bluetooth device.
 * This is the core model used throughout the Bluetooth system.
 *
 * @property name The display name of the device
 * @property address The MAC address of the device (unique identifier)
 * @property isPaired Whether the device has been paired with the phone
 * @property isConnected Whether currently connected to the device
 * @property rssi Signal strength (if available during scan)
 * @property deviceType The type of Bluetooth device
 */
data class BluetoothDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean = false,
    val isConnected: Boolean = false,
    val rssi: Int = 0,
    val deviceType: DeviceType = DeviceType.UNKNOWN
) {
    /**
     * Check if this device has a valid name.
     */
    fun hasValidName(): Boolean = name.isNotBlank() && name != "Unknown Device"

    /**
     * Get a display name, using address as fallback.
     */
    fun getDisplayName(): String = if (hasValidName()) name else address
}

/**
 * Type of Bluetooth device.
 */
enum class DeviceType {
    CLASSIC,      // Classic Bluetooth (Bluetooth 2.0/3.0)
    BLE,          // Bluetooth Low Energy (Bluetooth 4.0)
    DUAL,         // Supports both Classic and BLE
    UNKNOWN       // Unknown or unable to determine
}