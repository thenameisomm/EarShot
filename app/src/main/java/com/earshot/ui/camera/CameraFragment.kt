package com.earshot.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.earshot.R
import com.earshot.camera.CameraPhotoOutput
import com.earshot.camera.CameraXManager
import com.earshot.databinding.FragmentCameraBinding
import com.earshot.model.CameraMode
import com.earshot.model.CameraSelection
import com.earshot.repository.SettingsRepository
import com.earshot.ui.base.BaseFragment
import com.earshot.ui.base.requireApplication
import com.earshot.viewmodel.CameraViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Camera Fragment for capturing photos and videos using CameraX.
 *
 * This fragment provides:
 * - Live camera preview display
 * - Photo capture functionality
 * - Video recording functionality
 * - Camera switching (front/rear)
 * - Flash mode toggle
 * - Live camera permission UI state
 * - Bottom sheet settings panel
 *
 * ## Lifecycle
 *
 * CameraX is bound to this fragment's lifecycle:
 * - onCreate(): Initialize CameraXManager
 * - onStart(): Bind CameraX to lifecycle
 * - onStop(): Unbind CameraX
 * - onDestroyView(): Cleanup resources
 */
class CameraFragment : BaseFragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels {
        CameraViewModel.Factory(
            requireApplication(),
            SettingsRepository(requireContext())
        )
    }

    private val photoOutput by lazy { CameraPhotoOutput(requireContext()) }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private var countDownTimer: CountDownTimer? = null

    private var hasCameraPermission = false

    // Camera permission result launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasCameraPermission = true
            // Try to bind immediately if camera manager is already ready
            viewModel.cameraXManager.value?.let {
                viewModel.bindToLifecycle(this)
            }
        } else {
            // Permission denied - show explanation
            showPermissionDeniedMessage()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize CameraX with PreviewView BEFORE checking permissions
        viewModel.initializeCameraX(binding.previewView)

        // Check initial permission state
        hasCameraPermission = checkCameraPermissionsNow()

        // Observe camera manager ready state and bind when ready
        viewModel.cameraXManager.observe(viewLifecycleOwner) { manager ->
            manager?.let {
                // Only bind if we have permission
                if (hasCameraPermission) {
                    viewModel.bindToLifecycle(this)
                }
            }
        }

        setupBottomSheet()
        setupTimerDropdown()
        setupObservers()
        setupClickListeners()
    }

    /**
     * Check camera permissions synchronously.
     * Returns true if already granted, false otherwise.
     */
    private fun checkCameraPermissionsNow(): Boolean {
        val permission = Manifest.permission.CAMERA
        return requireContext().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStart() {
        super.onStart()
        // Start preview - binding already done in onViewCreated observer
        viewModel.startPreview()
    }

    override fun onStop() {
        super.onStop()
        // Unbind CameraX when fragment stops
        viewModel.stopPreview()
        viewModel.unbind()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }

    // -----------------------------------------------------------------------
    // Bottom Sheet Setup
    // -----------------------------------------------------------------------

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.settingsBottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Handle state changes if needed
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Animate camera controls based on slide offset
                if (slideOffset < 0) {
                    // Bottom sheet is opening - fade out camera controls
                    val alpha = 1f + slideOffset // 0 at fully expanded, 1 at hidden
                    binding.bottomControlsBar.alpha = alpha
                    binding.topControlsBar.alpha = alpha
                }
            }
        })
    }

    /**
     * Show the settings bottom sheet.
     */
    private fun showSettings() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    /**
     * Hide the settings bottom sheet.
     */
    private fun hideSettings() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    // -----------------------------------------------------------------------
    // Permission Handling
    // -----------------------------------------------------------------------

    /**
     * Show message when camera permission is denied.
     */
    private fun showPermissionDeniedMessage() {
        Toast.makeText(
            requireContext(),
            R.string.camera_permission_required,
            Toast.LENGTH_LONG
        ).show()
    }

    // -----------------------------------------------------------------------
    // Setup Methods
    // -----------------------------------------------------------------------

    private fun setupTimerDropdown() {
        val timerNames = viewModel.timerOptions.map { it.displayName }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            timerNames
        )
        binding.actvTimer.setAdapter(adapter)

        binding.actvTimer.setOnItemClickListener { _, _, position, _ ->
            viewModel.setTimerOption(viewModel.timerOptions[position])
        }
    }

    private fun setupObservers() {
        // Camera selection observer
        viewModel.cameraSelection.observe(viewLifecycleOwner) { selection ->
            when (selection) {
                CameraSelection.REAR -> binding.rbRear.isChecked = true
                CameraSelection.FRONT -> binding.rbFront.isChecked = true
                null -> {}
            }
        }

        // Camera mode observer
        viewModel.cameraMode.observe(viewLifecycleOwner) { mode ->
            when (mode) {
                CameraMode.PHOTO -> binding.toggleMode.check(R.id.btnPhoto)
                CameraMode.VIDEO -> binding.toggleMode.check(R.id.btnVideo)
                null -> {}
            }
        }

        // Grid overlay observer
        viewModel.gridOverlayEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchGrid.isChecked = enabled ?: false
        }

        // Timer observer
        viewModel.timerOption.observe(viewLifecycleOwner) { option ->
            binding.actvTimer.setText(option?.displayName ?: "Off", false)
        }

        // Save success observer
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                val message = if (it) {
                    getString(R.string.camera_saved)
                } else {
                    getString(R.string.error)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearSaveStatus()
                // Hide settings after saving
                hideSettings()
            }
        }

        // Error observer
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Flash mode observer
        viewModel.flashMode.observe(viewLifecycleOwner) { mode ->
            updateFlashButton(mode ?: CameraXManager.FLASH_MODE_AUTO)
        }

        // Recording state observer
        viewModel.isRecording.observe(viewLifecycleOwner) { isRecording ->
            updateRecordingUI(isRecording ?: false)
        }

        // Photo URI observer (photo taken successfully)
        viewModel.photoUri.observe(viewLifecycleOwner) { uri ->
            uri?.let {
                Toast.makeText(
                    requireContext(),
                    "Photo saved: $it",
                    Toast.LENGTH_SHORT
                ).show()
                updateGalleryThumbnail(it)
            }
        }

        // Video URI observer (video recorded successfully)
        viewModel.videoUri.observe(viewLifecycleOwner) { uri ->
            uri?.let {
                Toast.makeText(
                    requireContext(),
                    "Video saved: $it",
                    Toast.LENGTH_SHORT
                ).show()
                updateGalleryThumbnail(it)
            }
        }
    }

    private fun setupClickListeners() {
        // Back button - navigate up
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Settings button - opens bottom sheet
        binding.btnSettings.setOnClickListener {
            showSettings()
        }

        // Close settings button
        binding.btnCloseSettings.setOnClickListener {
            hideSettings()
        }

        // Camera selection radio group
        binding.rgCamera.setOnCheckedChangeListener { _, checkedId ->
            val selection = when (checkedId) {
                R.id.rbRear -> CameraSelection.REAR
                R.id.rbFront -> CameraSelection.FRONT
                else -> CameraSelection.REAR
            }
            viewModel.setCameraSelection(selection)
            viewModel.switchCamera(selection)
        }

        // Camera mode toggle
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.btnPhoto -> CameraMode.PHOTO
                    R.id.btnVideo -> CameraMode.VIDEO
                    else -> CameraMode.PHOTO
                }
                viewModel.setCameraMode(mode)
            }
        }

        // Grid overlay switch
        binding.switchGrid.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setGridOverlayEnabled(isChecked)
        }

        // Timer dropdown
        binding.actvTimer.setOnItemClickListener { _, _, position, _ ->
            viewModel.setTimerOption(viewModel.timerOptions[position])
        }

        // Save button
        binding.btnSave.setOnClickListener {
            viewModel.saveSettings()
            // Close settings panel after saving (don't navigate away)
            hideSettings()
        }

        // Capture button
        binding.btnCapture.setOnClickListener {
            // Check for timer
            val timerOption = viewModel.timerOption.value
            if (timerOption != null && timerOption.seconds > 0) {
                startCountdownTimer(timerOption.seconds)
            } else {
                performCapture()
            }
        }

        // Flash toggle button
        binding.btnFlash.setOnClickListener {
            viewModel.toggleFlash()
        }

        // Camera switch button
        binding.btnCameraSwitch.setOnClickListener {
            val current = viewModel.cameraSelection.value ?: CameraSelection.REAR
            val newSelection = if (current == CameraSelection.REAR) {
                CameraSelection.FRONT
            } else {
                CameraSelection.REAR
            }
            viewModel.setCameraSelection(newSelection)
            viewModel.switchCamera(newSelection)
        }

        // Gallery thumbnail click
        binding.cardGallery.setOnClickListener {
            openGallery()
        }

        // Drag handle to close settings
        binding.dragHandle.setOnClickListener {
            hideSettings()
        }
    }

    // -----------------------------------------------------------------------
    // Capture Methods
    // -----------------------------------------------------------------------

    private fun startCountdownTimer(seconds: Int) {
        binding.tvTimer.visibility = View.VISIBLE

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer((seconds * 1000).toLong(), 1000) {
            var remainingSeconds: Int = seconds

            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt() + 1
                binding.tvTimer.text = remainingSeconds.toString()
            }

            override fun onFinish() {
                binding.tvTimer.visibility = View.GONE
                performCapture()
            }
        }.start()
    }

    private fun performCapture() {
        when (viewModel.cameraMode.value) {
            CameraMode.PHOTO -> viewModel.takePhoto()
            CameraMode.VIDEO -> {
                if (viewModel.isRecording.value == true) {
                    viewModel.stopVideo()
                } else {
                    viewModel.startVideo()
                }
            }
            null -> {
                // Default to photo mode
                viewModel.takePhoto()
            }
        }
    }

    // -----------------------------------------------------------------------
    // UI Update Methods
    // -----------------------------------------------------------------------

    /**
     * Update flash button icon based on flash mode.
     */
    private fun updateFlashButton(mode: Int) {
        when (mode) {
            CameraXManager.FLASH_MODE_AUTO -> {
                binding.btnFlash.setImageResource(R.drawable.ic_flash_auto)
            }
            CameraXManager.FLASH_MODE_ON -> {
                binding.btnFlash.setImageResource(R.drawable.ic_flash_on)
            }
            CameraXManager.FLASH_MODE_OFF -> {
                binding.btnFlash.setImageResource(R.drawable.ic_flash_off)
            }
        }
    }

    /**
     * Update recording UI based on recording state.
     */
    private fun updateRecordingUI(isRecording: Boolean) {
        if (isRecording) {
            binding.btnCapture.setImageResource(R.drawable.ic_media_stop)
            binding.recordingStatusContainer.visibility = View.VISIBLE
        } else {
            binding.btnCapture.setImageResource(R.drawable.ic_camera)
            binding.recordingStatusContainer.visibility = View.GONE
            binding.tvRecordingStatus.text = "Recording..."
        }
    }

    /**
     * Update gallery thumbnail with the latest captured photo/video.
     */
    private fun updateGalleryThumbnail(uri: Uri) {
        try {
            binding.ivGalleryThumbnail.setImageURI(uri)
            binding.ivGalleryPlaceholder.visibility = View.GONE
        } catch (e: Exception) {
            // Keep placeholder if image fails to load
            binding.ivGalleryPlaceholder.visibility = View.VISIBLE
        }
    }

    /**
     * Open the in-app gallery to view captured photos/videos.
     */
    private fun openGallery() {
        // Navigate to the in-app gallery
        findNavController().navigate(R.id.galleryFragment)
    }

    // -----------------------------------------------------------------------
    // Camera Permission Constants
    // -----------------------------------------------------------------------

    companion object {
        private const val TAG = "CameraFragment"

        /**
         * Check if the app has camera permission.
         */
        fun hasCameraPermission(context: android.content.Context): Boolean {
            return context.checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        }
    }
}