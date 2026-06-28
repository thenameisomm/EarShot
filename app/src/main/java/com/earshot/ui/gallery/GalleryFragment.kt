package com.earshot.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.earshot.R
import com.earshot.databinding.FragmentGalleryBinding
import com.earshot.repository.GalleryRepository
import com.earshot.ui.base.BaseFragment
import com.earshot.viewmodel.GalleryViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Gallery Fragment for viewing captured photos and videos.
 *
 * This fragment provides:
 * - Grid display of all captured media
 * - Tap to open media in external viewer
 * - Long press to delete media
 * - Pull to refresh
 * - Empty state handling
 *
 * ## Usage
 *
 * Navigate to this fragment from the bottom navigation.
 * Media is loaded from the MediaStore (DCIM/EarShot and Movies/EarShot).
 */
class GalleryFragment : BaseFragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GalleryViewModel by viewModels {
        GalleryViewModel.Factory(requireActivity().application)
    }

    private lateinit var galleryAdapter: GalleryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        setupClickListeners()

        // Load media
        viewModel.loadMedia()
    }

    override fun onResume() {
        super.onResume()
        // Refresh media when returning to this fragment
        viewModel.checkMediaExists()
        viewModel.loadMedia()
    }

    private fun setupRecyclerView() {
        galleryAdapter = GalleryAdapter(
            onItemClick = { item -> onMediaItemClick(item) },
            onItemLongClick = { item -> onMediaItemLongClick(item) }
        )

        binding.rvGallery.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = galleryAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMedia()
        }

        // Set refresh indicator colors
        binding.swipeRefresh.setColorSchemeResources(
            R.color.accent_purple,
            R.color.accent_orange,
            R.color.accent_teal
        )
    }

    private fun setupObservers() {
        // Observe media items
        viewModel.mediaItems.observe(viewLifecycleOwner) { items ->
            galleryAdapter.submitList(items)
            updateEmptyState(items.isEmpty())
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressIndicator.isVisible = isLoading && galleryAdapter.currentList.isEmpty()
        }

        // Observe media count
        viewModel.mediaCount.observe(viewLifecycleOwner) { count ->
            binding.tvMediaCount.text = getString(R.string.media_count, count)
        }

        // Observe has media
        viewModel.hasMedia.observe(viewLifecycleOwner) { hasMedia ->
            updateEmptyState(!hasMedia && galleryAdapter.currentList.isEmpty())
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observe delete success
        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                if (it) {
                    Snackbar.make(binding.root, R.string.media_deleted, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, R.string.media_delete_failed, Snackbar.LENGTH_LONG).show()
                }
                viewModel.clearDeleteStatus()
            }
        }
    }

    private fun setupClickListeners() {
        // Open camera button in empty state
        binding.btnOpenCamera.setOnClickListener {
            findNavController().navigate(R.id.cameraFragment)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.isVisible = isEmpty
        binding.rvGallery.isVisible = !isEmpty
    }

    private fun onMediaItemClick(item: GalleryRepository.MediaItem) {
        // Open media in external viewer
        viewModel.openMedia(item)
    }

    private fun onMediaItemLongClick(item: GalleryRepository.MediaItem): Boolean {
        // Show delete confirmation dialog
        showDeleteDialog(item)
        return true
    }

    private fun showDeleteDialog(item: GalleryRepository.MediaItem) {
        val mediaType = if (item.type == GalleryRepository.MediaType.VIDEO) {
            getString(R.string.video)
        } else {
            getString(R.string.photo)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_media)
            .setMessage(getString(R.string.delete_media_confirmation, mediaType))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteMedia(item)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}