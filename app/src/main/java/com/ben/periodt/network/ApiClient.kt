package com.ben.periodt.network

import android.util.Log
import com.ben.periodt.security.TokenManager
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(private val tokenManager: TokenManager) {

    val httpClient = HttpClient(CIO) {
        // 1. JSON Serialization Setup
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                // CRITICAL: Prevents the app from crashing if the backend adds new fields later
                ignoreUnknownKeys = true
            })
        }

        // 2. Network Logging
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorNetwork", message)
                }
            }
            level = LogLevel.ALL
        }

        // 3. Base URL Configuration
        defaultRequest {
            url {
                protocol = URLProtocol.HTTP
                host = "localhost"
                port = 8080
            }
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }

    init {
        // FIXED: Intercept every single request dynamically to guarantee
        // the freshest token is injected, bypassing Ktor's aggressive Auth caching.
        httpClient.requestPipeline.intercept(HttpRequestPipeline.State) {
            val token = tokenManager.getToken()
            val path = context.url.encodedPath

            // Inject the token on all endpoints EXCEPT login and register
            if (token != null && !path.endsWith("/login") && !path.endsWith("/register")) {
                context.headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response = httpClient.post("/api/auth/register") {
                contentType(ContentType.Application.Json)
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
            Result.failure(e)
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response = httpClient.post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Login failed: Invalid username or password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Endpoint for the password/key rotation flow
    suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> {
        return try {
            val response = httpClient.post("/api/auth/change-password") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to change password. Ensure your old password is correct."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}