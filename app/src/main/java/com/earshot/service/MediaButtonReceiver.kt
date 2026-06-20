package com.earshot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that intercepts hardware media button presses
 * (e.g., from Bluetooth headsets/earbuds) and forwards them to the
 * MediaButtonService for processing.
 *
 * This receiver is registered in the manifest to receive media button
 * intents even when the app is not in the foreground.
 *
 * @see <a href="https://developer.android.com/guide/topics/media-apps/mediabuttons">Media Buttons</a>
 */
class MediaButtonReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MediaButtonReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called with action: ${intent.action}")

        // Check if this is a media button event
        if (Intent.ACTION_MEDIA_BUTTON == intent.action) {
            val keyEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }

            if (keyEvent != null) {
                Log.d(TAG, "Media button pressed: keyCode=${keyEvent.keyCode}, action=${keyEvent.action}")

                // Only process key down events
                if (keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    // Forward to MediaButtonService
                    val serviceIntent = Intent(context, MediaButtonService::class.java).apply {
                        action = intent.action
                        putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
                    }

                    // Start the service to handle the event
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}