package com.earshot.ui.gesture

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.earshot.databinding.ItemGestureBinding
import com.earshot.model.CameraAction
import com.earshot.model.GestureMapping
import com.earshot.model.GestureType

/**
 * RecyclerView Adapter for displaying gesture mappings.
 * Creates fresh adapter for each dropdown to ensure proper initialization.
 */
class GestureAdapter(
    private val availableActions: List<CameraAction>,
    private val onActionSelected: (GestureType, CameraAction) -> Unit,
    private val context: android.content.Context
) : ListAdapter<GestureMapping, GestureAdapter.GestureViewHolder>(GestureDiffCallback()) {

    // Pre-compute action names list once
    private val actionNames: List<String> = availableActions.map { it.displayName }

    init {
        // Enable stable IDs for better RecyclerView performance
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).gestureType.ordinal.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureViewHolder {
        val binding = ItemGestureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GestureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GestureViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    override fun onViewRecycled(holder: GestureViewHolder) {
        super.onViewRecycled(holder)
        holder.clearPosition()
    }

    inner class GestureViewHolder(
        private val binding: ItemGestureBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundPosition: Int = -1

        fun bind(mapping: GestureMapping, position: Int) {
            boundPosition = position

            binding.apply {
                tvGestureName.text = mapping.gestureType.displayName

                // Create a fresh adapter for each item to ensure dropdown works correctly
                // This avoids issues with RecyclerView view recycling where the adapter
                // might not be properly set on reused views
                val adapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    actionNames
                )
                actvAction.setAdapter(adapter)

                // Set current selection text
                val currentActionName = mapping.cameraAction.displayName
                if (actvAction.text.toString() != currentActionName) {
                    actvAction.setText(currentActionName, false)
                }

                // Set listener for selection changes
                actvAction.setOnItemClickListener { _, _, pos, _ ->
                    val selectedAction = availableActions.getOrNull(pos)
                    selectedAction?.let {
                        onActionSelected(mapping.gestureType, it)
                    }
                }
            }
        }

        fun clearPosition() {
            boundPosition = -1
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