package com.earshot.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.earshot.model.CameraAction
import com.earshot.model.GestureMapping
import com.earshot.model.GestureType
import com.earshot.repository.SettingsRepository

/**
 * ViewModel for the Gesture Mapping Screen.
 * Manages gesture to camera action mappings.
 */
class GestureViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _gestureMappings = MutableLiveData<List<GestureMapping>>(emptyList())
    val gestureMappings: LiveData<List<GestureMapping>> = _gestureMappings

    private val _saveSuccess = MutableLiveData<Boolean?>(null)
    val saveSuccess: LiveData<Boolean?> = _saveSuccess

    val availableActions: List<CameraAction> = CameraAction.entries

    init {
        loadMappings()
    }

    /**
     * Load gesture mappings from storage.
     */
    fun loadMappings() {
        _gestureMappings.value = settingsRepository.getGestureMappings()
    }

    /**
     * Update the camera action for a specific gesture.
     */
    fun updateGestureAction(gestureType: GestureType, action: CameraAction) {
        val currentMappings = _gestureMappings.value?.toMutableList() ?: mutableListOf()
        val index = currentMappings.indexOfFirst { it.gestureType == gestureType }

        if (index != -1) {
            currentMappings[index] = GestureMapping(gestureType, action)
            _gestureMappings.value = currentMappings
        }
    }

    /**
     * Save all gesture mappings to storage.
     */
    fun saveMappings() {
        try {
            val mappings = _gestureMappings.value
            if (mappings != null) {
                settingsRepository.saveGestureMappings(mappings)
                _saveSuccess.value = true
            }
        } catch (e: Exception) {
            _saveSuccess.value = false
        }
    }

    /**
     * Reset all mappings to default (no action).
     */
    fun resetMappings() {
        try {
            settingsRepository.resetGestureMappings()
            loadMappings()
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
     * Factory for creating GestureViewModel with dependencies.
     */
    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GestureViewModel::class.java)) {
                return GestureViewModel(settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}