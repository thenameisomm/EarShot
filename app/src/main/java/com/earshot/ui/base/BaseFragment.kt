package com.earshot.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ContentView
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment

/**
 * Base Fragment class that provides edge-to-edge support.
 * All fragments should extend this class to properly handle system bar insets.
 *
 * This class automatically applies WindowInsets to the root view of the fragment,
 * adding padding for the status bar (top) and navigation bar (bottom).
 */
open class BaseFragment : Fragment {

    constructor() : super()

    @ContentView
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply WindowInsets to handle status bar and navigation bar
        setupWindowInsets(view)
    }

    /**
     * Apply WindowInsets to the root view.
     * This handles:
     * - Status bar (top)
     * - Navigation bar (bottom)
     * - Display cutout/punch-hole (edges)
     *
     * Override this method in subclasses to customize inset handling.
     */
    protected open fun setupWindowInsets(view: View) {
        // Request WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            // Apply top inset as padding (for status bar)
            // Apply bottom inset for navigation bar
            // Apply left/right inset for display cutout
            v.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )

            // Return the insets to be consumed by other views
            WindowInsetsCompat.CONSUMED
        }

        // Trigger the insets to be applied
        ViewCompat.requestApplyInsets(view)
    }
}