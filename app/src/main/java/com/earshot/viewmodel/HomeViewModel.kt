package com.earshot.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.earshot.repository.DeviceRepository

/**
 * ViewModel for the Home Screen.
 * Manages Bluetooth connection status display.
 */
class HomeViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _connectedDeviceName = MutableLiveData<String?>(null)
    val connectedDeviceName: LiveData<String?> = _connectedDeviceName

    init {
        // Observe connected device from repository
        deviceRepository.connectedDevice.let { flow ->
            // In a real implementation, we would observe this StateFlow
            // For now, we check if there's a connected device
        }
    }

    /**
     * Refresh the connection status.
     */
    fun refreshConnectionStatus() {
        val connected = deviceRepository.connectedDevice.value
        _isConnected.value = connected != null
        _connectedDeviceName.value = connected?.name
    }

    /**
     * Factory for creating HomeViewModel with dependencies.
     */
    class Factory(
        private val deviceRepository: DeviceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(deviceRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}