package com.earshot.ui.media

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.earshot.R
import com.earshot.databinding.FragmentMediaButtonBinding
import com.earshot.service.MediaButtonService
import com.earshot.ui.base.BaseFragment
import com.earshot.viewmodel.MediaButtonViewModel

/**
 * Media Button Detection Fragment.
 * Displays media button event history and allows starting/stopping the detection service.
 */
class MediaButtonFragment : BaseFragment() {

    private var _binding: FragmentMediaButtonBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MediaButtonViewModel by viewModels {
        MediaButtonViewModel.Factory(requireActivity().application)
    }

    private lateinit var eventAdapter: MediaButtonEventAdapter

    // Permission launcher for notifications (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMediaButtonService()
        } else {
            Toast.makeText(
                requireContext(),
                "Notification permission required for service",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaButtonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Connect the service callback to the ViewModel
        connectServiceCallback()
    }

    private fun setupRecyclerView() {
        eventAdapter = MediaButtonEventAdapter()

        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }
    }

    private fun setupObservers() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            eventAdapter.submitList(events)
            binding.tvEmpty.isVisible = events.isEmpty()
            binding.rvEvents.isVisible = events.isNotEmpty()
        }

        viewModel.isServiceRunning.observe(viewLifecycleOwner) { isRunning ->
            binding.tvStatus.text = if (isRunning) {
                getString(R.string.media_service_running)
            } else {
                getString(R.string.media_service_stopped)
            }

            binding.btnToggleService.text = if (isRunning) {
                getString(R.string.media_stop)
            } else {
                getString(R.string.media_start)
            }
        }

        viewModel.latestEvent.observe(viewLifecycleOwner) { event ->
            event?.let {
                // Show toast message
                Toast.makeText(requireContext(), it.eventType.displayName, Toast.LENGTH_SHORT).show()
                // Clear after showing
                viewModel.clearLatestEvent()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnToggleService.setOnClickListener {
            if (viewModel.isServiceRunning.value == true) {
                stopMediaButtonService()
            } else {
                checkPermissionsAndStartService()
            }
        }

        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
        }
    }

    private fun connectServiceCallback() {
        // This callback will be passed to the service when it's started
        // For now, we'll handle events through the service starting in our context
    }

    /**
     * Check for required permissions before starting the service.
     */
    private fun checkPermissionsAndStartService() {
        // For Android 13+, we need notification permission to show foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startMediaButtonService()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startMediaButtonService()
        }
    }

    /**
     * Start the media button detection service.
     */
    private fun startMediaButtonService() {
        try {
            // Set up the callback before starting service
            MediaButtonService::class.java.getDeclaredField("onMediaButtonEvent")
                .apply { isAccessible = true }
                .set(null, viewModel.mediaButtonCallback)

            viewModel.startService()
            Toast.makeText(requireContext(), "Service started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Fallback: start service normally and handle via broadcast
            viewModel.startService()
        }
    }

    /**
     * Stop the media button detection service.
     */
    private fun stopMediaButtonService() {
        viewModel.stopService()
        Toast.makeText(requireContext(), "Service stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}