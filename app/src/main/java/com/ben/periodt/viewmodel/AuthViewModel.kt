package com.ben.periodt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ben.periodt.network.LoginRequest
import com.ben.periodt.network.PeriodtNetworkRepository
import com.ben.periodt.security.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: PeriodtNetworkRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<String>("Idle")
    val loginState: StateFlow<String> = _loginState.asStateFlow()

    fun performLogin(email: String, password: String) {
        _loginState.value = "Loading..."

        // Launch a coroutine on the background thread
        viewModelScope.launch {
            val result = repository.login(LoginRequest(email, password))

            result.onSuccess { authResponse ->
                // 1. Save the token and ID securely to the device hardware
                tokenManager.saveToken(authResponse.token)
                tokenManager.saveUserId(authResponse.userId)

                _loginState.value = "Success! Token stored."
            }.onFailure { error ->
                _loginState.value = "Failed: ${error.message}"
            }
        }
    }
}