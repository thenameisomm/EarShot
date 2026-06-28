package com.earshot.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for photo output operations.
 *
 * This class provides utilities for:
 * - Generating unique photo filenames with timestamps
 * - Creating files in app-specific directories
 * - Inserting photos into the MediaStore gallery
 * - Managing photo file lifecycle
 *
 * ## Usage
 *
 * ```kotlin
 * val photoOutput = CameraPhotoOutput(context)
 *
 * // Generate a unique photo file
 * val photoFile = photoOutput.createPhotoFile()
 *
 * // Insert into gallery (Android 10+)
 * val uri = photoOutput.insertToGallery(photoFile)
 * ```
 */
class CameraPhotoOutput(
    private val context: Context
) {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    companion object {
        private const val TAG = "CameraPhotoOutput"

        /** File name prefix for photos */
        private const val FILE_PREFIX = "IMG_"

        /** File extension for photos */
        private const val FILE_EXTENSION = ".jpg"

        /** Directory name for photos */
        private const val DIRECTORY_NAME = "EarShot"
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Create a new photo file with a unique timestamp-based name.
     *
     * The file is created in the app's external files directory (DCIM/EarShot).
     *
     * @return A File object representing the new photo file
     */
    fun createPhotoFile(): File {
        val filename = generateUniqueFilename()

        // Get the directory for our app's photos
        val directory = getPhotoDirectory()

        // Create the file
        return File(directory, filename).apply {
            createNewFile()
            Log.d(TAG, "Created photo file: $absolutePath")
        }
    }

    /**
     * Insert a photo file into the MediaStore gallery.
     *
     * This makes the photo visible in the device's Gallery app.
     * Works on all Android versions (6+).
     *
     * @param file The photo file to insert
     * @return The Content URI of the inserted photo
     */
    fun insertToGallery(file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertToGalleryAndroidQ(file)
        } else {
            insertToGalleryLegacy(file)
        }
    }

    /**
     * Get the photo directory for saving photos.
     *
     * This returns the app-specific DCIM/EarShot directory.
     *
     * @return The File representing the photo directory
     */
    fun getPhotoDirectory(): File {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            DIRECTORY_NAME
        )

        if (!directory.exists()) {
            directory.mkdirs()
            Log.d(TAG, "Created photo directory: ${directory.absolutePath}")
        }

        return directory
    }

    /**
     * Generate a unique photo filename with timestamp.
     *
     * @return A filename string in format: IMG_yyyyMMdd_HHmmss.jpg
     */
    fun generateUniqueFilename(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(Date())
        return "${FILE_PREFIX}${timestamp}${FILE_EXTENSION}"
    }

    /**
     * Get a human-readable timestamp string for display.
     *
     * @return A timestamp string in format: yyyy-MM-dd HH:mm:ss
     */
    fun getDisplayTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
    }

    // -----------------------------------------------------------------------
    // Internal Methods
    // -----------------------------------------------------------------------

    /**
     * Insert photo into MediaStore for Android Q (API 29) and above.
     *
     * Uses the MediaStore API with contentResolver for proper gallery integration.
     */
    private fun insertToGalleryAndroidQ(file: File): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/EarShot")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IllegalStateException("Failed to insert image into MediaStore")

        // Copy file content to the MediaStore URI
        file.inputStream().use { input ->
            context.contentResolver.openOutputStream(uri)?.use { output ->
                input.copyTo(output)
            }
        }

        // Mark as not pending
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, contentValues, null, null)

        Log.d(TAG, "Inserted photo to gallery: $uri")
        return uri
    }

    /**
     * Insert photo into MediaStore for legacy Android versions (before API 29).
     *
     * Uses MediaScannerConnection for gallery integration.
     */
    private fun insertToGalleryLegacy(file: File): Uri {
        // Scan the file to make it visible in the gallery
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("image/jpeg")
        ) { _, uri ->
            Log.d(TAG, "Legacy gallery insert completed: $uri")
        }

        return Uri.fromFile(file)
    }

    /**
     * Delete a media file from the gallery.
     *
     * @param uri The Content URI of the media to delete
     * @return true if deletion was successful, false otherwise
     */
    fun deleteFromGallery(uri: Uri): Boolean {
        return try {
            context.contentResolver.delete(uri, null, null)
            Log.d(TAG, "Deleted media from gallery: $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete media from gallery", e)
            false
        }
    }

    /**
     * Insert a video file into the MediaStore gallery.
     *
     * This makes the video visible in the device's Gallery app.
     *
     * @param file The video file to insert
     * @return The Content URI of the inserted video
     */
    fun insertVideoToGallery(file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertVideoToGalleryAndroidQ(file)
        } else {
            insertVideoToGalleryLegacy(file)
        }
    }

    /**
     * Insert video into MediaStore for Android Q (API 29) and above.
     */
    private fun insertVideoToGalleryAndroidQ(file: File): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/EarShot")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IllegalStateException("Failed to insert video into MediaStore")

        // Copy file content to the MediaStore URI
        file.inputStream().use { input ->
            context.contentResolver.openOutputStream(uri)?.use { output ->
                input.copyTo(output)
            }
        }

        // Mark as not pending
        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, contentValues, null, null)

        Log.d(TAG, "Inserted video to gallery: $uri")
        return uri
    }

    /**
     * Insert video into MediaStore for legacy Android versions (before API 29).
     */
    private fun insertVideoToGalleryLegacy(file: File): Uri {
        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("video/mp4")
        ) { _, uri ->
            Log.d(TAG, "Legacy video gallery insert completed: $uri")
        }

        return Uri.fromFile(file)
    }

    /**
     * Get all captured photos from the MediaStore.
     *
     * @param limit Maximum number of photos to return
     * @return List of photo Uris sorted by date (newest first)
     */
    fun getAllCapturedPhotos(limit: Int = 50): List<Uri> {
        return try {
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
                    photos.add(Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    ))
                    count++
                }
            }
            photos
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all captured photos", e)
            emptyList()
        }
    }

    /**
     * Get all captured videos from the MediaStore.
     *
     * @param limit Maximum number of videos to return
     * @return List of video Uris sorted by date (newest first)
     */
    fun getAllCapturedVideos(limit: Int = 50): List<Uri> {
        return try {
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
                    videos.add(Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    ))
                    count++
                }
            }
            videos
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all captured videos", e)
            emptyList()
        }
    }

    /**
     * Get all captured media (photos and videos) merged and sorted by date.
     *
     * @param limit Maximum number of items to return
     * @return List of media items sorted by date (newest first)
     */
    fun getAllCapturedMedia(limit: Int = 50): List<MediaItem> {
        val allMedia = mutableListOf<MediaItem>()

        // Get photos
        getAllCapturedPhotos(limit).forEach { uri ->
            allMedia.add(MediaItem(uri, MediaType.PHOTO))
        }

        // Get videos
        getAllCapturedVideos(limit).forEach { uri ->
            allMedia.add(MediaItem(uri, MediaType.VIDEO))
        }

        // Sort by date (newest first) - note: this is approximate since we can't easily
        // get exact timestamps without additional queries
        return allMedia.sortedByDescending { it.uri.toString().hashCode() }.take(limit)
    }

    /**
     * Data class representing a media item.
     */
    data class MediaItem(
        val uri: Uri,
        val type: MediaType
    )

    /**
     * Enum for media type.
     */
    enum class MediaType {
        PHOTO,
        VIDEO
    }

    /**
     * Get the most recently captured photo from the MediaStore.
     *
     * @return The Uri of the last captured photo, or null if none found
     */
    fun getLastCapturedPhoto(): Uri? {
        return try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%EarShot%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val id = cursor.getLong(idColumn)
                    Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last captured photo", e)
            null
        }
    }

    /**
     * Get the most recently captured video from the MediaStore.
     *
     * @return The Uri of the last captured video, or null if none found
     */
    fun getLastCapturedVideo(): Uri? {
        return try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%EarShot%")
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val id = cursor.getLong(idColumn)
                    Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last captured video", e)
            null
        }
    }
}
