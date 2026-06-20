package com.earshot.model

/**
 * Enum representing the different gesture types that can be detected from earbud buttons.
 */
enum class GestureType(val displayName: String) {
    SINGLE_TAP("Single Tap"),
    DOUBLE_TAP("Double Tap"),
    TRIPLE_TAP("Triple Tap"),
    LONG_PRESS("Long Press")
}

/**
 * Enum representing the available camera actions that can be mapped to gestures.
 */
enum class CameraAction(val displayName: String) {
    NONE("None"),
    TAKE_PHOTO("Take Photo"),
    START_VIDEO("Start Video"),
    STOP_VIDEO("Stop Video"),
    SWITCH_CAMERA("Switch Camera"),
    TOGGLE_FLASH("Toggle Flash")
}

/**
 * Data class representing a gesture to camera action mapping.
 *
 * @property gestureType The type of gesture
 * @property cameraAction The camera action to perform when gesture is detected
 */
data class GestureMapping(
    val gestureType: GestureType,
    val cameraAction: CameraAction = CameraAction.NONE
)