package com.earshot.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.earshot.camera.CameraPhotoOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing gallery/media operations.
 * Provides access to captured photos and videos from the MediaStore.
 *
 * This class handles:
 * - Querying all captured media from the MediaStore
 * - Getting media item details (metadata)
 * - Deleting media from the gallery
 * - Opening media in external apps
 *
 * ## Storage Locations
 *
 * Photos: DCIM/EarShot/
 * Videos: Movies/EarShot/
 *
 * ## Usage
 *
 * ```kotlin
 * val repository = GalleryRepository(context)
 *
 * // Get all captured media
 * val mediaItems = repository.getAllMedia()
 *
 * // Get specific media details
 * val details = repository.getMediaDetails(uri)
 * ```
 */
class GalleryRepository(private val context: Context) {

    companion object {
        private const val TAG = "GalleryRepository"
    }

    private val photoOutput = CameraPhotoOutput(context)

    /**
     * Get all captured media (photos and videos) from the MediaStore.
     *
     * @param limit Maximum number of items to return
     * @return List of media items sorted by date (newest first)
     */
    suspend fun getAllMedia(limit: Int = 50): List<MediaItem> = withContext(Dispatchers.IO) {
        val allMedia = mutableListOf<MediaItem>()

        // Get photos
        getPhotos(limit).forEach { uri ->
            allMedia.add(MediaItem(
                uri = uri,
                type = MediaType.PHOTO,
                dateAdded = getDateAdded(uri, MediaType.PHOTO)
            ))
        }

        // Get videos
        getVideos(limit).forEach { uri ->
            allMedia.add(MediaItem(
                uri = uri,
                type = MediaType.VIDEO,
                dateAdded = getDateAdded(uri, MediaType.VIDEO)
            ))
        }

        // Sort by date (newest first)
        allMedia.sortedByDescending { it.dateAdded }.take(limit)
    }

    /**
     * Get all captured photos.
     */
    suspend fun getPhotos(limit: Int = 50): List<Uri> = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%EarShot%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val photos = mutableListOf<Uri>()
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val id = cursor.getLong(idColumn)
                    photos.add(ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ))
                    count++
                }
            }
            photos
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get photos", e)
            emptyList()
        }
    }

    /**
     * Get all captured videos.
     */
    suspend fun getVideos(limit: Int = 50): List<Uri> = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%EarShot%")
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            val videos = mutableListOf<Uri>()
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val id = cursor.getLong(idColumn)
                    videos.add(ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    ))
                    count++
                }
            }
            videos
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get videos", e)
            emptyList()
        }
    }

    /**
     * Get details for a specific media item.
     */
    suspend fun getMediaDetails(uri: Uri): MediaDetails? = withContext(Dispatchers.IO) {
        try {
            val isVideo = uri.toString().contains("video")
            val mediaUri = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val projection = if (isVideo) {
                arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.SIZE
                )
            } else {
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.SIZE
                )
            }

            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = if (isVideo) {
                        cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    } else {
                        cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    }.let { col -> cursor.getString(col) }

                    val date = if (isVideo) {
                        cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                    } else {
                        cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    }.let { col -> cursor.getLong(col) }

                    val size = if (isVideo) {
                        cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                    } else {
                        cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                    }.let { col -> cursor.getLong(col) }

                    val duration = if (isVideo) {
                        cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                            .let { col -> cursor.getLong(col) }
                    } else 0L

                    MediaDetails(
                        uri = uri,
                        name = name,
                        dateAdded = date,
                        size = size,
                        durationMs = duration,
                        isVideo = isVideo
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get media details", e)
            null
        }
    }

    /**
     * Delete a media item from the gallery.
     *
     * @param uri The URI of the media to delete
     * @return true if deletion was successful
     */
    suspend fun deleteMedia(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleted = context.contentResolver.delete(uri, null, null) > 0
            if (deleted) {
                Log.d(TAG, "Deleted media: $uri")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete media", e)
            false
        }
    }

    /**
     * Check if any media exists in the EarShot folder.
     */
    suspend fun hasMedia(): Boolean = withContext(Dispatchers.IO) {
        val photos = getPhotos(1)
        val videos = getVideos(1)
        photos.isNotEmpty() || videos.isNotEmpty()
    }

    /**
     * Get the count of all captured media.
     */
    suspend fun getMediaCount(): Int = withContext(Dispatchers.IO) {
        val photos = getPhotos(Int.MAX_VALUE)
        val videos = getVideos(Int.MAX_VALUE)
        photos.size + videos.size
    }

    /**
     * Get the last captured photo.
     */
    fun getLastPhoto(): Uri? = photoOutput.getLastCapturedPhoto()

    /**
     * Get the last captured video.
     */
    fun getLastVideo(): Uri? = photoOutput.getLastCapturedVideo()

    private fun getDateAdded(uri: Uri, type: MediaType): Long {
        return try {
            val mediaUri = if (type == MediaType.VIDEO) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                if (type == MediaType.VIDEO) MediaStore.Video.Media.DATE_ADDED
                else MediaStore.Images.Media.DATE_ADDED
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(0)
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Data class representing a media item.
     */
    data class MediaItem(
        val uri: Uri,
        val type: MediaType,
        val dateAdded: Long = 0L
    )

    /**
     * Enum for media type.
     */
    enum class MediaType {
        PHOTO,
        VIDEO
    }

    /**
     * Data class representing media details.
     */
    data class MediaDetails(
        val uri: Uri,
        val name: String,
        val dateAdded: Long,
        val size: Long,
        val durationMs: Long = 0L,
        val isVideo: Boolean
    )
}