package com.earshot.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.earshot.model.CameraMode
import com.earshot.model.CameraSelection
import com.earshot.model.CameraSettings
import com.earshot.model.TimerOption
import com.earshot.repository.SettingsRepository

/**
 * ViewModel for the Camera Settings Screen.
 * Manages camera configuration options.
 */
class CameraViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _cameraSelection = MutableLiveData<CameraSelection>()
    val cameraSelection: LiveData<CameraSelection> = _cameraSelection

    private val _cameraMode = MutableLiveData<CameraMode>()
    val cameraMode: LiveData<CameraMode> = _cameraMode

    private val _gridOverlayEnabled = MutableLiveData<Boolean>()
    val gridOverlayEnabled: LiveData<Boolean> = _gridOverlayEnabled

    private val _timerOption = MutableLiveData<TimerOption>()
    val timerOption: LiveData<TimerOption> = _timerOption

    private val _saveSuccess = MutableLiveData<Boolean?>(null)
    val saveSuccess: LiveData<Boolean?> = _saveSuccess

    val timerOptions: List<TimerOption> = TimerOption.entries

    init {
        loadSettings()
    }

    /**
     * Load camera settings from storage.
     */
    fun loadSettings() {
        val settings = settingsRepository.getCameraSettings()
        _cameraSelection.value = settings.cameraSelection
        _cameraMode.value = settings.cameraMode
        _gridOverlayEnabled.value = settings.gridOverlayEnabled
        _timerOption.value = settings.timerOption
    }

    /**
     * Set camera selection (front or rear).
     */
    fun setCameraSelection(selection: CameraSelection) {
        _cameraSelection.value = selection
    }

    /**
     * Set camera mode (photo or video).
     */
    fun setCameraMode(mode: CameraMode) {
        _cameraMode.value = mode
    }

    /**
     * Set grid overlay enabled state.
     */
    fun setGridOverlayEnabled(enabled: Boolean) {
        _gridOverlayEnabled.value = enabled
    }

    /**
     * Set timer option.
     */
    fun setTimerOption(option: TimerOption) {
        _timerOption.value = option
    }

    /**
     * Save camera settings to storage.
     */
    fun saveSettings() {
        try {
            val settings = CameraSettings(
                cameraSelection = _cameraSelection.value ?: CameraSelection.REAR,
                cameraMode = _cameraMode.value ?: CameraMode.PHOTO,
                gridOverlayEnabled = _gridOverlayEnabled.value ?: false,
                timerOption = _timerOption.value ?: TimerOption.OFF
            )
            settingsRepository.saveCameraSettings(settings)
            _saveSuccess.value = true
        } catch (e: Exception) {
            _saveSuccess.value = false
        }
    }

    /**
     * Clear the save success status.
     */
    fun clearSaveStatus() {
        _saveSuccess.value = null
    }

    /**
     * Factory for creating CameraViewModel with dependencies.
     */
    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
                return CameraViewModel(settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}