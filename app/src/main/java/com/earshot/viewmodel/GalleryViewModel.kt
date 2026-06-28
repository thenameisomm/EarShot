package com.earshot.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.earshot.repository.GalleryRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Gallery functionality.
 * Manages media queries and gallery operations.
 *
 * ## Features
 *
 * - Load all captured photos and videos
 * - Get media details
 * - Delete media
 * - Check for media existence
 *
 * ## Usage
 *
 * ```kotlin
 * val viewModel = ViewModelProvider(this, GalleryViewModel.Factory())
 *     .get(GalleryViewModel::class.java)
 *
 * // Observe media items
 * viewModel.mediaItems.observe(viewLifecycleOwner) { items ->
 *     // Update adapter
 * }
 * ```
 */
class GalleryViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "GalleryViewModel"
    }

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private val repository = GalleryRepository(application.applicationContext)

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private val _mediaItems = MutableLiveData<List<GalleryRepository.MediaItem>>()
    val mediaItems: LiveData<List<GalleryRepository.MediaItem>> = _mediaItems

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _hasMedia = MutableLiveData<Boolean>(false)
    val hasMedia: LiveData<Boolean> = _hasMedia

    private val _mediaCount = MutableLiveData<Int>(0)
    val mediaCount: LiveData<Int> = _mediaCount

    private val _selectedItem = MutableLiveData<GalleryRepository.MediaItem?>()
    val selectedItem: LiveData<GalleryRepository.MediaItem?> = _selectedItem

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _deleteSuccess = MutableLiveData<Boolean?>()
    val deleteSuccess: LiveData<Boolean?> = _deleteSuccess

    // -----------------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------------

    init {
        checkMediaExists()
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Load all captured media from the gallery.
     */
    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val items = repository.getAllMedia()
                _mediaItems.value = items
                _mediaCount.value = items.size
                _hasMedia.value = items.isNotEmpty()
                _error.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load media", e)
                _error.value = "Failed to load media: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if any media exists.
     */
    fun checkMediaExists() {
        viewModelScope.launch {
            try {
                _hasMedia.value = repository.hasMedia()
                _mediaCount.value = repository.getMediaCount()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check media existence", e)
            }
        }
    }

    /**
     * Get details for a specific media item.
     */
    fun getMediaDetails(uri: Uri) {
        viewModelScope.launch {
            try {
                val details = repository.getMediaDetails(uri)
                Log.d(TAG, "Media details: $details")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get media details", e)
            }
        }
    }

    /**
     * Select a media item.
     */
    fun selectItem(item: GalleryRepository.MediaItem?) {
        _selectedItem.value = item
    }

    /**
     * Delete a media item.
     */
    fun deleteMedia(item: GalleryRepository.MediaItem) {
        viewModelScope.launch {
            try {
                val success = repository.deleteMedia(item.uri)
                _deleteSuccess.value = success
                if (success) {
                    // Remove from the list
                    val currentList = _mediaItems.value?.toMutableList() ?: mutableListOf()
                    currentList.remove(item)
                    _mediaItems.value = currentList
                    _mediaCount.value = currentList.size
                    _hasMedia.value = currentList.isNotEmpty()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete media", e)
                _error.value = "Failed to delete media: ${e.message}"
                _deleteSuccess.value = false
            }
        }
    }

    /**
     * Clear the selected item.
     */
    fun clearSelection() {
        _selectedItem.value = null
    }

    /**
     * Clear the delete success status.
     */
    fun clearDeleteStatus() {
        _deleteSuccess.value = null
    }

    /**
     * Clear the error.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Get the last captured photo URI.
     */
    fun getLastPhoto(): Uri? = repository.getLastPhoto()

    /**
     * Get the last captured video URI.
     */
    fun getLastVideo(): Uri? = repository.getLastVideo()

    /**
     * Open media in an external app (gallery viewer).
     */
    fun openMedia(item: GalleryRepository.MediaItem): Boolean {
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, if (item.type == GalleryRepository.MediaType.VIDEO) "video/*" else "image/*")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            getApplication<Application>().startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open media", e)
            _error.value = "No app available to view this media"
            false
        }
    }

    /**
     * Factory for creating GalleryViewModel.
     */
    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
                return GalleryViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}