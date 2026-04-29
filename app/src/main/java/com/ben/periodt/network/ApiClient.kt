package com.ben.periodt.network

import android.util.Log
import com.ben.periodt.security.TokenManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
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

        // 2. Network Logging (Outputs to Logcat so you can debug requests/responses)
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
            // Because of the USB physical device adb reverse tunnel, localhost routes correctly to the development machine.
            url {
                protocol = URLProtocol.HTTP
                host = "localhost"
                port = 8080
            }
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        // 4. The Magic: Automatic JWT Injection
        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenManager.getToken()
                    if (token != null) {
                        // Ktor automatically prepends "Bearer " to this string
                        BearerTokens(accessToken = token, refreshToken = "")
                    } else {
                        null
                    }
                }
            }
        }
    }
}