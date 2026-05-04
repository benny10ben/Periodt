package com.ben.periodt.network

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class RegisterRequest(
    val username: String,
    @SerialName("password") val passwordPlain: String,
    val saltBase64: String,
    val wrappedDataKey: String
)

@Keep
@Serializable
data class LoginRequest(
    val username: String,
    @SerialName("password") val passwordPlain: String
)

@Keep
@Serializable
data class ChangeUsernameRequest(
    val newUsername: String
)

@Keep
@Serializable
data class ChangePasswordRequest(
    @SerialName("oldPassword") val oldPasswordPlain: String,
    @SerialName("newPassword") val newPasswordPlain: String,
    val newSaltBase64: String,
    val newWrappedDataKey: String
)

@Keep
@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val userId: Long,
    val username: String,
    val saltBase64: String
)

@Keep
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Keep
@Serializable
data class RefreshTokenResponse(
    val accessToken: String
)