package com.earshot.model

/**
 * Enum representing the available cameras on the device.
 */
enum class CameraSelection {
    FRONT,
    REAR
}

/**
 * Enum representing the camera mode.
 */
enum class CameraMode {
    PHOTO,
    VIDEO
}

/**
 * Enum representing timer options.
 */
enum class TimerOption(val displayName: String, val seconds: Int) {
    OFF("Off", 0),
    THREE_SECONDS("3 seconds", 3),
    TEN_SECONDS("10 seconds", 10)
}

/**
 * Data class representing camera settings.
 *
 * @property cameraSelection Front or rear camera
 * @property cameraMode Photo or video mode
 * @property gridOverlayEnabled Whether to show grid overlay
 * @property timerOption Timer setting
 */
data class CameraSettings(
    val cameraSelection: CameraSelection = CameraSelection.REAR,
    val cameraMode: CameraMode = CameraMode.PHOTO,
    val gridOverlayEnabled: Boolean = false,
    val timerOption: TimerOption = TimerOption.OFF
)