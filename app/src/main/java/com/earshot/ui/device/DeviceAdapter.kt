package com.earshot.ui.device

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.earshot.R
import com.earshot.databinding.ItemDeviceBinding
import com.earshot.model.BluetoothDevice

/**
 * RecyclerView Adapter for displaying Bluetooth devices.
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
                tvDeviceName.text = device.name
                tvDeviceAddress.text = device.address

                // Set status text and color
                tvStatus.text = if (device.isConnected) {
                    root.context.getString(R.string.device_connected)
                } else {
                    root.context.getString(R.string.device_disconnected)
                }

                // Set action button text
                btnAction.text = when {
                    device.isConnected -> root.context.getString(R.string.device_unpair)
                    device.isPaired -> root.context.getString(R.string.device_connected)
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