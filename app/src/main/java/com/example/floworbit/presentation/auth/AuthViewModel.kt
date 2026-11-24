package com.example.floworbit.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.floworbit.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableStateFlow(AuthState())
    val authState = _authState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<AuthResult?>(null)
    val resetPasswordState = _resetPasswordState.asStateFlow()

    fun onLogin(email: String, password: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, loginError = null) }
            val result = repository.login(email, password)
            _authState.update {
                if (result.data != null) {
                    it.copy(isLoading = false, isLoginSuccess = true)
                } else {
                    it.copy(isLoading = false, loginError = result.errorMessage)
                }
            }
        }
    }

    fun onRegister(email: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _authState.update { it.copy(registrationError = "Passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, registrationError = null) }
            val result = repository.register(email, password)
            _authState.update {
                if (result.data != null) {
                    it.copy(isLoading = false, isRegistrationSuccess = true)
                } else {
                    it.copy(isLoading = false, registrationError = result.errorMessage)
                }
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            _resetPasswordState.value = result
        }
    }

    fun resetPasswordState() {
        _resetPasswordState.value = null
    }

    fun resetAuthState() {
        _authState.value = AuthState()
    }
}
