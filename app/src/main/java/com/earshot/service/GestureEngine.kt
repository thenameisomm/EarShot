package com.earshot.service

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.earshot.model.CameraAction
import com.earshot.model.GestureMapping
import com.earshot.model.GestureType
import com.earshot.model.MediaButtonEvent
import com.earshot.model.MediaButtonType
import com.earshot.repository.SettingsRepository

/**
 * GestureEngine detects tap patterns and long presses from raw [MediaButtonEvent]s and
 * translates them into [GestureType] values. It then looks up the user-configured
 * [CameraAction] from [SettingsRepository] and fires the result through [onActionTriggered].
 *
 * ## How gesture detection works
 *
 * Each incoming event carries a timestamp from [MediaButtonEvent.timestamp]. The engine
 * accumulates events into a "tap window" (default 300 ms). When the window closes with no
 * further events, the tap count determines the gesture:
 *
 *   1 tap  → SINGLE_TAP
 *   2 taps → DOUBLE_TAP
 *   3 taps → TRIPLE_TAP (any further taps are clamped to TRIPLE_TAP)
 *
 * Long press is detected separately: if the hardware holds ACTION_DOWN for longer than
 * [LONG_PRESS_THRESHOLD_MS], a LONG_PRESS gesture fires immediately and the tap counter
 * is suppressed so no additional tap gesture fires on release.
 *
 * ## Thread safety
 *
 * [onMediaButtonEvent] and [onButtonDown] / [onButtonUp] are called from the service's
 * worker thread but all state mutations and callbacks are posted to the main thread via
 * the internal [Handler], so [onActionTriggered] always fires on the main thread.
 *
 * ## Usage
 *
 * ```kotlin
 * val engine = GestureEngine(
 *     settingsRepository = settingsRepository,
 *     onActionTriggered = { action -> executeAction(action) }
 * )
 *
 * // In MediaButtonService.processMediaButtonEvent():
 * engine.onMediaButtonEvent(event)
 *
 * // For long-press support, call onButtonDown / onButtonUp around the key event:
 * engine.onButtonDown(event)   // on KeyEvent.ACTION_DOWN
 * engine.onButtonUp(event)     // on KeyEvent.ACTION_UP
 * ```
 *
 * @param settingsRepository Source of the user's [GestureMapping] configuration.
 * @param onActionTriggered Called on the main thread when a gesture resolves to a
 *                          non-[CameraAction.NONE] action.
 * @param tapWindowMs       How long (ms) to wait for additional taps before committing.
 *                          Defaults to [TAP_WINDOW_MS] (300 ms).
 * @param longPressMs       How long (ms) a button must be held to count as a long press.
 *                          Defaults to [LONG_PRESS_THRESHOLD_MS] (600 ms).
 */
