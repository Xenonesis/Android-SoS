package com.womensafety.app.ui.auth

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.womensafety.app.data.firebase.FirebaseAuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val authRepository = FirebaseAuthRepository()
    
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState
    
    sealed class AuthState {
        object Idle : AuthState()
        data class Loading(val isLoading: Boolean) : AuthState()
        object CodeSent : AuthState() // For phone auth
        object Authenticated : AuthState()
        data class Error(val message: String) : AuthState()
    }
    
    init {
        // Check if user is already authenticated
        if (authRepository.isUserLoggedIn) {
            _authState.value = AuthState.Authenticated
        }
    }
    
    fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading(true)
            
            val result = authRepository.sendVerificationCode(phoneNumber, activity, callbacks)
            
            result.fold(
                onSuccess = {
                    // Code sent successfully - callback will handle state change
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Failed to send verification code"
                    )
                }
            )
            
            _authState.value = AuthState.Loading(false)
        }
    }
    
    fun verifyPhoneNumber(verificationId: String, code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading(true)
            
            val result = authRepository.verifyPhoneNumber(verificationId, code)
            
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated
                    
                    // Update FCM token
                    authRepository.updateFcmToken()
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Verification failed"
                    )
                }
            )
            
            _authState.value = AuthState.Loading(false)
        }
    }
    
    fun signInWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading(true)
            
            val result = if (credential is PhoneAuthCredential) {
                authRepository.signInWithCredential(credential)
            } else {
                authRepository.signInWithCredential(credential)
            }
            
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated
                    
                    // Update FCM token
                    authRepository.updateFcmToken()
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Authentication failed"
                    )
                }
            )
            
            _authState.value = AuthState.Loading(false)
        }
    }
    
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading(true)
            
            val result = authRepository.signInWithEmail(email, password)
            
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated
                    
                    // Update FCM token
                    authRepository.updateFcmToken()
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Email sign in failed"
                    )
                }
            )
            
            _authState.value = AuthState.Loading(false)
        }
    }
    
    fun signInAnonymously() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading(true)
            
            val result = authRepository.signInAnonymously()
            
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated
                    
                    // Update FCM token
                    authRepository.updateFcmToken()
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Anonymous sign-in failed"
                    )
                }
            )
            
            _authState.value = AuthState.Loading(false)
        }
    }
    
    fun updateUserProfile(name: String, phoneNumber: String) {
        viewModelScope.launch {
            val result = authRepository.updateUserProfile(name, phoneNumber)
            
            result.fold(
                onSuccess = {
                    // Profile updated successfully
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Failed to update profile"
                    )
                }
            )
        }
    }
    
    fun setLoading(isLoading: Boolean) {
        _authState.value = AuthState.Loading(isLoading)
    }
    
    fun setCodeSent(codeSent: Boolean) {
        if (codeSent) {
            _authState.value = AuthState.CodeSent
        }
    }
    
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading(true)
            
            val result = authRepository.resetPassword(email)
            
            result.fold(
                onSuccess = {
                    // Password reset email sent successfully
                    _authState.value = AuthState.Idle
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Failed to send password reset email"
                    )
                }
            )
            
            _authState.value = AuthState.Loading(false)
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Idle
        }
    }
}
