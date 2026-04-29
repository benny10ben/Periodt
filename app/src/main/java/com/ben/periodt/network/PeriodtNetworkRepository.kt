package com.ben.periodt.network

import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class PeriodtNetworkRepository(private val apiClient: ApiClient) {

    private val client = apiClient.httpClient

    // ─── AUTHENTICATION ──────────────────────────────────────────────────

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            // Makes a POST request to http://localhost:8080/api/auth/register
            val response: AuthResponse = client.post("/api/auth/register") {
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Registration failed", e)
            Result.failure(e)
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response: AuthResponse = client.post("/api/auth/login") {
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Login failed", e)
            Result.failure(e)
        }
    }

    // ─── DEVICE & KEYS ───────────────────────────────────────────────────

    suspend fun registerDevice(request: DeviceRegistrationRequest): Result<Unit> {
        return try {
            client.post("/api/devices/register") {
                setBody(request)
            }
            // If it doesn't throw an exception, it was a 200 OK success
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Device registration failed", e)
            Result.failure(e)
        }
    }

    suspend fun uploadKey(request: WrappedKeyDto): Result<Unit> {
        return try {
            client.post("/api/keys/upload") {
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
            val response: WrappedKeyDto = client.get("/api/keys/fetch").body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Key fetch failed", e)
            Result.failure(e)
        }
    }

    // ─── SYNC ENGINE ─────────────────────────────────────────────────────

    suspend fun pushSyncData(request: SyncPushRequest): Result<Unit> {
        return try {
            client.post("/api/sync/push") {
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
            val response: SyncPullResponse = client.get("/api/sync/pull") {
                // This appends ?cursor=123 to the URL automatically
                parameter("cursor", cursor)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("NetworkRepo", "Sync pull failed", e)
            Result.failure(e)
        }
    }
}