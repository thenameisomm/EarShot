package com.earshot.model

import com.earshot.bluetooth.DeviceType

/**
 * Legacy Bluetooth device model.
 *
 * @deprecated Use [com.earshot.bluetooth.BluetoothDevice] instead.
 * This class is kept for backward compatibility.
 */
@Deprecated(
    message = "Use com.earshot.bluetooth.BluetoothDevice instead",
    replaceWith = ReplaceWith(
        "com.earshot.bluetooth.BluetoothDevice",
        "com.earshot.bluetooth.BluetoothDevice"
    )
)
typealias BluetoothDevice = com.earshot.bluetooth.BluetoothDevice

/**
 * Legacy connection state enum.
 *
 * @deprecated Use [com.earshot.bluetooth.ConnectionState] instead.
 */
@Deprecated(
    message = "Use com.earshot.bluetooth.ConnectionState instead",
    replaceWith = ReplaceWith(
        "com.earshot.bluetooth.ConnectionState",
        "com.earshot.bluetooth.ConnectionState"
    )
)
typealias ConnectionState = com.earshot.bluetooth.ConnectionState

/**
 * Legacy device type enum.
 *
 * @deprecated Use [com.earshot.bluetooth.DeviceType] instead.
 */
@Deprecated(
    message = "Use com.earshot.bluetooth.DeviceType instead",
    replaceWith = ReplaceWith(
        "com.earshot.bluetooth.DeviceType",
        "com.earshot.bluetooth.DeviceType"
    )
)
typealias DeviceType = com.earshot.bluetooth.DeviceType