package com.earshot.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.earshot.R
import com.earshot.databinding.ActivityMainBinding

/**
 * Main Activity that hosts the navigation component and bottom navigation bar.
 * Manages the overall app navigation flow with edge-to-edge support.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge - content extends behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupEdgeToEdge()
    }

    /**
     * Set up edge-to-edge display.
     * This allows content to extend behind system bars (status bar, navigation bar).
     */
    private fun setupEdgeToEdge() {
        // Apply windowInsets to the container layout
        binding.container.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insets = windowInsets.getInsets(
                android.view.WindowInsets.Type.systemBars()
            )

            // Apply top inset to the nav host fragment area
            // Bottom inset will be handled by the bottom navigation

            // Return the insets to be consumed
            windowInsets
        }

        // Set up the bottom navigation to account for navigation bar
        binding.bottomNavigation.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insets = windowInsets.getInsets(
                android.view.WindowInsets.Type.systemBars()
            )
            view.setPadding(0, 0, 0, insets.bottom)
            windowInsets
        }
    }

    /**
     * Set up the bottom navigation with the navigation component.
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
    }

    /**
     * Helper method to get status bar height for fragments.
     */
    fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }
}