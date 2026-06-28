package com.earshot.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Handles Bluetooth runtime permissions for different Android versions.
 *
 * ## Permission Requirements
 *
 * - **Android 12+ (API 31+)**: BLUETOOTH_SCAN, BLUETOOTH_CONNECT
 * - **Android 10-11 (API 29-30)**: ACCESS_FINE_LOCATION
 * - **Android 9 and below**: ACCESS_FINE_LOCATION
 *
 * ## Usage
 *
 * ```kotlin
 * val permissionManager = BluetoothPermissionManager(context)
 *
 * // Check if scanning is allowed
 * if (permissionManager.hasScanPermissions()) {
 *     // Start scanning
 * }
 *
 * // Get required permissions for request
 * val permissions = permissionManager.getScanPermissions()
 * ```
 */
class BluetoothPermissionManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothPermissionMgr"
    }

    /**
     * Check if the app has all required permissions for Bluetooth scanning.
     */
    fun hasScanPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                hasPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+ (API 31-32)
                hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-11
                hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                // Android 9 and below
                hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * Check if the app has permission to connect to Bluetooth devices.
     */
    fun hasConnectPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Not required for API < 31
            true
        }
    }

    /**
     * Check if the app has permission to advertise (for connecting).
     */
    fun hasAdvertisePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            // Not required for API < 31
            true
        }
    }

    /**
     * Get the list of permissions needed for Bluetooth scanning.
     * Use this to request permissions from the user.
     */
    fun getScanPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+ (API 31-32)
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-11
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                // Android 9 and below
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * Get only the permissions that are not yet granted.
     * Useful for requesting only missing permissions.
     */
    fun getMissingScanPermissions(): List<String> {
        return getScanPermissions().filter { !hasPermission(it) }
    }

    /**
     * Check if a specific permission is granted.
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if nearby devices permission is needed (Android 13+).
     * This is an optional permission for BLE scanning on Android 13+.
     */
    fun needsNearbyDevicesPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Get the nearby devices permission for Android 13+.
     */
    fun getNearbyDevicesPermission(): String? {
        return if (needsNearbyDevicesPermission()) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            null
        }
    }
}