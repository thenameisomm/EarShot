package com.earshot.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.earshot.model.CameraMode
import com.earshot.model.CameraSelection
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Manages CameraX lifecycle and operations for photo and video capture.
 *
 * This class handles:
 * - Camera lifecycle binding to Fragment/Activity lifecycle
 * - Rear and front camera switching
 * - Photo and video mode switching
 * - Flash mode toggling (Auto, On, Off)
 * - Preview display with proper orientation handling
 *
 * ## Thread Safety
 *
 * All CameraX calls must be made on the main thread.
 *
 * ## Usage
 *
 * ```kotlin
 * val cameraXManager = CameraXManager(context, previewView)
 *
 * // Bind to lifecycle
 * cameraXManager.bindToLifecycle(lifecycleOwner)
 *
 * // Start preview
 * cameraXManager.startPreview()
 *
 * // Take photo
 * cameraXManager.takePhoto(
 *     outputFile = outputFile,
 *     onSuccess = { uri -> /* handle success */ },
 *     onError = { e -> /* handle error */ }
 * )
 *
 * // Unbind when done
 * cameraXManager.unbind()
 * ```
 */
class CameraXManager(
    private val context: Context,
    private val previewView: PreviewView? = null
) {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    companion object {
        private const val TAG = "CameraXManager"

        /** Flash mode constants */
        const val FLASH_MODE_AUTO = ImageCapture.FLASH_MODE_AUTO
        const val FLASH_MODE_ON = ImageCapture.FLASH_MODE_ON
        const val FLASH_MODE_OFF = ImageCapture.FLASH_MODE_OFF
    }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private val mainHandler = Handler(Looper.getMainLooper())

    private val photoExecutor: Executor = Executors.newSingleThreadExecutor()

    /** Camera provider instance */
    private var cameraProvider: ProcessCameraProvider? = null

    /** Current camera instance */
    private var camera: Camera? = null

    /** Image capture use case */
    private var imageCapture: ImageCapture? = null

    /** Video capture use case */
    private var videoCapture: VideoCapture<Recorder>? = null

    /** Current recording session */
    private var recording: Recording? = null

    /** Preview use case */
    private var preview: Preview? = null

    /** Current camera facing (front or rear) */
    private var currentFacing: CameraSelection = CameraSelection.REAR

    /** Current camera mode (photo or video) */
    private var currentMode: CameraMode = CameraMode.PHOTO

    /** Current flash mode */
    private var currentFlashMode: Int = ImageCapture.FLASH_MODE_AUTO

    /** Stored lifecycle owner for camera operations */
    private var storedLifecycleOwner: LifecycleOwner? = null

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Bind CameraX to the lifecycle of a Fragment/Activity.
     *
     * This initializes the CameraProvider and binds the preview use case.
     *
     * @param lifecycleOwner The lifecycle owner (Fragment or Activity)
     */
    fun bindToLifecycle(lifecycleOwner: LifecycleOwner) {
        if (!isCameraAvailable()) {
            mainHandler.post {
                Log.e(TAG, "Camera is not available on this device")
            }
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindPreview(lifecycleOwner)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    /**
     * Unbind all camera use cases and release resources.
     *
     * Call this when the Fragment/Activity is destroyed or paused to prevent
     * memory leaks.
     */
    fun unbind() {
        mainHandler.post {
            stopVideoRecording()
            cameraProvider?.let { provider ->
                try {
                    provider.unbindAll()
                } catch (e: Exception) {
                    Log.e(TAG, "Error unbinding camera", e)
                }
            }
            camera = null
            imageCapture = null
            videoCapture = null
            preview = null
        }
    }

    /**
     * Start the preview display.
     *
     * This binds the preview use case if not already bound.
     */
    fun startPreview() {
        if (cameraProvider == null) {
            Log.e(TAG, "Camera provider not initialized. Call bindToLifecycle first.")
            return
        }

        // Preview should be bound during bindToLifecycle
        // This method exists for explicit preview control if needed
        Log.d(TAG, "Preview started")
    }

    /**
     * Stop the preview display.
     */
    fun stopPreview() {
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        videoCapture = null
        Log.d(TAG, "Preview stopped")
    }

    /**
     * Take a photo and save it to the specified file.
     *
     * @param outputFile The file to save the photo to
     * @param onSuccess Callback called when photo is successfully saved
     * @param onError Callback called when photo capture fails
     */
    fun takePhoto(
        outputFile: File,
        onSuccess: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        mainHandler.post {
            val imageCapture = imageCapture ?: run {
                Log.e(TAG, "ImageCapture is not initialized")
                onError(IllegalStateException("ImageCapture not initialized"))
                return@post
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

            imageCapture.takePicture(
                outputOptions,
                photoExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(imageResult: ImageCapture.OutputFileResults) {
                        val savedUri = imageResult.savedUri
                        if (savedUri != null) {
                            mainHandler.post {
                                onSuccess(savedUri)
                                Log.d(TAG, "Photo saved to: ${savedUri.path}")
                            }
                        } else {
                            // If savedUri is null, use file URI
                            val fileUri = Uri.fromFile(outputFile)
                            mainHandler.post {
                                onSuccess(fileUri)
                                Log.d(TAG, "Photo saved to: ${outputFile.absolutePath}")
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        mainHandler.post {
                            onError(exception)
                            Log.e(TAG, "Photo capture failed", exception)
                        }
                    }
                }
            )
        }
    }

    /**
     * Start video recording to the specified file.
     *
     * @param outputFile The file to save the video to
     * @param onSuccess Callback called when recording starts successfully
     * @param onError Callback called when recording fails to start
     */
    fun startVideoRecording(
        outputFile: File,
        onSuccess: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        mainHandler.post {
            val videoCapture = videoCapture ?: run {
                Log.e(TAG, "VideoCapture is not initialized")
                onError(IllegalStateException("VideoCapture not initialized"))
                return@post
            }

            // Check if already recording
            if (recording != null) {
                Log.w(TAG, "Already recording")
                return@post
            }

            val outputOptions = FileOutputOptions.Builder(outputFile).build()

            try {
                recording = videoCapture.output
                    .prepareRecording(context, outputOptions)
                    .start(ContextCompat.getMainExecutor(context)) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                Log.d(TAG, "Video recording started")
                                onSuccess(Uri.fromFile(outputFile))
                            }
                            is VideoRecordEvent.Finalize -> {
                                if (event.hasError()) {
                                    val errorMsg = "Video recording error: ${event.cause?.message ?: "Unknown"}"
                                    Log.e(TAG, errorMsg)
                                    onError(Exception(errorMsg))
                                } else {
                                    Log.d(TAG, "Video recording finished successfully")
                                    onSuccess(Uri.fromFile(outputFile))
                                }
                                recording = null
                            }
                            else -> {
                                Log.d(TAG, "Video recording event: $event")
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start video recording", e)
                onError(e)
            }
        }
    }

    /**
     * Stop video recording.
     */
    fun stopVideoRecording() {
        mainHandler.post {
            recording?.let {
                it.stop()
                Log.d(TAG, "Video recording stopped")
                recording = null
            } ?: run {
                Log.w(TAG, "No active recording to stop")
            }
        }
    }

    /**
     * Toggle the flash mode.
     *
     * Cycles through: Auto -> On -> Off -> Auto
     */
    fun toggleFlash() {
        mainHandler.post {
            currentFlashMode = when (currentFlashMode) {
                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                else -> ImageCapture.FLASH_MODE_AUTO
            }

            imageCapture?.let {
                it.flashMode = currentFlashMode
                Log.d(TAG, "Flash mode toggled to: $currentFlashMode")
            }
        }
    }

    /**
     * Switch between front and rear camera.
     *
     * @param facing The camera facing direction (front or rear)
     * @param lifecycleOwner The lifecycle owner for binding (required for camera switch to work)
     */
    fun switchCamera(facing: CameraSelection, lifecycleOwner: LifecycleOwner? = null) {
        mainHandler.post {
            currentFacing = facing
            val owner = lifecycleOwner ?: storedLifecycleOwner
            if (owner != null) {
                cameraProvider?.let { provider ->
                    provider.unbindAll()
                    bindPreview(owner)
                }
            } else {
                // If no lifecycleOwner provided, just update the facing state
                Log.d(TAG, "Camera switch queued, will apply on next bind")
            }
        }
    }

    /**
     * Change the camera mode (photo or video).
     *
     * @param mode The camera mode to switch to
     */
    fun setCameraMode(mode: CameraMode, lifecycleOwner: LifecycleOwner? = null) {
        mainHandler.post {
            currentMode = mode
            val owner = lifecycleOwner ?: storedLifecycleOwner
            if (owner != null) {
                cameraProvider?.let { provider ->
                    provider.unbindAll()
                    bindPreview(owner)
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal Methods
    // -----------------------------------------------------------------------

    /**
     * Check if a camera with the required features is available.
     */
    private fun isCameraAvailable(): Boolean {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        return try {
            cameraProviderFuture.get().hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ||
                cameraProviderFuture.get().hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Bind the preview and appropriate capture use cases.
     *
     * @param lifecycleOwner The lifecycle owner (required)
     */
    private fun bindPreview(lifecycleOwner: LifecycleOwner) {
        val cameraProvider = cameraProvider ?: return

        // Store lifecycle owner for camera switching
        storedLifecycleOwner = lifecycleOwner

        // Unbind all before rebinding
        cameraProvider.unbindAll()

        // Build camera selector based on current facing
        val cameraSelector = when (currentFacing) {
            CameraSelection.REAR -> CameraSelector.DEFAULT_BACK_CAMERA
            CameraSelection.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        }

        // Preview use case
        preview = Preview.Builder().build().also {
            val surfaceProvider = previewView?.surfaceProvider
            if (surfaceProvider != null) {
                it.setSurfaceProvider(surfaceProvider)
            } else {
                Log.w(TAG, "No surface provider available for preview")
                return
            }
        }

        // Image capture use case (always available)
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(currentFlashMode)
            .build()

        // Video capture use case - create Recorder first
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        // Bind to lifecycle
        try {
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )

            Log.d(
                TAG,
                "Preview bound for ${currentFacing.name} camera in ${currentMode.name} mode"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera use cases", e)
        }
    }

    /**
     * Get the current camera facing.
     */
    fun getFacing(): CameraSelection = currentFacing

    /**
     * Get the current camera mode.
     */
    fun getMode(): CameraMode = currentMode

    /**
     * Get the current flash mode.
     */
    fun getFlashMode(): Int = currentFlashMode

    /**
     * Check if recording is in progress.
     */
    fun isRecording(): Boolean = recording != null
}