class GestureEngine(
    private val settingsRepository: SettingsRepository,
    private val onActionTriggered: (CameraAction) -> Unit,
    private val tapWindowMs: Long = TAP_WINDOW_MS,
    private val longPressMs: Long = LONG_PRESS_THRESHOLD_MS
) {

    companion object {
        private const val TAG = "GestureEngine"

        /** Time budget (ms) to collect additional taps into a multi-tap gesture. */
        const val TAP_WINDOW_MS = 300L

        /**
         * How long a button must be held before it is classified as a long press.
         * Should be meaningfully longer than [TAP_WINDOW_MS] so a slow double-tap
         * never accidentally fires LONG_PRESS.
         */
        const val LONG_PRESS_THRESHOLD_MS = 600L

        /** Maximum tap count the engine will track (TRIPLE_TAP = 3). */
        private const val MAX_TAPS = 3
    }

    // ---------------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------------

    private val handler = Handler(Looper.getMainLooper())

    /** Number of taps accumulated in the current window. */
    @Volatile private var tapCount = 0

    /** True while a button is being held down (for long-press tracking). */
    @Volatile private var isButtonHeld = false

    /**
     * True after a long press fires, so we suppress the tap that would otherwise
     * follow on button-up.
     */
    @Volatile private var longPressFired = false

    /**
     * Runnable posted to [handler] with a [tapWindowMs] delay. When it fires it
     * resolves the current [tapCount] into a [GestureType].
     */
    private val commitGestureRunnable = Runnable { commitGesture() }

    /**
     * Runnable posted to [handler] with a [longPressMs] delay on button-down.
     * If the button is still held when it fires, we emit LONG_PRESS.
     */
    private val longPressRunnable = Runnable {
        if (isButtonHeld) {
            Log.d(TAG, "Long press detected")
            longPressFired = true
            // Cancel any pending tap-window commit — long press wins.
            handler.removeCallbacks(commitGestureRunnable)
            tapCount = 0
            dispatchGesture(GestureType.LONG_PRESS)
        }
    }

    // ---------------------------------------------------------------------------
    // Public API — simple (event-only) path
    // ---------------------------------------------------------------------------

    /**
     * Primary entry point when only [MediaButtonEvent] data is available (no raw
     * [android.view.KeyEvent] access). This path supports tap counting but cannot
     * detect long press because it has no button-up signal.
     *
     * Call this from [MediaButtonService.processMediaButtonEvent].
     */
    fun onMediaButtonEvent(event: MediaButtonEvent) {
        // Ignore UNKNOWN events — they carry no gesture intent.
        if (event.eventType == MediaButtonType.UNKNOWN) {
            Log.d(TAG, "Ignoring UNKNOWN media button event")
            return
        }

        Log.d(TAG, "Button event received: ${event.eventType.displayName}")

        handler.post {
            // Cancel the pending commit — we're extending the tap window.
            handler.removeCallbacks(commitGestureRunnable)

            tapCount = (tapCount + 1).coerceAtMost(MAX_TAPS)
            Log.d(TAG, "Tap count now: $tapCount")

            // Schedule commit after the tap window expires.
            handler.postDelayed(commitGestureRunnable, tapWindowMs)
        }
    }

    // ---------------------------------------------------------------------------
    // Public API — advanced (KeyEvent) path with long-press support
    // ---------------------------------------------------------------------------

    /**
     * Call this on [android.view.KeyEvent.ACTION_DOWN] when raw KeyEvent access is
     * available (e.g. from [MediaButtonService.handleKeyEvent]). Starts the long-press
     * detection timer.
     */
    fun onButtonDown(event: MediaButtonEvent) {
        if (event.eventType == MediaButtonType.UNKNOWN) return

        handler.post {
            isButtonHeld = true
            longPressFired = false
            // Schedule long-press detection.
            handler.removeCallbacks(longPressRunnable)
            handler.postDelayed(longPressRunnable, longPressMs)
        }
    }

    /**
     * Call this on [android.view.KeyEvent.ACTION_UP]. Cancels the long-press timer
     * and, if a long press has not already fired, counts the release as a tap.
     */
    fun onButtonUp(event: MediaButtonEvent) {
        if (event.eventType == MediaButtonType.UNKNOWN) return

        handler.post {
            isButtonHeld = false
            handler.removeCallbacks(longPressRunnable)

            if (longPressFired) {
                // Long press already dispatched — swallow this release.
                Log.d(TAG, "Button up after long press — suppressed")
                longPressFired = false
                return@post
            }

            // Count it as a tap and restart the commit window.
            handler.removeCallbacks(commitGestureRunnable)
            tapCount = (tapCount + 1).coerceAtMost(MAX_TAPS)
            Log.d(TAG, "Tap count on button-up: $tapCount")
            handler.postDelayed(commitGestureRunnable, tapWindowMs)
        }
    }

    // ---------------------------------------------------------------------------
    // Internal — gesture resolution
    // ---------------------------------------------------------------------------

    /**
     * Called when the tap window expires. Resolves [tapCount] into a [GestureType],
     * looks up the mapped [CameraAction], and fires [onActionTriggered] if the action
     * is not [CameraAction.NONE].
     */
    private fun commitGesture() {
        val count = tapCount
        tapCount = 0

        if (count == 0) return

        val gestureType = when (count) {
            1    -> GestureType.SINGLE_TAP
            2    -> GestureType.DOUBLE_TAP
            else -> GestureType.TRIPLE_TAP   // 3 or more → TRIPLE_TAP
        }

        Log.d(TAG, "Committing gesture: ${gestureType.displayName} (from $count tap(s))")
        dispatchGesture(gestureType)
    }

    /**
     * Looks up the [CameraAction] for [gestureType] in [SettingsRepository] and
     * dispatches [onActionTriggered] unless the action is [CameraAction.NONE].
     */
    private fun dispatchGesture(gestureType: GestureType) {
        // SettingsRepository reads from SharedPreferences — do it on the main thread
        // here since the prefs file is tiny and we're already on the handler thread.
        val mappings: List<GestureMapping> = try {
            settingsRepository.getGestureMappings()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load gesture mappings", e)
            return
        }

        val mapping = mappings.firstOrNull { it.gestureType == gestureType }
        val action = mapping?.cameraAction ?: CameraAction.NONE

        Log.d(TAG, "Gesture ${gestureType.displayName} → Action: ${action.displayName}")

        if (action == CameraAction.NONE) {
            Log.d(TAG, "No action mapped to ${gestureType.displayName}, ignoring")
            return
        }

        onActionTriggered(action)
    }

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    /**
     * Cancel all pending runnables. Call this from [MediaButtonService.onDestroy]
     * to avoid leaking callbacks after the service is torn down.
     */
    fun release() {
        handler.removeCallbacks(commitGestureRunnable)
        handler.removeCallbacks(longPressRunnable)
        tapCount = 0
        isButtonHeld = false
        longPressFired = false
        Log.d(TAG, "GestureEngine released")
    }
}