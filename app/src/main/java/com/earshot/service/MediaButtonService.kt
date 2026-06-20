package com.earshot.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.earshot.R
import com.earshot.model.MediaButtonEvent
import com.earshot.model.MediaButtonType
import com.earshot.ui.MainActivity

/**
 * Foreground Service that detects Bluetooth media button presses.
 *
 * This service uses the MediaSession API to intercept media button events
 * from Bluetooth headsets/earbuds. It runs as a foreground service to ensure
 * it can receive events even when the app is in the background.
 *
 * Supported button events:
 * - Play/Pause
 * - Next Track
 * - Previous Track
 *
 * @see <a href="https://developer.android.com/guide/topics/media-apps/mediabuttons">Media Buttons</a>
 */
class MediaButtonService : Service() {

    private lateinit var notificationManager: NotificationManager

    // Callback for dispatching events to the UI
    var onMediaButtonEvent: ((MediaButtonEvent) -> Unit)? = null

    companion object {
        private const val TAG = "MediaButtonService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "earshot_media_button_channel"

        const val ACTION_PLAY_PAUSE = "com.earshot.action.PLAY_PAUSE"
        const val ACTION_NEXT_TRACK = "com.earshot.action.NEXT_TRACK"
        const val ACTION_PREVIOUS_TRACK = "com.earshot.action.PREVIOUS_TRACK"

        /**
         * Start this service with proper intent handling.
         */
        fun start(context: Context) {
            val intent = Intent(context, MediaButtonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop this service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, MediaButtonService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with intent: ${intent?.action}")

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())

        // Handle media button intents
        handleIntent(intent)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    /**
     * Handle incoming media button intents.
     * This method is called both from onStartCommand and from MediaButtonReceiver.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            Log.d(TAG, "Intent is null")
            return
        }

        val action = intent.action
        val buttonType = when (action) {
            ACTION_PLAY_PAUSE -> MediaButtonType.PLAY_PAUSE
            ACTION_NEXT_TRACK -> MediaButtonType.NEXT_TRACK
            ACTION_PREVIOUS_TRACK -> MediaButtonType.PREVIOUS_TRACK
            else -> {
                // Check for standard media button actions
                when {
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent::class.java) != null -> {
                        handleKeyEvent(intent)
                    }
                    else -> {
                        Log.d(TAG, "Unknown action: $action")
                        MediaButtonType.UNKNOWN
                    }
                }
            }
        }

        if (buttonType != MediaButtonType.UNKNOWN) {
            processMediaButtonEvent(buttonType)
        }
    }

    /**
     * Handle KeyEvent from MediaButtonReceiver.
     */
    private fun handleKeyEvent(intent: Intent): MediaButtonType {
        val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        }

        if (keyEvent == null) {
            Log.d(TAG, "KeyEvent is null")
            return MediaButtonType.UNKNOWN
        }

        // Only handle key down events to avoid duplicate events
        if (keyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
            return MediaButtonType.UNKNOWN
        }

        val buttonType = when (keyEvent.keyCode) {
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> MediaButtonType.PLAY_PAUSE
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> MediaButtonType.PLAY_PAUSE
            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> MediaButtonType.PLAY_PAUSE
            android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> MediaButtonType.NEXT_TRACK
            android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaButtonType.PREVIOUS_TRACK
            else -> {
                Log.d(TAG, "Unknown keyCode: ${keyEvent.keyCode}")
                MediaButtonType.UNKNOWN
            }
        }

        if (buttonType != MediaButtonType.UNKNOWN) {
            processMediaButtonEvent(buttonType)
        }

        return buttonType
    }

    /**
     * Process a media button event - dispatch to callback and update notification.
     */
    private fun processMediaButtonEvent(buttonType: MediaButtonType) {
        val event = MediaButtonEvent(buttonType)
        Log.d(TAG, "Media button event detected: ${buttonType.displayName}")

        // Log to logcat
        android.util.Log.i(TAG, ">>> ${buttonType.displayName}")

        // Dispatch to callback for UI update
        onMediaButtonEvent?.invoke(event)

        // Update notification to show last pressed button
        updateNotification(buttonType)
    }

    /**
     * Create the notification channel for Android O and above.
     */
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

    /**
     * Create the foreground service notification.
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Listening for Bluetooth media buttons...")
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Update the notification to show the last pressed button.
     */
    private fun updateNotification(buttonType: MediaButtonType) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(buttonType.displayName)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}