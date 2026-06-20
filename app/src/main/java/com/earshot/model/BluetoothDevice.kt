package com.earshot.model

/**
 * Data class representing a Bluetooth device.
 *
 * @property name The display name of the device
 * @property address The MAC address of the device
 * @property isConnected Whether the device is currently connected
 * @property isPaired Whether the device is paired with the phone
 */
data class BluetoothDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val isPaired: Boolean = false
)