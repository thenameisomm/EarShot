package com.earshot.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.earshot.model.BluetoothDevice
import com.earshot.repository.DeviceRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the Device Screen.
 * Manages Bluetooth device list and scanning state.
 */
class DeviceViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _devices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val devices: LiveData<List<BluetoothDevice>> = _devices

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadDevices()
        observeDevices()
        observeScanning()
    }

    private fun observeDevices() {
        viewModelScope.launch {
            deviceRepository.devices.collectLatest { deviceList ->
                _devices.value = deviceList
            }
        }
    }

    private fun observeScanning() {
        viewModelScope.launch {
            deviceRepository.isScanning.collectLatest { scanning ->
                _isScanning.value = scanning
            }
        }
    }

    /**
     * Load cached devices.
     */
    fun loadDevices() {
        deviceRepository.loadDevices()
    }

    /**
     * Start scanning for new devices.
     */
    fun startScanning() {
        _errorMessage.value = null
        deviceRepository.startScanning()
    }

    /**
     * Connect to a specific device.
     */
    fun connectToDevice(device: BluetoothDevice) {
        try {
            deviceRepository.connectToDevice(device)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to connect: ${e.message}"
        }
    }

    /**
     * Disconnect from current device.
     */
    fun disconnectDevice() {
        try {
            deviceRepository.disconnectDevice()
        } catch (e: Exception) {
            _errorMessage.value = "Failed to disconnect: ${e.message}"
        }
    }

    /**
     * Pair with a device.
     */
    fun pairDevice(device: BluetoothDevice) {
        try {
            deviceRepository.pairDevice(device)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to pair: ${e.message}"
        }
    }

    /**
     * Unpair from a device.
     */
    fun unpairDevice(device: BluetoothDevice) {
        try {
            deviceRepository.unpairDevice(device)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to unpair: ${e.message}"
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Factory for creating DeviceViewModel with dependencies.
     */
    class Factory(
        private val deviceRepository: DeviceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
                return DeviceViewModel(deviceRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}