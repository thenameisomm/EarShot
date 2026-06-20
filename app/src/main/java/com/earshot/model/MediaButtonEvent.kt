package com.earshot.model

/**
 * Enum representing the different media button types that can be detected.
 * These correspond to common Bluetooth earbud button actions.
 */
enum class MediaButtonType(val displayName: String) {
    PLAY_PAUSE("Play/Pause pressed"),
    NEXT_TRACK("Next Track pressed"),
    PREVIOUS_TRACK("Previous Track pressed"),
    UNKNOWN("Unknown button pressed")
}

/**
 * Data class representing a media button event.
 *
 * @property eventType The type of media button event
 * @property timestamp The time when the event occurred
 */
data class MediaButtonEvent(
    val eventType: MediaButtonType,
    val timestamp: Long = System.currentTimeMillis()
)