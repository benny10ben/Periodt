package com.ben.periodt.network

import android.util.Log
import com.ben.periodt.security.TokenManager
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(private val tokenManager: TokenManager) {

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) { Log.d("KtorNetwork", message) }
            }
            level = LogLevel.ALL
        }

        defaultRequest {
            url {
                protocol = URLProtocol.HTTP
                host = "localhost"
                port = 8080
            }
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        install(Auth) {
            bearer {
                // 1. Load tokens for standard requests
                loadTokens {
                    val accessToken = tokenManager.getToken()
                    val refreshToken = tokenManager.getRefreshToken()
                    if (accessToken != null && refreshToken != null) {
                        BearerTokens(accessToken, refreshToken)
                    } else null
                }

                // 2. Handle 401 Auto-Refresh
                refreshTokens {
                    val currentRefresh = tokenManager.getRefreshToken() ?: return@refreshTokens null
                    try {
                        val response = client.post("/api/v1/auth/refresh") {
                            markAsRefreshTokenRequest()
                            setBody(RefreshTokenRequest(currentRefresh))
                        }

                        if (response.status.isSuccess()) {
                            val body = response.body<RefreshTokenResponse>()
                            tokenManager.saveToken(body.accessToken)
                            BearerTokens(body.accessToken, currentRefresh)
                        } else if (response.status.value in 400..499) {
                            // Only wipe data if the server actively rejects the refresh token
                            Log.e("KtorAuth", "Refresh rejected by server. Logging out.")
                            tokenManager.clearAll()
                            null
                        } else {
                            // 500 error, don't wipe local data
                            null
                        }
                    } catch (e: Exception) {
                        // Network error (timeout, no wifi). DO NOT clear the vault here!
                        Log.e("KtorAuth", "Network error during refresh, keeping local session active.", e)
                        null
                    }
                }

                // 3. Skip auth headers for public endpoints
                sendWithoutRequest { request ->
                    val path = request.url.encodedPath
                    path.endsWith("/login") || path.endsWith("/register") || path.endsWith("/refresh")
                }
            }
        }
    }
}