package com.earshot.repository

import com.earshot.model.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing Bluetooth devices.
 * Currently provides placeholder data - actual Bluetooth functionality not implemented yet.
 */
class DeviceRepository {

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    /**
     * Load placeholder devices for UI demonstration.
     * In a real implementation, this would scan for actual Bluetooth devices.
     */
    fun loadDevices() {
        // Placeholder devices - not implemented yet
        _devices.value = listOf(
            BluetoothDevice(
                name = "AirPods Pro",
                address = "AA:BB:CC:DD:EE:01",
                isConnected = true,
                isPaired = true
            ),
            BluetoothDevice(
                name = "Galaxy Buds",
                address = "AA:BB:CC:DD:EE:02",
                isConnected = false,
                isPaired = true
            ),
            BluetoothDevice(
                name = "Sony WF-1000XM4",
                address = "AA:BB:CC:DD:EE:03",
                isConnected = false,
                isPaired = true
            ),
            BluetoothDevice(
                name = " Jabra Elite 75t",
                address = "AA:BB:CC:DD:EE:04",
                isConnected = false,
                isPaired = false
            )
        )
    }

    /**
     * Start scanning for devices.
     * Currently simulates scanning with placeholder data.
     */
    fun startScanning() {
        _isScanning.value = true

        // In a real implementation, this would start Bluetooth scanning
        // For now, we simulate scanning by loading devices after a delay
        loadDevices()

        _isScanning.value = false
    }

    /**
     * Connect to a specific device.
     * Currently not implemented - placeholder only.
     */
    fun connectToDevice(device: BluetoothDevice) {
        // In a real implementation, this would establish Bluetooth connection
        val updatedDevices = _devices.value.map {
            if (it.address == device.address) {
                it.copy(isConnected = true)
            } else {
                it.copy(isConnected = false)
            }
        }
        _devices.value = updatedDevices
        _connectedDevice.value = updatedDevices.find { it.isConnected }
    }

    /**
     * Disconnect from the current device.
     * Currently not implemented - placeholder only.
     */
    fun disconnectDevice() {
        val updatedDevices = _devices.value.map {
            it.copy(isConnected = false)
        }
        _devices.value = updatedDevices
        _connectedDevice.value = null
    }

    /**
     * Pair with a device.
     * Currently not implemented - placeholder only.
     */
    fun pairDevice(device: BluetoothDevice) {
        val updatedDevices = _devices.value.map {
            if (it.address == device.address) {
                it.copy(isPaired = true)
            } else {
                it
            }
        }
        _devices.value = updatedDevices
    }

    /**
     * Unpair from a device.
     * Currently not implemented - placeholder only.
     */
    fun unpairDevice(device: BluetoothDevice) {
        val updatedDevices = _devices.value.map {
            if (it.address == device.address) {
                it.copy(isPaired = false, isConnected = false)
            } else {
                it
            }
        }
        _devices.value = updatedDevices

        if (_connectedDevice.value?.address == device.address) {
            _connectedDevice.value = null
        }
    }
}