package com.earshot.ui.gesture

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.earshot.R
import com.earshot.databinding.ItemGestureBinding
import com.earshot.model.CameraAction
import com.earshot.model.GestureMapping
import com.earshot.model.GestureType

/**
 * RecyclerView Adapter for displaying gesture mappings.
 */
class GestureAdapter(
    private val availableActions: List<CameraAction>,
    private val onActionSelected: (GestureType, CameraAction) -> Unit
) : ListAdapter<GestureMapping, GestureAdapter.GestureViewHolder>(GestureDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureViewHolder {
        val binding = ItemGestureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GestureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GestureViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GestureViewHolder(
        private val binding: ItemGestureBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mapping: GestureMapping) {
            binding.apply {
                tvGestureName.text = mapping.gestureType.displayName

                // Setup dropdown with available actions
                val actionNames = availableActions.map { it.displayName }
                val adapter = ArrayAdapter(
                    root.context,
                    android.R.layout.simple_dropdown_item_1line,
                    actionNames
                )
                actvAction.setAdapter(adapter)

                // Set current selection
                actvAction.setText(mapping.cameraAction.displayName, false)

                // Handle selection changes
                actvAction.setOnItemClickListener { _, _, position, _ ->
                    val selectedAction = availableActions[position]
                    onActionSelected(mapping.gestureType, selectedAction)
                }
            }
        }
    }

    private class GestureDiffCallback : DiffUtil.ItemCallback<GestureMapping>() {
        override fun areItemsTheSame(oldItem: GestureMapping, newItem: GestureMapping): Boolean {
            return oldItem.gestureType == newItem.gestureType
        }

        override fun areContentsTheSame(oldItem: GestureMapping, newItem: GestureMapping): Boolean {
            return oldItem == newItem
        }
    }
}