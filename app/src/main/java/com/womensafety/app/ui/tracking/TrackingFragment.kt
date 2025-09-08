package com.womensafety.app.ui.tracking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.womensafety.app.R
import com.womensafety.app.SosApplication
import com.womensafety.app.data.model.LocationData
import com.womensafety.app.databinding.FragmentTrackingBinding
import com.womensafety.app.ui.MainActivity
import com.womensafety.app.ui.auth.AuthActivity
import com.womensafety.app.ui.home.HomeViewModel
import com.womensafety.app.ui.home.HomeViewModelFactory
import com.womensafety.app.utils.Constants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TrackingFragment : Fragment() {
    
    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: HomeViewModel
    private lateinit var locationHistoryAdapter: LocationHistoryAdapter
    private var vibrator: Vibrator? = null
    private lateinit var auth: FirebaseAuth
    
    private var isTracking = false
    private var isSosActive = false
    private var sosCountdownTimer: CountDownTimer? = null
    private var currentLocation: LocationData? = null
    private val locationHistory = mutableListOf<LocationData>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Check if user is authenticated
        if (auth.currentUser == null) {
            // Redirect to auth activity
            redirectToAuth()
            return
        }
        
        // Initialize ViewModel
        val application = requireActivity().application as SosApplication
        val sosRepository = application.sosRepository
        val contactRepository = application.contactRepository
        if (sosRepository == null || contactRepository == null) {
            // Handle the case where repositories are not available
            return
        }
        viewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(
                sosRepository,
                application.locationRepository,
                contactRepository
            )
        )[HomeViewModel::class.java]
        
        // Initialize vibrator
        vibrator = requireContext().getSystemService()
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup UI listeners
        setupUI()
        
        // Load initial data
        loadLocationHistory()
        
        // Observe ViewModel
        observeViewModel()
        
        // Update UI
        updateUI()
    }
    
    private fun redirectToAuth() {
        Toast.makeText(context, "Please sign in to use tracking features", Toast.LENGTH_LONG).show()
        val intent = Intent(activity, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
    
    private fun setupRecyclerView() {
        locationHistoryAdapter = LocationHistoryAdapter()
        binding.rvLocationHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = locationHistoryAdapter
        }
    }
    
    private fun setupUI() {
        binding.btnStartTracking.setOnClickListener {
            startTracking()
        }
        
        binding.btnStopTracking.setOnClickListener {
            stopTracking()
        }
        
        binding.btnShareLocation.setOnClickListener {
            shareCurrentLocation()
        }
        
        binding.btnSos.setOnClickListener {
            if (isSosActive) {
                cancelSos()
            } else {
                startSosCountdown()
            }
        }
    }
    
    private fun observeViewModel() {
        // Observe current location
        viewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            currentLocation = location
            updateUI()
        }
        
        // Observe location history from database
        val application = requireActivity().application as SosApplication
        application.database?.locationDao()?.getAllLocations()?.observe(viewLifecycleOwner) { locations ->
            locationHistory.clear()
            locationHistory.addAll(locations)
            locationHistoryAdapter.updateLocations(locationHistory)
        }
        
        // Observe SOS status
        viewModel.sosStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is HomeViewModel.SosStatus.Idle -> {
                    isSosActive = false
                    updateSosButton(false)
                    updateSosStatus("Ready to send SOS")
                }
                is HomeViewModel.SosStatus.Countdown -> {
                    updateSosStatus("SOS in ${status.seconds}...")
                }
                is HomeViewModel.SosStatus.Active -> {
                    isSosActive = true
                    updateSosButton(true)
                    updateSosStatus("SOS SENT - Help is on the way!")
                    Toast.makeText(requireContext(), "SOS Emergency Sent!", Toast.LENGTH_LONG).show()
                }
                is HomeViewModel.SosStatus.Error -> {
                    isSosActive = false
                    updateSosButton(false)
                    updateSosStatus("Error: ${status.message}")
                    Toast.makeText(requireContext(), "SOS Error: ${status.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun updateSosStatus(message: String) {
        binding.tvLastUpdate.text = message
    }
    
    private fun updateSosButton(isActive: Boolean) {
        if (isActive) {
            binding.btnSos.text = "CANCEL SOS"
            binding.btnSos.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green_500)
        } else {
            binding.btnSos.text = "SOS EMERGENCY"
            binding.btnSos.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red_500)
        }
    }
    
    private fun loadLocationHistory() {
        // Show loading skeleton
        locationHistoryAdapter.showLoading()
        
        // Load from database
        val application = requireActivity().application as SosApplication
        lifecycleScope.launch {
            try {
                val locations = application.database?.locationDao()?.getRecentTrackingLocations(20) ?: emptyList()
                locationHistory.clear()
                locationHistory.addAll(locations)
                locationHistoryAdapter.updateLocations(locationHistory)
            } catch (e: Exception) {
                locationHistoryAdapter.hideLoading()
                Toast.makeText(context, "Failed to load location history", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startTracking() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }
        
        isTracking = true
        updateTrackingStatus()
        
        // Start location updates through ViewModel
        viewModel.startLocationUpdates()
        
        Toast.makeText(context, "Tracking started", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopTracking() {
        isTracking = false
        updateTrackingStatus()
        
        // Stop location updates through ViewModel
        viewModel.stopLocationUpdates()
        
        Toast.makeText(context, "Tracking stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun startSosCountdown() {
        if (!(requireActivity() as MainActivity).hasAllPermissions()) {
            (requireActivity() as MainActivity).requestPermissions()
            return
        }
        
        sosCountdownTimer?.cancel()
        
        sosCountdownTimer = object : CountDownTimer(
            Constants.SOS_COUNTDOWN_SECONDS * 1000L,
            1000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt() + 1
                updateSosStatus("SOS in $seconds...")
                
                // Vibrate on each second
                vibrator?.let {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(longArrayOf(0, 100), -1)
                    }
                }
            }
            
            override fun onFinish() {
                triggerSos()
            }
        }
        
        sosCountdownTimer?.start()
        updateSosButton(false) // Show cancel button during countdown
        binding.btnSos.text = "CANCEL SOS"
    }
    
    private fun cancelSos() {
        sosCountdownTimer?.cancel()
        sosCountdownTimer = null
        
        if (isSosActive) {
            lifecycleScope.launch {
                viewModel.cancelActiveSos()
            }
        }
        
        viewModel.resetSosStatus()
        updateSosButton(false)
        updateSosStatus("SOS cancelled")
        Toast.makeText(requireContext(), "SOS Cancelled", Toast.LENGTH_SHORT).show()
    }
    
    private fun triggerSos() {
        lifecycleScope.launch {
            try {
                viewModel.triggerSos(Constants.SOS_TYPE_MANUAL)
                
                // Strong vibration for SOS activation
                vibrator?.let {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                        it.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
                    }
                }
            } catch (e: Exception) {
                viewModel.setSosError(e.message ?: "Failed to send SOS")
            }
        }
    }
    
    private fun updateTrackingStatus() {
        val chipStatus = binding.chipTrackingStatus
        if (isTracking) {
            chipStatus.text = "Active"
            chipStatus.setChipBackgroundColorResource(R.color.green_500)
        } else {
            chipStatus.text = "Inactive"
            chipStatus.setChipBackgroundColorResource(R.color.red_500)
        }
    }
    
    private fun updateUI() {
        // Update last update time and current location
        currentLocation?.let { location ->
            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            binding.tvLastUpdate.text = "Last updated: ${dateFormat.format(Date(location.timestamp))}"
            
            // Update current location
            binding.tvCurrentLocation.text = location.address ?: 
                "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}"
        } ?: run {
            binding.tvLastUpdate.text = "Last updated: Never"
            binding.tvCurrentLocation.text = "Current location: Unknown"
        }
    }
    
    private fun shareCurrentLocation() {
        lifecycleScope.launch {
            try {
                viewModel.shareCurrentLocation(requireContext())
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to share location", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        (activity as? MainActivity)?.requestPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        // Start location updates when fragment becomes visible
        if (isTracking) {
            viewModel.startLocationUpdates()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop location updates when fragment is not visible
        viewModel.stopLocationUpdates()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        sosCountdownTimer?.cancel()
        _binding = null
    }
}
