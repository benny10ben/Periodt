package com.ben.periodt.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    // 1. Create a Master Key tied to the Android hardware Keystore
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // 2. Initialize the Encrypted SharedPreferences
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "periodt_secure_prefs", // The name of the XML file
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, // Encrypts the keys
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM // Encrypts the values
    )

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SYNC_CURSOR = "sync_cursor"
    }

    // --- JWT Token ---
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null)
    }

    /**
     * Specifically removes the JWT token.
     * Fixes the 'Unresolved reference: clearToken' error in AuthViewModel.
     */
    fun clearToken() {
        sharedPreferences.edit().remove(KEY_JWT_TOKEN).apply()
    }

    // --- User ID ---
    fun saveUserId(userId: Long) {
        sharedPreferences.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Long {
        return sharedPreferences.getLong(KEY_USER_ID, -1L)
    }

    // --- Nuke the Vault (for Logout/Delete) ---
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    // --- Sync Cursor ---
    fun saveSyncCursor(cursor: Long) {
        sharedPreferences.edit().putLong(KEY_SYNC_CURSOR, cursor).apply()
    }

    fun getSyncCursor(): Long {
        return sharedPreferences.getLong(KEY_SYNC_CURSOR, 0L) // Defaults to 0 on first install
    }
}