package com.earshot.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.earshot.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for the Home Screen.
 *
 * Validates:
 * - Home screen loads successfully
 * - Bluetooth Status section is displayed
 * - Quick Actions section exists
 * - Bottom Navigation is present
 * - App does not crash on load
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HomeScreenTest {

    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        // Launch the activity and stay on Home screen
        // The activity starts at homeFragment by default in nav_graph
    }

    /**
     * Test that the Home screen loads without crashing.
     * This verifies the basic activity and fragment lifecycle work correctly.
     */
    @Test
    fun testHomeScreenLoads() {
        // Verify the main container is displayed (basic sanity check)
        onView(withId(R.id.container)).check(matches(isDisplayed()))
    }

    /**
     * Test that the app title is displayed on the Home screen.
     */
    @Test
    fun testAppTitleDisplayed() {
        // Verify the app title is shown
        onView(withId(R.id.tvTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.app_name)))
    }

    /**
     * Test that the Bluetooth Status section is displayed.
     * The status card contains the Bluetooth icon and status text.
     */
    @Test
    fun testBluetoothStatusDisplayed() {
        // Verify the status card is displayed
        onView(withId(R.id.cardStatus)).check(matches(isDisplayed()))

        // Verify the status label text is displayed
        onView(withText(R.string.home_status_label)).check(matches(isDisplayed()))

        // Verify the status text (shows "Not Connected" or "Connected")
        onView(withId(R.id.tvStatus)).check(matches(isDisplayed()))
    }

    /**
     * Test that the Quick Actions section exists.
     */
    @Test
    fun testQuickActionsSectionExists() {
        // Verify the Quick Actions title is displayed
        onView(withText(R.string.home_quick_actions))
            .check(matches(isDisplayed()))
    }

    /**
     * Test that the Quick Action cards are present.
     */
    @Test
    fun testQuickActionCardsDisplayed() {
        // Verify Connect Device card is displayed
        onView(withId(R.id.btnConnectDevice)).check(matches(isDisplayed()))

        // Verify the Connect Device text is displayed
        onView(withText(R.string.home_connect_device)).check(matches(isDisplayed()))

        // Verify Map Gestures card is displayed
        onView(withId(R.id.btnMapGestures)).check(matches(isDisplayed()))

        // Verify the Map Gestures text is displayed
        onView(withText(R.string.home_map_gestures)).check(matches(isDisplayed()))
    }

    /**
     * Test that the Bottom Navigation exists and is displayed.
     */
    @Test
    fun testBottomNavigationExists() {
        // Verify bottom navigation is displayed
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
    }

    /**
     * Test that the status indicator (dot) is displayed.
     */
    @Test
    fun testStatusIndicatorDisplayed() {
        // Verify the status indicator dot is displayed
        onView(withId(R.id.statusIndicator)).check(matches(isDisplayed()))
    }

    /**
     * Comprehensive test verifying all key Home screen elements.
     */
    @Test
    fun testHomeScreenComprehensive() {
        // 1. Verify container loads
        onView(withId(R.id.container)).check(matches(isDisplayed()))

        // 2. Verify app title
        onView(withId(R.id.tvTitle)).check(matches(isDisplayed()))

        // 3. Verify Bluetooth status section
        onView(withId(R.id.cardStatus)).check(matches(isDisplayed()))
        onView(withId(R.id.tvStatus)).check(matches(isDisplayed()))

        // 4. Verify Quick Actions section
        onView(withText(R.string.home_quick_actions)).check(matches(isDisplayed()))

        // 5. Verify Quick Action cards
        onView(withId(R.id.btnConnectDevice)).check(matches(isDisplayed()))
        onView(withId(R.id.btnMapGestures)).check(matches(isDisplayed()))

        // 6. Verify Bottom Navigation
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
    }
}