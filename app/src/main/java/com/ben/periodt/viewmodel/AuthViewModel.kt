package com.ben.periodt.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ben.periodt.data.AppDatabase
import com.ben.periodt.network.ApiClient
import com.ben.periodt.network.LoginRequest
import com.ben.periodt.network.PeriodtNetworkRepository
import com.ben.periodt.network.RegisterRequest
import com.ben.periodt.security.SecureVault
import com.ben.periodt.security.SyncCryptoManager
import com.ben.periodt.security.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.spec.SecretKeySpec

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val secureVault = SecureVault(application)
    private val apiClient = ApiClient(tokenManager)
    private val networkRepo = PeriodtNetworkRepository(apiClient)

    private val _currentUser = MutableStateFlow<String?>(tokenManager.getUsername())
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _passwordChangeState = MutableStateFlow<AuthState>(AuthState.Idle)
    val passwordChangeState: StateFlow<AuthState> = _passwordChangeState.asStateFlow()

    fun register(username: String, passwordPlain: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                withContext(Dispatchers.IO) {
                    val saltBase64 = SyncCryptoManager.generateSaltBase64()
                    val accountKey = SyncCryptoManager.deriveAccountKey(passwordPlain, saltBase64)
                    val dataKey = SyncCryptoManager.generateRandomDataKey()
                    val wrappedDataKey = SyncCryptoManager.wrapDataKey(accountKey, dataKey)

                    val request = RegisterRequest(username, passwordPlain, saltBase64, wrappedDataKey)
                    val result = networkRepo.register(request)

                    result.onSuccess { response ->
                        processSuccessfulAuth(
                            response.token,
                            response.refreshToken, // NEW
                            response.userId,
                            saltBase64,
                            accountKey,
                            dataKey,
                            username
                        )
                    }.onFailure { error ->
                        withContext(Dispatchers.Main) {
                            _authState.value = AuthState.Error(error.message ?: "Registration Failed")
                        }
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Cryptographic setup failed.")
            }
        }
    }

    fun login(username: String, passwordPlain: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val loginResult = networkRepo.login(LoginRequest(username, passwordPlain))

                loginResult.onSuccess { response ->
                    val saltBase64 = response.saltBase64 ?: throw Exception("Missing cryptographic salt.")

                    withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(getApplication()).clearAllTables()

                        // Save tokens early so fetchKey() works!
                        tokenManager.saveToken(response.token)
                        tokenManager.saveRefreshToken(response.refreshToken)

                        val keyResult = networkRepo.fetchKey()
                        keyResult.onSuccess { keyDto ->
                            val accountKey = SyncCryptoManager.deriveAccountKey(passwordPlain, saltBase64)
                            val dataKey = SyncCryptoManager.unwrapDataKey(accountKey, keyDto.wrappedDataKey)

                            processSuccessfulAuth(
                                response.token,
                                response.refreshToken, // NEW
                                response.userId,
                                saltBase64,
                                accountKey,
                                dataKey,
                                username
                            )
                        }.onFailure {
                            withContext(Dispatchers.Main) {
                                _authState.value = AuthState.Error("Failed to fetch encryption keys.")
                            }
                        }
                    }
                }.onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login Failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Cryptographic setup failed. Incorrect password?")
            }
        }
    }

    private suspend fun processSuccessfulAuth(
        token: String,
        refreshToken: String, // NEW
        userId: Long,
        saltBase64: String,
        accountKey: ByteArray,
        dataKey: ByteArray,
        username: String
    ) {
        withContext(Dispatchers.Default) {
            try {
                tokenManager.saveToken(token)
                tokenManager.saveRefreshToken(refreshToken) // NEW
                tokenManager.saveUserId(userId)
                tokenManager.saveUsername(username)

                SyncCryptoManager.sessionDataKey = dataKey
                secureVault.saveAesKey(SecretKeySpec(dataKey, "AES"))
                secureVault.saveSalt(Base64.decode(saltBase64, Base64.NO_WRAP))

                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.ben.periodt.sync.PeriodtSyncWorker>()
                    .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                    .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, androidx.work.WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build()

                androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "PeriodtImmediateSync",
                    androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                    syncRequest
                )

                withContext(Dispatchers.Main) {
                    _currentUser.value = username
                    _authState.value = AuthState.Success
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Error("Failed to initialize cryptographic vault.")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Invalidate refresh token on server
            tokenManager.getRefreshToken()?.let { networkRepo.logout(it) }

            // 2. Wipe local database
            AppDatabase.getDatabase(getApplication()).clearAllTables()

            withContext(Dispatchers.Main) {
                // 4. Clear local vault
                tokenManager.clearAll()
                secureVault.clearVault()
                SyncCryptoManager.clearSession()
                _currentUser.value = null
                _authState.value = AuthState.Idle
            }
        }
    }

    fun resetPasswordChangeState() {
        _passwordChangeState.value = AuthState.Idle
    }

    fun changePassword(oldPasswordPlain: String, newPasswordPlain: String) {
        viewModelScope.launch {
            _passwordChangeState.value = AuthState.Loading
            try {
                val currentDataKey = SyncCryptoManager.sessionDataKey
                if (currentDataKey == null) {
                    _passwordChangeState.value = AuthState.Error("Session expired. Please log in again.")
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    val newSaltBase64 = SyncCryptoManager.generateSaltBase64()
                    val newAccountKey = SyncCryptoManager.deriveAccountKey(newPasswordPlain, newSaltBase64)
                    val newWrappedDataKey = SyncCryptoManager.wrapDataKey(newAccountKey, currentDataKey)

                    val request = com.ben.periodt.network.ChangePasswordRequest(
                        oldPasswordPlain = oldPasswordPlain,
                        newPasswordPlain = newPasswordPlain,
                        newSaltBase64 = newSaltBase64,
                        newWrappedDataKey = newWrappedDataKey
                    )

                    val result = networkRepo.changePassword(request)

                    result.onSuccess {
                        secureVault.saveSalt(Base64.decode(newSaltBase64, Base64.NO_WRAP))

                        withContext(Dispatchers.Main) {
                            _passwordChangeState.value = AuthState.Success
                        }
                    }.onFailure { error ->
                        withContext(Dispatchers.Main) {
                            _passwordChangeState.value = AuthState.Error(error.message ?: "Failed to change password.")
                        }
                    }
                }
            } catch (e: Exception) {
                _passwordChangeState.value = AuthState.Error("Cryptographic error occurred.")
            }
        }
    }

    private val _usernameChangeState = MutableStateFlow<AuthState>(AuthState.Idle)
    val usernameChangeState: StateFlow<AuthState> = _usernameChangeState.asStateFlow()

    fun resetUsernameChangeState() {
        _usernameChangeState.value = AuthState.Idle
    }

    fun changeUsername(newUsername: String) {
        viewModelScope.launch {
            _usernameChangeState.value = AuthState.Loading
            try {
                val request = com.ben.periodt.network.ChangeUsernameRequest(newUsername)
                val result = networkRepo.changeUsername(request)

                result.onSuccess { response ->
                    withContext(Dispatchers.IO) {
                        // Save both tokens returned from a successful username change!
                        tokenManager.saveToken(response.token)
                        tokenManager.saveRefreshToken(response.refreshToken)
                        tokenManager.saveUsername(response.username)

                        withContext(Dispatchers.Main) {
                            _currentUser.value = response.username
                            _usernameChangeState.value = AuthState.Success
                        }
                    }
                }.onFailure { error ->
                    _usernameChangeState.value = AuthState.Error(error.message ?: "Failed to change username.")
                }
            } catch (e: Exception) {
                _usernameChangeState.value = AuthState.Error("Network error occurred.")
            }
        }
    }

    private val _deleteAccountState = MutableStateFlow<AuthState>(AuthState.Idle)
    val deleteAccountState: StateFlow<AuthState> = _deleteAccountState.asStateFlow()

    fun resetDeleteAccountState() {
        _deleteAccountState.value = AuthState.Idle
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteAccountState.value = AuthState.Loading
            try {
                val result = networkRepo.deleteAccount()

                result.onSuccess {
                    withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(getApplication()).clearAllTables()

                        withContext(Dispatchers.Main) {
                            tokenManager.clearAll()
                            secureVault.clearVault()
                            SyncCryptoManager.clearSession()
                            _currentUser.value = null
                            _deleteAccountState.value = AuthState.Success
                            _authState.value = AuthState.Idle
                        }
                    }
                }.onFailure { error ->
                    _deleteAccountState.value = AuthState.Error(error.message ?: "Failed to delete account.")
                }
            } catch (e: Exception) {
                _deleteAccountState.value = AuthState.Error("Network error occurred.")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}