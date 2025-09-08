package com.womensafety.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuthException
import com.womensafety.app.utils.ValidationUtils
import com.womensafety.app.databinding.ActivityAuthBinding
import com.womensafety.app.ui.MainActivity

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "AuthActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        initializeFirebase()

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setupUI()

        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: ${currentUser.email}")
            navigateToMainActivity()
        }
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "Firebase initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed", e)
            Toast.makeText(this, "Firebase initialization failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        binding.apply {
            // Biometric Authentication Buttons
            btnFingerprint.setOnClickListener {
                Toast.makeText(this@AuthActivity, "🔐 Fingerprint authentication coming soon!", Toast.LENGTH_SHORT).show()
            }

            btnFaceId.setOnClickListener {
                Toast.makeText(this@AuthActivity, "👤 Face ID authentication coming soon!", Toast.LENGTH_SHORT).show()
            }

            // Google Sign In Button
            btnGoogleSignIn.setOnClickListener {
                Toast.makeText(this@AuthActivity, "🔐 Google Sign In coming soon!", Toast.LENGTH_SHORT).show()
            }

            // Email Sign In Button
            btnEmailSignIn.setOnClickListener {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                if (validateInput(email, password)) {
                    signInWithEmail(email, password)
                }
            }

            // Forgot Password
            tvForgotPassword.setOnClickListener {
                val email = etEmail.text.toString().trim()
                if (email.isNotEmpty()) {
                    resetPassword(email)
                } else {
                    Toast.makeText(this@AuthActivity, "Please enter your email first", Toast.LENGTH_SHORT).show()
                }
            }

            // Sign Up Text View
            tvSignUp.setOnClickListener {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                if (validateInput(email, password)) {
                    signUpWithEmail(email, password)
                }
            }

            // Skip Authentication (Demo)
            btnSkipAuth.setOnClickListener {
                Toast.makeText(this@AuthActivity, "🚀 Entering Demo Mode...", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        val result = ValidationUtils.validateEmailAndPassword(email, password)

        binding.tilEmail.error = result.emailError
        binding.tilPassword.error = result.passwordError

        return result.isValid
    }

    private fun signInWithEmail(email: String, password: String) {
        showLoading(true)
        Log.d(TAG, "Attempting email sign-in for: $email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Email sign in success: ${user?.email}")
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    val exception = task.exception
                    Log.w(TAG, "Email sign in failure", exception)
                    val errorMessage = when (exception) {
                        is FirebaseAuthException -> {
                            when (exception.errorCode) {
                                "ERROR_INVALID_EMAIL" -> "Invalid email address."
                                "ERROR_WRONG_PASSWORD" -> "Incorrect password."
                                "ERROR_USER_NOT_FOUND" -> "No account found with this email."
                                "ERROR_USER_DISABLED" -> "This account has been disabled."
                                "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts."
                                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error."
                                else -> "Sign in failed: ${exception.message}"
                            }
                        }
                        else -> "Sign in failed: ${exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
                showLoading(false)
            }
    }

    private fun signUpWithEmail(email: String, password: String) {
        showLoading(true)
        Log.d(TAG, "Attempting email sign-up for: $email")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Email sign up success: ${user?.email}")

                    // Send email verification
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Log.d(TAG, "Email verification sent")
                            Toast.makeText(this, "Account created! Verification email sent to $email", Toast.LENGTH_LONG).show()
                        } else {
                            Log.w(TAG, "Email verification failed to send", verificationTask.exception)
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    val exception = task.exception
                    Log.w(TAG, "Email sign up failure", exception)
                    val errorMessage = when (exception) {
                        is FirebaseAuthException -> {
                            when (exception.errorCode) {
                                "ERROR_INVALID_EMAIL" -> "Invalid email address."
                                "ERROR_WEAK_PASSWORD" -> "Password is too weak (minimum 6 characters)."
                                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists with this email."
                                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection."
                                else -> "Sign up failed: ${exception.message}"
                            }
                        }
                        else -> "Sign up failed: ${exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
                showLoading(false)
            }
    }

    private fun resetPassword(email: String) {
        showLoading(true)
        Log.d(TAG, "Attempting password reset for: $email")

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Password reset email sent")
                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Log.w(TAG, "Password reset failed", task.exception)
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
                showLoading(false)
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        
        // Disable/enable all interactive UI elements during loading
        binding.btnFingerprint.isEnabled = !show
        binding.btnFaceId.isEnabled = !show
        binding.btnGoogleSignIn.isEnabled = !show
        binding.btnEmailSignIn.isEnabled = !show
        binding.tvForgotPassword.isEnabled = !show
        binding.tvSignUp.isEnabled = !show
        binding.btnSkipAuth.isEnabled = !show
        
        // Disable/enable input fields
        binding.tilEmail.isEnabled = !show
        binding.tilPassword.isEnabled = !show
        binding.etEmail.isEnabled = !show
        binding.etPassword.isEnabled = !show
        
        // Set alpha for visual feedback
        val alpha = if (show) 0.5f else 1.0f
        binding.cardLogin.alpha = alpha
        binding.cardDemo.alpha = alpha
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
