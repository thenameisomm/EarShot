package com.earshot.repository

import android.content.Context
import android.content.SharedPreferences
import com.earshot.model.CameraAction
import com.earshot.model.CameraMode
import com.earshot.model.CameraSelection
import com.earshot.model.CameraSettings
import com.earshot.model.GestureMapping
import com.earshot.model.GestureType
import com.earshot.model.TimerOption

/**
 * Repository for managing app settings using SharedPreferences.
 * Handles persistence of gesture mappings and camera settings.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    /**
     * Get all gesture mappings from storage.
     */
    fun getGestureMappings(): List<GestureMapping> {
        return GestureType.entries.map { gestureType ->
            val actionName = prefs.getString(
                getGestureKey(gestureType),
                CameraAction.NONE.name
            ) ?: CameraAction.NONE.name

            val action = try {
                CameraAction.valueOf(actionName)
            } catch (e: IllegalArgumentException) {
                CameraAction.NONE
            }

            GestureMapping(gestureType, action)
        }
    }

    /**
     * Save all gesture mappings to storage.
     */
    fun saveGestureMappings(mappings: List<GestureMapping>) {
        prefs.edit().apply {
            mappings.forEach { mapping ->
                putString(
                    getGestureKey(mapping.gestureType),
                    mapping.cameraAction.name
                )
            }
            apply()
        }
    }

    /**
     * Reset gesture mappings to default (no action for all gestures).
     */
    fun resetGestureMappings() {
        prefs.edit().apply {
            GestureType.entries.forEach { gestureType ->
                putString(getGestureKey(gestureType), CameraAction.NONE.name)
            }
            apply()
        }
    }

    /**
     * Get camera settings from storage.
     */
    fun getCameraSettings(): CameraSettings {
        val cameraSelection = prefs.getString(
            KEY_CAMERA_SELECTION,
            CameraSelection.REAR.name
        )?.let {
            try {
                CameraSelection.valueOf(it)
            } catch (e: IllegalArgumentException) {
                CameraSelection.REAR
            }
        } ?: CameraSelection.REAR

        val cameraMode = prefs.getString(
            KEY_CAMERA_MODE,
            CameraMode.PHOTO.name
        )?.let {
            try {
                CameraMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                CameraMode.PHOTO
            }
        } ?: CameraMode.PHOTO

        val gridOverlayEnabled = prefs.getBoolean(KEY_GRID_OVERLAY, false)

        val timerOption = prefs.getString(
            KEY_TIMER_OPTION,
            TimerOption.OFF.name
        )?.let {
            try {
                TimerOption.valueOf(it)
            } catch (e: IllegalArgumentException) {
                TimerOption.OFF
            }
        } ?: TimerOption.OFF

        return CameraSettings(
            cameraSelection = cameraSelection,
            cameraMode = cameraMode,
            gridOverlayEnabled = gridOverlayEnabled,
            timerOption = timerOption
        )
    }

    /**
     * Save camera settings to storage.
     */
    fun saveCameraSettings(settings: CameraSettings) {
        prefs.edit().apply {
            putString(KEY_CAMERA_SELECTION, settings.cameraSelection.name)
            putString(KEY_CAMERA_MODE, settings.cameraMode.name)
            putBoolean(KEY_GRID_OVERLAY, settings.gridOverlayEnabled)
            putString(KEY_TIMER_OPTION, settings.timerOption.name)
            apply()
        }
    }

    /**
     * Get the currently connected device address.
     */
    fun getConnectedDeviceAddress(): String? {
        return prefs.getString(KEY_CONNECTED_DEVICE, null)
    }

    /**
     * Save the connected device address.
     */
    fun saveConnectedDeviceAddress(address: String?) {
        prefs.edit().apply {
            if (address != null) {
                putString(KEY_CONNECTED_DEVICE, address)
            } else {
                remove(KEY_CONNECTED_DEVICE)
            }
            apply()
        }
    }

    private fun getGestureKey(gestureType: GestureType): String {
        return "${KEY_GESTURE_PREFIX}${gestureType.name}"
    }

    companion object {
        private const val PREFS_NAME = "earshot_settings"
        private const val KEY_GESTURE_PREFIX = "gesture_"
        private const val KEY_CAMERA_SELECTION = "camera_selection"
        private const val KEY_CAMERA_MODE = "camera_mode"
        private const val KEY_GRID_OVERLAY = "grid_overlay"
        private const val KEY_TIMER_OPTION = "timer_option"
        private const val KEY_CONNECTED_DEVICE = "connected_device"
    }
}