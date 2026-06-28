package com.earshot.ui.gallery

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.earshot.R
import com.earshot.repository.GalleryRepository

/**
 * Adapter for displaying gallery media items in a grid.
 *
 * Displays thumbnail previews of photos and videos with
 * video indicator overlay for video items.
 */
class GalleryAdapter(
    private val onItemClick: (GalleryRepository.MediaItem) -> Unit,
    private val onItemLongClick: (GalleryRepository.MediaItem) -> Boolean
) : ListAdapter<GalleryRepository.MediaItem, GalleryAdapter.ViewHolder>(MediaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val ivVideoIndicator: ImageView = itemView.findViewById(R.id.ivVideoIndicator)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)

        fun bind(item: GalleryRepository.MediaItem) {
            // Load thumbnail
            ivThumbnail.setImageURI(item.uri)

            // Show video indicator
            ivVideoIndicator.visibility = if (item.type == GalleryRepository.MediaType.VIDEO) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Duration text - we'll update this with actual duration when available
            tvDuration.visibility = if (item.type == GalleryRepository.MediaType.VIDEO) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Click listeners
            itemView.setOnClickListener {
                onItemClick(item)
            }

            itemView.setOnLongClickListener {
                onItemLongClick(item)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    class MediaDiffCallback : DiffUtil.ItemCallback<GalleryRepository.MediaItem>() {
        override fun areItemsTheSame(
            oldItem: GalleryRepository.MediaItem,
            newItem: GalleryRepository.MediaItem
        ): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(
            oldItem: GalleryRepository.MediaItem,
            newItem: GalleryRepository.MediaItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}