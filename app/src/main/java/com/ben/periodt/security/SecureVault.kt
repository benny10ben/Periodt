package com.ben.periodt.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SecureVault(context: Context) {

    // 1. Generate the Hardware-Backed Master Key
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // 2. Create the Encrypted XML file on the device
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "periodt_secure_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ==========================================
    // AES KEY STORAGE
    // ==========================================
    fun saveAesKey(key: SecretKey) {
        val base64Key = Base64.encodeToString(key.encoded, Base64.NO_WRAP)
        sharedPreferences.edit().putString("master_aes_key", base64Key).apply()
    }

    fun getAesKey(): SecretKey? {
        val base64Key = sharedPreferences.getString("master_aes_key", null) ?: return null
        val keyBytes = Base64.decode(base64Key, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, "AES")
    }

    // ==========================================
    // SALT STORAGE
    // ==========================================
    fun saveSalt(salt: ByteArray) {
        val base64Salt = Base64.encodeToString(salt, Base64.NO_WRAP)
        sharedPreferences.edit().putString("user_salt", base64Salt).apply()
    }

    fun getSalt(): ByteArray? {
        val base64Salt = sharedPreferences.getString("user_salt", null) ?: return null
        return Base64.decode(base64Salt, Base64.NO_WRAP)
    }

    // ==========================================
    // CLEANUP (For Logout)
    // ==========================================
    fun clearVault() {
        sharedPreferences.edit().clear().apply()
    }
}