package com.earshot.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.earshot.model.MediaButtonEvent
import com.earshot.service.MediaButtonService

/**
 * ViewModel for managing media button events.
 * This ViewModel coordinates between the MediaButtonService and the UI.
 */
class MediaButtonViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MediaButtonViewModel"
        private const val MAX_EVENT_HISTORY = 50 // Maximum events to keep in history
    }

    // List of recent media button events
    private val _events = MutableLiveData<List<MediaButtonEvent>>(emptyList())
    val events: LiveData<List<MediaButtonEvent>> = _events

    // Service running status
    private val _isServiceRunning = MutableLiveData(false)
    val isServiceRunning: LiveData<Boolean> = _isServiceRunning

    // Latest event for toast display
    private val _latestEvent = MutableLiveData<MediaButtonEvent?>()
    val latestEvent: LiveData<MediaButtonEvent?> = _latestEvent

    // Event callback that will be connected to the service
    val mediaButtonCallback: (MediaButtonEvent) -> Unit = { event ->
        onMediaButtonEvent(event)
    }

    /**
     * Handle incoming media button event from the service.
     */
    fun onMediaButtonEvent(event: MediaButtonEvent) {
        Log.d(TAG, "Received media button event: ${event.eventType.displayName}")

        // Add to event history
        val currentEvents = _events.value?.toMutableList() ?: mutableListOf()
        currentEvents.add(0, event) // Add at beginning for most recent first

        // Limit history size
        if (currentEvents.size > MAX_EVENT_HISTORY) {
            currentEvents.removeAt(currentEvents.size - 1)
        }

        _events.postValue(currentEvents)
        _latestEvent.postValue(event)
    }

    /**
     * Set the service running status.
     */
    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    /**
     * Start the media button service.
     */
    fun startService() {
        MediaButtonService.start(getApplication())
        _isServiceRunning.value = true
    }

    /**
     * Stop the media button service.
     */
    fun stopService() {
        MediaButtonService.stop(getApplication())
        _isServiceRunning.value = false
    }

    /**
     * Clear the event history.
     */
    fun clearHistory() {
        _events.value = emptyList()
    }

    /**
     * Clear the latest event (after toast is shown).
     */
    fun clearLatestEvent() {
        _latestEvent.value = null
    }

    /**
     * Factory for creating MediaButtonViewModel with dependencies.
     */
    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MediaButtonViewModel::class.java)) {
                return MediaButtonViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}