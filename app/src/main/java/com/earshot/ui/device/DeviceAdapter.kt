package com.earshot.ui.device

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.earshot.R
import com.earshot.bluetooth.BluetoothDevice
import com.earshot.databinding.ItemDeviceBinding

/**
 * RecyclerView Adapter for displaying Bluetooth devices.
 *
 * This adapter shows:
 * - Device name and MAC address
 * - Connection/paired status
 * - Action button (Pair/Connect/Disconnect)
 */
class DeviceAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit,
    private val onActionClick: (BluetoothDevice) -> Unit
) : ListAdapter<BluetoothDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(
        private val binding: ItemDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: BluetoothDevice) {
            binding.apply {
                // Device name
                tvDeviceName.text = device.getDisplayName()

                // MAC address
                tvDeviceAddress.text = device.address

                // Status based on connection state
                when {
                    device.isConnected -> {
                        tvStatus.text = root.context.getString(R.string.device_connected)
                        tvStatus.isVisible = true
                    }
                    device.isPaired -> {
                        // Paired but not connected
                        tvStatus.isVisible = false
                    }
                    else -> {
                        // Available but not paired
                        tvStatus.isVisible = false
                    }
                }

                // Action button
                btnAction.text = when {
                    device.isConnected -> root.context.getString(R.string.device_unpair)
                    device.isPaired -> root.context.getString(R.string.device_connect)
                    else -> root.context.getString(R.string.device_pair)
                }

                // Click listeners
                root.setOnClickListener { onDeviceClick(device) }
                btnAction.setOnClickListener { onActionClick(device) }
            }
        }
    }

    private class DeviceDiffCallback : DiffUtil.ItemCallback<BluetoothDevice>() {
        override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            return oldItem == newItem
        }
    }
}