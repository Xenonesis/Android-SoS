package com.womensafety.app.ui.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.womensafety.app.R
import com.womensafety.app.SosApplication
import com.womensafety.app.databinding.FragmentHomeBinding
import com.womensafety.app.ui.MainActivity
import com.womensafety.app.utils.Constants
import com.womensafety.app.utils.MapsHelper
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class HomeFragment : Fragment(), SensorEventListener {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: HomeViewModel
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var vibrator: Vibrator? = null
    
    private var sosCountdownTimer: CountDownTimer? = null
    private var isSosActive = false
    private var isShakeDetectionEnabled = true
    
    // Shake detection variables
    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val application = requireActivity().application as SosApplication
        
        // Check if repositories are available (database might not be initialized)
        val sosRepo = application.sosRepository
        val contactRepo = application.contactRepository
        
        if (sosRepo != null && contactRepo != null) {
            viewModel = ViewModelProvider(
                this,
                HomeViewModelFactory(sosRepo, application.locationRepository, contactRepo)
            )[HomeViewModel::class.java]
            
            setupSensors()
            setupUI()
            observeViewModel()
        } else {
            // Show error message and disable database-dependent features
            showDatabaseError()
        }
    }
    
    private fun showDatabaseError() {
        // Show a user-friendly error message
        binding.apply {
            tvAppTitle.text = "Database initialization in progress..."
            tvLocationStatus.text = "Please wait while the app initializes"
            btnEmergency.isEnabled = false
            btnEmergency.text = "Initializing..."
        }

        // Try to reinitialize after a delay
        binding.root.postDelayed({
            // Retry initialization
            val application = requireActivity().application as SosApplication
            if (application.sosRepository != null && application.contactRepository != null) {
                // Restart the fragment
                parentFragmentManager.beginTransaction()
                    .replace(this.id, HomeFragment())
                    .commit()
            }
        }, 3000) // Wait 3 seconds before retry
    }
    
    private fun setupSensors() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)
    }
    
    private fun setupUI() {
        binding.apply {
            // SOS Button
            btnEmergency.setOnClickListener {
                if (isSosActive) {
                    cancelSos()
                } else {
                    startSosCountdown()
                }
            }
            
            // Quick actions
            btnCallPolice.setOnClickListener {
                makeEmergencyCall(Constants.EMERGENCY_NUMBER_POLICE)
            }
            
            btnShareLocation.setOnClickListener {
                viewModel.sendLocationViaWhatsApp(requireContext())
            }
            
            // Maps functionality will be added when button exists in layout
            
            btnSilentAlarm.setOnClickListener {
                triggerSilentSos()
            }
        }
        
        updateSosButtonState(false)
    }
    
    private fun observeViewModel() {
        viewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            location?.let {
                binding.tvLocationStatus.text = it.address ?: "Location: ${it.latitude}, ${it.longitude}"
            }
        }
        
        viewModel.sosStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is HomeViewModel.SosStatus.Idle -> {
                    updateSosButtonState(false)
                    binding.tvSosStatus.text = getString(R.string.tap_for_help)
                }
                is HomeViewModel.SosStatus.Countdown -> {
                    binding.tvSosStatus.text = "SOS in ${status.seconds}..."
                }
                is HomeViewModel.SosStatus.Active -> {
                    updateSosButtonState(true)
                    binding.tvSosStatus.text = getString(R.string.sos_sent)
                    showSosActiveAnimation()
                }
                is HomeViewModel.SosStatus.Error -> {
                    updateSosButtonState(false)
                    binding.tvSosStatus.text = getString(R.string.tap_for_help)
                    Toast.makeText(requireContext(), status.message, Toast.LENGTH_LONG).show()
                }
            }
        }
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
                viewModel.updateSosCountdown(seconds)
                
                // Vibrate on each second
                vibrator?.let {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            }
            
            override fun onFinish() {
                triggerSos()
            }
        }
        
        sosCountdownTimer?.start()
        updateSosButtonState(true, isCountdown = true)
    }
    
    private fun cancelSos() {
        sosCountdownTimer?.cancel()
        sosCountdownTimer = null
        
        if (isSosActive) {
            viewModel.cancelActiveSos()
        }
        
        viewModel.resetSosStatus()
        updateSosButtonState(false)
        
        Toast.makeText(requireContext(), getString(R.string.sos_cancelled), Toast.LENGTH_SHORT).show()
    }
    
    private fun triggerSos() {
        lifecycleScope.launch {
            try {
                viewModel.triggerSos(Constants.SOS_TYPE_MANUAL)
                isSosActive = true
                
                // Strong vibration for SOS activation
                vibrator?.let {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                        it.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                        it.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    }
                }
                
            } catch (e: Exception) {
                viewModel.setSosError(e.message ?: "Failed to send SOS")
            }
        }
    }
    
    private fun triggerSilentSos() {
        lifecycleScope.launch {
            try {
                viewModel.triggerSos(Constants.SOS_TYPE_MANUAL)
                Toast.makeText(requireContext(), "Silent SOS sent", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to send silent SOS", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun makeEmergencyCall(phoneNumber: String) {
        viewModel.makeEmergencyCall(requireContext(), phoneNumber)
    }
    
    private fun shareCurrentLocation() {
        viewModel.sendLocationViaWhatsApp(requireContext())
    }
    
    private fun openMapsView() {
        val currentLocation = viewModel.currentLocation.value
        if (currentLocation != null) {
            MapsHelper.showLocationOnMap(
                requireContext(),
                currentLocation.latitude,
                currentLocation.longitude,
                "My Current Location"
            )
        } else {
            // Open maps without specific location
            startActivity(android.content.Intent(requireContext(), com.womensafety.app.ui.maps.MapsActivity::class.java))
        }
    }
    
    private fun updateSosButtonState(isActive: Boolean, isCountdown: Boolean = false) {
        binding.apply {
            when {
                isCountdown -> {
                    btnEmergency.text = getString(R.string.cancel_sos)
                    btnEmergency.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent_orange))
                    startPulseAnimation()
                }
                isActive -> {
                    btnEmergency.text = getString(R.string.cancel_sos)
                    btnEmergency.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
                    stopPulseAnimation()
                }
                else -> {
                    btnEmergency.text = getString(R.string.emergency_button)
                    btnEmergency.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_red))
                    stopPulseAnimation()
                }
            }
        }
    }
    
    private fun startPulseAnimation() {
        val scaleX = ObjectAnimator.ofFloat(binding.btnEmergency, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.btnEmergency, "scaleY", 1f, 1.1f, 1f)
        
        scaleX.duration = 1000
        scaleY.duration = 1000
        scaleX.repeatCount = ValueAnimator.INFINITE
        scaleY.repeatCount = ValueAnimator.INFINITE
        
        scaleX.start()
        scaleY.start()
    }
    
    private fun stopPulseAnimation() {
        binding.btnEmergency.clearAnimation()
        binding.btnEmergency.scaleX = 1f
        binding.btnEmergency.scaleY = 1f
    }
    
    private fun showSosActiveAnimation() {
        // Add visual feedback for active SOS
        binding.layoutSosActive.visibility = View.VISIBLE
        
        val fadeIn = ObjectAnimator.ofFloat(binding.layoutSosActive, "alpha", 0f, 1f)
        fadeIn.duration = 500
        fadeIn.start()
    }
    
    // Shake detection
    override fun onSensorChanged(event: SensorEvent?) {
        if (!isShakeDetectionEnabled || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }
        
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH
        
        if (acceleration > Constants.SHAKE_THRESHOLD_GRAVITY) {
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastShakeTime > Constants.SHAKE_SLOP_TIME_MS) {
                shakeCount++
                lastShakeTime = currentTime
                
                if (shakeCount >= Constants.SHAKE_REQUIRED_COUNT) {
                    onShakeDetected()
                    shakeCount = 0
                }
            }
        }
        
        // Reset shake count if too much time has passed
        if (System.currentTimeMillis() - lastShakeTime > Constants.SHAKE_COUNT_RESET_TIME_MS) {
            shakeCount = 0
        }
        
        lastX = x
        lastY = y
        lastZ = z
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
    
    private fun onShakeDetected() {
        if (!isSosActive) {
            lifecycleScope.launch {
                try {
                    viewModel.triggerSos(Constants.SOS_TYPE_SHAKE)
                    Toast.makeText(requireContext(), "Shake SOS triggered!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to trigger shake SOS", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        viewModel.startLocationUpdates()
    }
    
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        viewModel.stopLocationUpdates()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        sosCountdownTimer?.cancel()
        _binding = null
    }
}
