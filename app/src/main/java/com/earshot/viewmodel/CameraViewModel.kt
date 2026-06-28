package com.earshot.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.earshot.camera.CameraPhotoOutput
import com.earshot.camera.CameraXManager
import com.earshot.model.CameraMode
import com.earshot.model.CameraSelection
import com.earshot.model.CameraSettings
import com.earshot.model.TimerOption
import com.earshot.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for the Camera functionality.
 * Manages CameraX operations and camera settings.
 *
 * ## Features
 *
 * - Photo and video capture
 * - Camera switching (front/rear)
 * - Flash mode control
 * - Camera settings management
 *
 * ## Usage
 *
 * ```kotlin
 * val cameraViewModel = ViewModelProvider(this, CameraViewModel.Factory())
 *     .get(CameraViewModel::class.java)
 *
 * // Observe capture results
 * cameraViewModel.photoUri.observe(viewLifecycleOwner) { uri ->
 *     // Handle photo URI
 * }
 * ```
 */
class CameraViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "CameraViewModel"
    }

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private val context = application.applicationContext
    private val photoOutput = CameraPhotoOutput(context)

    // -----------------------------------------------------------------------
    // CameraX Manager
    // -----------------------------------------------------------------------

    private val _cameraXManager = MutableLiveData<CameraXManager?>()
    val cameraXManager: LiveData<CameraXManager?> = _cameraXManager

    // -----------------------------------------------------------------------
    // Camera State
    // -----------------------------------------------------------------------

    private val _cameraSelection = MutableLiveData<CameraSelection>()
    val cameraSelection: LiveData<CameraSelection> = _cameraSelection

    private val _cameraMode = MutableLiveData<CameraMode>()
    val cameraMode: LiveData<CameraMode> = _cameraMode

    private val _gridOverlayEnabled = MutableLiveData<Boolean>()
    val gridOverlayEnabled: LiveData<Boolean> = _gridOverlayEnabled

    private val _timerOption = MutableLiveData<TimerOption>()
    val timerOption: LiveData<TimerOption> = _timerOption

    // -----------------------------------------------------------------------
    // Capture State
    // -----------------------------------------------------------------------

    private val _photoUri = MutableLiveData<Uri?>()
    val photoUri: LiveData<Uri?> = _photoUri

    private val _videoUri = MutableLiveData<Uri?>()
    val videoUri: LiveData<Uri?> = _videoUri

    private val _isRecording = MutableLiveData<Boolean>(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private val _flashMode = MutableLiveData<Int>(CameraXManager.FLASH_MODE_AUTO)
    val flashMode: LiveData<Int> = _flashMode

    private val _saveSuccess = MutableLiveData<Boolean?>(null)
    val saveSuccess: LiveData<Boolean?> = _saveSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    val timerOptions: List<TimerOption> = TimerOption.entries

    // Track current video file for MediaStore insertion
    private var currentVideoFile: File? = null

    // -----------------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------------

    init {
        loadSettings()
        // Don't initialize CameraX here - let CameraFragment call initializeCameraX()
        // with the proper PreviewView reference
    }

    // -----------------------------------------------------------------------
    // CameraX Setup
    // -----------------------------------------------------------------------

    /**
     * Initialize CameraXManager with the provided PreviewView.
     *
     * @param previewView The PreviewView for camera preview
     */
    fun initializeCameraX(previewView: androidx.camera.view.PreviewView? = null) {
        viewModelScope.launch {
            _cameraXManager.value = CameraXManager(context, previewView)
        }
    }

    /**
     * Bind CameraX to the lifecycle.
     *
     * Call this in Fragment's onStart() or Activity's onStart().
     */
    fun bindToLifecycle(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        _cameraXManager.value?.bindToLifecycle(lifecycleOwner)
    }

    /**
     * Unbind CameraX from the lifecycle.
     *
     * Call this in Fragment's onStop() or Activity's onStop().
     */
    fun unbind() {
        _cameraXManager.value?.unbind()
        _cameraXManager.value = null
    }

    /**
     * Start the camera preview.
     */
    fun startPreview() {
        _cameraXManager.value?.startPreview()
    }

    /**
     * Stop the camera preview.
     */
    fun stopPreview() {
        _cameraXManager.value?.stopPreview()
    }

    // -----------------------------------------------------------------------
    // Capture Methods
    // -----------------------------------------------------------------------

    /**
     * Take a photo.
     *
     * The photo will be saved to the app-specific directory and inserted
     * into the MediaStore gallery.
     */
    fun takePhoto() {
        viewModelScope.launch(Dispatchers.IO) {
            val cameraManager = _cameraXManager.value ?: run {
                _error.value = "Camera not initialized"
                return@launch
            }

            try {
                val outputFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "EarShot/${photoOutput.generateUniqueFilename()}"
                )

                cameraManager.takePhoto(
                    outputFile = outputFile,
                    onSuccess = { uri ->
                        // Insert photo to gallery so it's visible in system gallery
                        try {
                            photoOutput.insertToGallery(outputFile)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to insert photo to gallery", e)
                        }
                        _photoUri.value = uri
                        launch(Dispatchers.Main) {
                            _error.value = null
                        }
                    },
                    onError = { e ->
                        launch(Dispatchers.Main) {
                            _error.value = "Failed to capture photo: ${e.message}"
                        }
                    }
                )
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    _error.value = "Failed to create photo file: ${e.message}"
                }
            }
        }
    }

    /**
     * Start video recording.
     */
    fun startVideo() {
        viewModelScope.launch(Dispatchers.IO) {
            val cameraManager = _cameraXManager.value ?: run {
                _error.value = "Camera not initialized"
                return@launch
            }

            try {
                val videoFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    "EarShot/${videoOutputFilename()}"
                )
                // Ensure directory exists
                videoFile.parentFile?.mkdirs()
                // Track the video file for later insertion to MediaStore
                currentVideoFile = videoFile

                cameraManager.startVideoRecording(
                    outputFile = videoFile,
                    onSuccess = { uri ->
                        _videoUri.value = uri
                        _isRecording.value = true
                        launch(Dispatchers.Main) {
                            _error.value = null
                        }
                    },
                    onError = { e ->
                        currentVideoFile = null
                        launch(Dispatchers.Main) {
                            _error.value = "Failed to start video recording: ${e.message}"
                        }
                    }
                )
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    _error.value = "Failed to create video file: ${e.message}"
                }
            }
        }
    }

    /**
     * Stop video recording.
     */
    fun stopVideo() {
        val videoFile = currentVideoFile
        _cameraXManager.value?.stopVideoRecording()
        _isRecording.value = false

        // Insert video to gallery after recording stops
        if (videoFile != null && videoFile.exists()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    photoOutput.insertVideoToGallery(videoFile)
                    Log.d(TAG, "Video inserted to gallery: ${videoFile.absolutePath}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to insert video to gallery", e)
                }
                currentVideoFile = null
            }
        }
    }

    /**
     * Switch between front and rear camera.
     *
     * @param selection The camera selection (front or rear)
     */
    fun switchCamera(selection: CameraSelection) {
        _cameraSelection.value = selection
        _cameraXManager.value?.switchCamera(selection)
    }

    /**
     * Toggle flash mode.
     */
    fun toggleFlash() {
        _cameraXManager.value?.toggleFlash()
        _flashMode.value = _cameraXManager.value?.getFlashMode() ?: CameraXManager.FLASH_MODE_AUTO
    }

    // -----------------------------------------------------------------------
    // Settings Methods
    // -----------------------------------------------------------------------

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
        _cameraXManager.value?.setCameraMode(mode)
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
            _error.value = "Failed to save settings: ${e.message}"
        }
    }

    /**
     * Clear the save success status.
     */
    fun clearSaveStatus() {
        _saveSuccess.value = null
    }

    /**
     * Clear the current error.
     */
    fun clearError() {
        _error.value = null
    }

    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    private fun videoOutputFilename(): String {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        return "VID_${timestamp}.mp4"
    }

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    /**
     * Factory for creating CameraViewModel with dependencies.
     */
    class Factory(
        private val application: Application,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
                return CameraViewModel(application, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
