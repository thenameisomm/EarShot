package com.earshot.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.earshot.R
import com.earshot.camera.CameraPhotoOutput
import com.earshot.camera.CameraXManager
import com.earshot.model.CameraAction
import com.earshot.model.CameraSelection
import com.earshot.model.MediaButtonEvent
import com.earshot.model.MediaButtonType
import com.earshot.repository.SettingsRepository
import com.earshot.ui.MainActivity
import java.io.File

/**
 * Foreground Service that detects Bluetooth media button presses and routes them
 * through [GestureEngine] to camera actions.
 *
 * ## What changed from the original
 *
 * - [GestureEngine] is instantiated in [onCreate] using a [SettingsRepository].
 * - [processMediaButtonEvent] now feeds the engine instead of only firing a callback.
 * - [handleKeyEvent] calls [GestureEngine.onButtonDown] / [onButtonUp] for long-press
 *   support when raw [android.view.KeyEvent] data is available.
 * - [executeCameraAction] is the new dispatch point — it calls [CameraXManager] methods
 *   for all camera actions (photo, video, switch, flash).
 * - [GestureEngine.release] is called in [onDestroy].
 * - [CameraXManager] is initialized lazily and bound to this service's lifecycle.
 */
class MediaButtonService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var gestureEngine: GestureEngine

    // CameraX Manager - initialized lazily
    private var cameraXManager: CameraXManager? = null
    private val photoOutput by lazy { CameraPhotoOutput(applicationContext) }

    // Optional callback so the UI (MediaButtonFragment) can observe raw events.
    var onMediaButtonEvent: ((MediaButtonEvent) -> Unit)? = null

    companion object {
        private const val TAG = "MediaButtonService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "earshot_media_button_channel"

        const val ACTION_PLAY_PAUSE     = "com.earshot.action.PLAY_PAUSE"
        const val ACTION_NEXT_TRACK     = "com.earshot.action.NEXT_TRACK"
        const val ACTION_PREVIOUS_TRACK = "com.earshot.action.PREVIOUS_TRACK"

        fun start(context: Context) {
            val intent = Intent(context, MediaButtonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MediaButtonService::class.java))
        }
    }

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Initialize CameraXManager
        cameraXManager = CameraXManager(applicationContext)

        // Build the engine. It reads SharedPreferences on the main thread (tiny file,
        // safe here) and calls executeCameraAction on every resolved gesture.
        gestureEngine = GestureEngine(
            settingsRepository = SettingsRepository(applicationContext),
            onActionTriggered  = { action -> executeCameraAction(action) }
        )

        Log.d(TAG, "Service created — CameraXManager and GestureEngine ready")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        handleIntent(intent)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        gestureEngine.release()
        cameraXManager?.unbind()
        cameraXManager = null
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    // ---------------------------------------------------------------------------
    // Intent routing
    // ---------------------------------------------------------------------------

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val buttonType = when (intent.action) {
            ACTION_PLAY_PAUSE     -> MediaButtonType.PLAY_PAUSE
            ACTION_NEXT_TRACK     -> MediaButtonType.NEXT_TRACK
            ACTION_PREVIOUS_TRACK -> MediaButtonType.PREVIOUS_TRACK
            Intent.ACTION_MEDIA_BUTTON -> {
                // Raw KeyEvent path — extract key and delegate.
                handleKeyEvent(intent)
                return
            }
            else -> {
                Log.d(TAG, "Unknown or null action: ${intent.action}")
                return
            }
        }

        processMediaButtonEvent(buttonType)
    }

    /**
     * Handles the raw [android.view.KeyEvent] embedded in a media-button [Intent].
     * Calls [GestureEngine.onButtonDown] / [onButtonUp] so long press can be detected.
     */
    private fun handleKeyEvent(intent: Intent) {
        val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        } ?: return

        val buttonType = when (keyEvent.keyCode) {
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE  -> MediaButtonType.PLAY_PAUSE
            android.view.KeyEvent.KEYCODE_MEDIA_NEXT   -> MediaButtonType.NEXT_TRACK
            android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaButtonType.PREVIOUS_TRACK
            else -> {
                Log.d(TAG, "Unhandled keyCode: ${keyEvent.keyCode}")
                return
            }
        }

        val event = MediaButtonEvent(buttonType)

        when (keyEvent.action) {
            android.view.KeyEvent.ACTION_DOWN -> {
                Log.d(TAG, "Key DOWN: ${buttonType.displayName}")
                gestureEngine.onButtonDown(event)
                // Also notify the UI of the raw event immediately.
                onMediaButtonEvent?.invoke(event)
                updateNotification(buttonType)
            }
            android.view.KeyEvent.ACTION_UP -> {
                Log.d(TAG, "Key UP: ${buttonType.displayName}")
                gestureEngine.onButtonUp(event)
            }
        }
    }

    /**
     * Called for intents that carry only an action string (no raw KeyEvent).
     * Uses the simple one-shot tap path — no long press detection.
     */
    private fun processMediaButtonEvent(buttonType: MediaButtonType) {
        val event = MediaButtonEvent(buttonType)
        Log.d(TAG, "Processing: ${buttonType.displayName}")

        // Notify the UI (MediaButtonFragment) for the event log.
        onMediaButtonEvent?.invoke(event)

        // Feed the engine — tap window + debounce happens inside.
        gestureEngine.onMediaButtonEvent(event)

        updateNotification(buttonType)
    }

    // ---------------------------------------------------------------------------
    // Camera dispatch — executes camera actions based on gestures
    // ---------------------------------------------------------------------------

    /**
     * Called by [GestureEngine] on the main thread when a gesture resolves to an action.
     *
     * This method executes the appropriate [CameraXManager] operation based on the
     * [CameraAction] type:
     *
     * - [CameraAction.TAKE_PHOTO]: Captures a photo and saves it to the gallery
     * - [CameraAction.START_VIDEO]: Begins video recording
     * - [CameraAction.STOP_VIDEO]: Stops the current video recording
     * - [CameraAction.SWITCH_CAMERA]: Toggles between front and rear camera
     * - [CameraAction.TOGGLE_FLASH]: Cycles the flash mode (auto/on/off)
     */
    private fun executeCameraAction(action: CameraAction) {
        Log.i(TAG, "▶ Camera action triggered: ${action.displayName}")

        when (action) {
            CameraAction.TAKE_PHOTO -> {
                executeTakePhoto()
            }
            CameraAction.START_VIDEO -> {
                executeStartVideo()
            }
            CameraAction.STOP_VIDEO -> {
                executeStopVideo()
            }
            CameraAction.SWITCH_CAMERA -> {
                executeSwitchCamera()
            }
            CameraAction.TOGGLE_FLASH -> {
                executeToggleFlash()
            }
            CameraAction.NONE -> {
                // Should not reach here — GestureEngine filters NONE
            }
        }

        // Update notification to reflect the triggered action.
        val notification = buildNotification("Last: ${action.displayName}")
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // -----------------------------------------------------------------------
    // Camera Action Implementations
    // -----------------------------------------------------------------------

    /**
     * Execute photo capture.
     */
    private fun executeTakePhoto() {
        val cameraManager = cameraXManager ?: run {
            Log.e(TAG, "CameraXManager not initialized")
            return
        }

        // Create photo file in app-specific directory
        val photoDir = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "EarShot"
        )
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }

        val photoFile = File(photoDir, photoOutput.generateUniqueFilename())

        cameraManager.takePhoto(
            outputFile = photoFile,
            onSuccess = { uri ->
                Log.i(TAG, "📸 Photo saved: $uri")
                // Insert into MediaStore gallery
                photoOutput.insertToGallery(photoFile)
            },
            onError = { e ->
                Log.e(TAG, "📸 Photo capture failed", e)
            }
        )
    }

    /**
     * Start video recording.
     */
    private fun executeStartVideo() {
        val cameraManager = cameraXManager ?: run {
            Log.e(TAG, "CameraXManager not initialized")
            return
        }

        // Create video file in app-specific directory
        val videoDir = File(
            getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "EarShot"
        )
        if (!videoDir.exists()) {
            videoDir.mkdirs()
        }

        val videoFile = File(videoDir, "VID_${photoOutput.generateUniqueFilename()}")

        cameraManager.startVideoRecording(
            outputFile = videoFile,
            onSuccess = { uri ->
                Log.i(TAG, "🎬 Video recording started: $uri")
            },
            onError = { e ->
                Log.e(TAG, "🎬 Video recording failed", e)
            }
        )
    }

    /**
     * Stop video recording.
     */
    private fun executeStopVideo() {
        val cameraManager = cameraXManager ?: run {
            Log.e(TAG, "CameraXManager not initialized")
            return
        }

        cameraManager.stopVideoRecording()
        Log.i(TAG, "⏹ Video recording stopped")
    }

    /**
     * Switch between front and rear camera.
     */
    private fun executeSwitchCamera() {
        val cameraManager = cameraXManager ?: run {
            Log.e(TAG, "CameraXManager not initialized")
            return
        }

        val currentFacing = cameraManager.getFacing()
        val newFacing = when (currentFacing) {
            CameraSelection.REAR -> CameraSelection.FRONT
            CameraSelection.FRONT -> CameraSelection.REAR
        }

        cameraManager.switchCamera(newFacing)
        Log.i(TAG, "🔄 Switched camera to ${newFacing.name}")
    }

    /**
     * Toggle flash mode.
     */
    private fun executeToggleFlash() {
        val cameraManager = cameraXManager ?: run {
            Log.e(TAG, "CameraXManager not initialized")
            return
        }

        cameraManager.toggleFlash()
        Log.i(TAG, "⚡ Flash mode toggled")
    }

    // ---------------------------------------------------------------------------
    // Notification helpers
    // ---------------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Button Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when EarShot is detecting media button presses"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification =
        buildNotification("Listening for Bluetooth media buttons…")

    private fun updateNotification(buttonType: MediaButtonType) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(buttonType.displayName))
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
