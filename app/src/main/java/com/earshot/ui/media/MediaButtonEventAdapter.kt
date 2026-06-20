package com.earshot.ui.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.earshot.R
import com.earshot.databinding.ItemMediaEventBinding
import com.earshot.model.MediaButtonEvent
import com.earshot.model.MediaButtonType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView Adapter for displaying media button events.
 */
class MediaButtonEventAdapter :
    ListAdapter<MediaButtonEvent, MediaButtonEventAdapter.EventViewHolder>(EventDiffCallback()) {

    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemMediaEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(
        private val binding: ItemMediaEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: MediaButtonEvent) {
            binding.apply {
                tvEventName.text = event.eventType.displayName
                tvTimestamp.text = timeFormat.format(Date(event.timestamp))

                // Set icon based on event type
                val iconRes = when (event.eventType) {
                    MediaButtonType.PLAY_PAUSE -> R.drawable.ic_media_play_pause
                    MediaButtonType.NEXT_TRACK -> R.drawable.ic_media_next
                    MediaButtonType.PREVIOUS_TRACK -> R.drawable.ic_media_previous
                    MediaButtonType.UNKNOWN -> R.drawable.ic_media_button
                }
                ivEventIcon.setImageResource(iconRes)
            }
        }
    }

    private class EventDiffCallback : DiffUtil.ItemCallback<MediaButtonEvent>() {
        override fun areItemsTheSame(oldItem: MediaButtonEvent, newItem: MediaButtonEvent): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: MediaButtonEvent, newItem: MediaButtonEvent): Boolean {
            return oldItem == newItem
        }
    }
}