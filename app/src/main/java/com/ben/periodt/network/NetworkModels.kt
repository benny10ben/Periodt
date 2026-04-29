package com.ben.periodt.network

import kotlinx.serialization.Serializable

// --- Auth Payloads ---

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Long
)

// --- Device Payloads ---

@Serializable
data class DeviceRegistrationRequest(
    val deviceId: String,
    val deviceName: String? = null
)

// --- Key Payloads ---

@Serializable
data class WrappedKeyDto(
    val wrappedDataKey: String
)

// --- Sync Payloads ---

@Serializable
data class SyncItemDto(
    val syncUuid: String, // UUIDs are sent as Strings in JSON
    val entityType: String,
    val encryptedPayload: String,
    val isDeleted: Boolean,
    val serverVersion: Long? = null
)

@Serializable
data class SyncPushRequest(
    val items: List<SyncItemDto>
)

@Serializable
data class SyncPullResponse(
    val items: List<SyncItemDto>,
    val latestCursor: Long
)