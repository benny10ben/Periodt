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

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun register(username: String, passwordPlain: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Generate local cryptography
                val saltBase64 = SyncCryptoManager.generateSaltBase64()
                val accountKey = SyncCryptoManager.deriveAccountKey(passwordPlain, saltBase64)
                val dataKey = SyncCryptoManager.generateRandomDataKey()
                val wrappedDataKey = SyncCryptoManager.wrapDataKey(accountKey, dataKey)

                // 2. Send to server
                val request = RegisterRequest(username, passwordPlain, saltBase64, wrappedDataKey)
                val result = apiClient.register(request)

                result.onSuccess { response ->
                    processSuccessfulAuth(response.token, response.userId, saltBase64, accountKey, dataKey)
                }.onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Registration Failed")
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
                // 1. Authenticate and get Salt
                val loginResult = apiClient.login(LoginRequest(username, passwordPlain))

                loginResult.onSuccess { response ->
                    val saltBase64 = response.saltBase64 ?: throw Exception("Missing cryptographic salt.")

                    // 2. We are switching to a cloud account, wipe the local guest sandbox
                    AppDatabase.getDatabase(getApplication()).periodCycleDao().deleteAll()

                    // 3. Save auth token so we can fetch the wrapped key
                    tokenManager.saveToken(response.token)

                    // 4. Fetch the Wrapped Data Key from the server
                    val keyResult = networkRepo.fetchKey()
                    keyResult.onSuccess { keyDto ->

                        // 5. Reconstruct the keys
                        val accountKey = SyncCryptoManager.deriveAccountKey(passwordPlain, saltBase64)
                        val dataKey = SyncCryptoManager.unwrapDataKey(accountKey, keyDto.wrappedDataKey)

                        processSuccessfulAuth(response.token, response.userId, saltBase64, accountKey, dataKey)

                    }.onFailure {
                        _authState.value = AuthState.Error("Failed to fetch encryption keys.")
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
        userId: Long,
        saltBase64: String,
        accountKey: ByteArray,
        dataKey: ByteArray
    ) {
        withContext(Dispatchers.Default) {
            try {
                // 1. Save standard tokens
                tokenManager.saveToken(token)
                tokenManager.saveUserId(userId)

                // 2. Save cryptographic material to memory and the secure hardware vault
                SyncCryptoManager.sessionDataKey = dataKey
                secureVault.saveAesKey(SecretKeySpec(accountKey, "AES"))
                secureVault.saveSalt(Base64.decode(saltBase64, Base64.NO_WRAP))

                // 3. Trigger initial sync
                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.ben.periodt.sync.PeriodtSyncWorker>()
                    .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                    .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, androidx.work.WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build()

                androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "PeriodtImmediateSync",
                    androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                    syncRequest
                )

                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to initialize cryptographic vault.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(getApplication()).periodCycleDao().deleteAll()
            withContext(Dispatchers.Main) {
                tokenManager.clearAll()
                secureVault.clearVault()
                SyncCryptoManager.clearSession()
                _authState.value = AuthState.Idle
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