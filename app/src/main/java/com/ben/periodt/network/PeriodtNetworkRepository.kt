package com.ben.periodt.network

import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.delete
import io.ktor.http.isSuccess

class PeriodtNetworkRepository(private val apiClient: ApiClient) {

    private val client = apiClient.httpClient

    // ─── AUTHENTICATION ──────────────────────────────────────────────────

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response = client.post("/api/v1/auth/register") {
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else if (response.status.value == 400 || response.status.value == 409) {
                Result.failure(Exception("This username is already taken."))
            } else {
                Result.failure(Exception("Registration failed: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Registration failed", e)
            Result.failure(e)
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response = client.post("/api/v1/auth/login") {
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Login failed: Invalid username or password"))
            }
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Login failed", e)
            Result.failure(e)
        }
    }

    suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> {
        return try {
            val response = client.post("/api/v1/auth/change-password") {
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to change password. Ensure your old password is correct."))
            }
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Password change failed", e)
            Result.failure(e)
        }
    }

    suspend fun changeUsername(request: ChangeUsernameRequest): Result<AuthResponse> {
        return try {
            val response = client.post("/api/v1/auth/change-username") {
                setBody(request)
            }
            if (response.status.isSuccess()) {
                // If successful, the server sends back a fresh JWT token!
                Result.success(response.body())
            } else if (response.status.value == 409) {
                Result.failure(Exception("This username is already taken."))
            } else {
                Result.failure(Exception("Failed to change username."))
            }
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Username change failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            client.delete("/api/v1/auth/delete")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Account deletion failed", e)
            Result.failure(e)
        }
    }

    // ─── DEVICE & KEYS ───────────────────────────────────────────────────

    suspend fun registerDevice(request: DeviceRegistrationRequest): Result<Unit> {
        return try {
            client.post("/api/v1/devices/register") {
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Device registration failed", e)
            Result.failure(e)
        }
    }

    suspend fun uploadKey(request: WrappedKeyDto): Result<Unit> {
        return try {
            client.post("/api/v1/keys/upload") {
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Key upload failed", e)
            Result.failure(e)
        }
    }

    suspend fun fetchKey(): Result<WrappedKeyDto> {
        return try {
            val response: WrappedKeyDto = client.get("/api/v1/keys/fetch").body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Key fetch failed", e)
            Result.failure(e)
        }
    }

    // ─── SYNC ENGINE ─────────────────────────────────────────────────────

    suspend fun pushSyncData(request: SyncPushRequest): Result<Unit> {
        return try {
            client.post("/api/v1/sync/push") {
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Sync push failed", e)
            Result.failure(e)
        }
    }

    suspend fun pullSyncData(cursor: Long): Result<SyncPullResponse> {
        return try {
            val response: SyncPullResponse = client.get("/api/v1/sync/pull") {
                parameter("cursor", cursor)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Sync pull failed", e)
            Result.failure(e)
        }
    }
}