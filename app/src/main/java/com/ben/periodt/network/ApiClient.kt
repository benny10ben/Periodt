package com.ben.periodt.network

import android.util.Log
import com.ben.periodt.security.TokenManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(private val tokenManager: TokenManager) {

    val httpClient = HttpClient(CIO) {
        // 1. JSON Serialization Setup
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
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
        // Intercept every single request dynamically to guarantee the freshest token is injected
        httpClient.requestPipeline.intercept(HttpRequestPipeline.State) {
            val token = tokenManager.getToken()
            val path = context.url.encodedPath

            // Inject the token on all endpoints EXCEPT login and register
            if (token != null && !path.endsWith("/login") && !path.endsWith("/register")) {
                context.headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}