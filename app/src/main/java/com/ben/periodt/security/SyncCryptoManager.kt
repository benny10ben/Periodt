package com.ben.periodt.security

import android.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles End-to-End Encryption (E2EE) for cloud synchronization.
 * Implements the Two-Layer Key Wrapping Scheme:
 * 1. Password + Salt -> Argon2id -> Account Key
 * 2. Random Data Key -> Encrypted by Account Key (Stored on server)
 * 3. Health Data -> Encrypted by Data Key
 */
object SyncCryptoManager {

    private const val AES_GCM_ALGO = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    // Held in memory only while the user is actively logged in.
    // Must be cleared when the app is completely closed or user logs out.
    var sessionDataKey: ByteArray? = null

    /**
     * Generates a secure, 16-byte random salt and encodes it to Base64 for the server.
     */
    fun generateSaltBase64(): String {
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    /**
     * LAYER 1: Derives the 256-bit Account Key from the user's password and salt.
     * Uses Argon2id, the current industry standard for password hashing/KDF.
     */
    fun deriveAccountKey(password: String, saltBase64: String): ByteArray {
        val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
        val passwordBytes = password.toByteArray(Charsets.UTF_8)

        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(3)
            .withMemoryAsKB(65536) // 64 MB
            .withParallelism(4)
            .withSalt(saltBytes)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(parameters)

        // Generate a 32-byte (256-bit) AES key
        val accountKey = ByteArray(32)
        generator.generateBytes(passwordBytes, accountKey, 0, accountKey.size)
        return accountKey
    }

    /**
     * LAYER 2: Generates a completely random 256-bit Data Key.
     * Called ONLY ONCE when the user first sets up their cloud account.
     */
    fun generateRandomDataKey(): ByteArray {
        val dataKey = ByteArray(32)
        SecureRandom().nextBytes(dataKey)
        return dataKey
    }

    /**
     * Wraps (encrypts) the Data Key using the Account Key so it can be safely
     * uploaded and stored on the Spring Boot server.
     */
    fun wrapDataKey(accountKey: ByteArray, dataKey: ByteArray): String {
        return encryptAesGcm(accountKey, dataKey)
    }

    /**
     * Unwraps (decrypts) the Wrapped Data Key downloaded from the server
     * using the user's locally derived Account Key.
     */
    fun unwrapDataKey(accountKey: ByteArray, wrappedDataKeyBase64: String): ByteArray {
        return decryptAesGcm(accountKey, wrappedDataKeyBase64)
    }

    /**
     * Encrypts a JSON SyncPayload string using the active session Data Key.
     * Throws an exception if the Data Key is not loaded.
     */
    fun encryptPayload(jsonPayload: String): String {
        val key = sessionDataKey ?: throw IllegalStateException("Data Key not loaded in session")
        return encryptAesGcm(key, jsonPayload.toByteArray(Charsets.UTF_8))
    }

    /**
     * Decrypts an encrypted payload from the server back into a JSON string.
     */
    fun decryptPayload(encryptedBase64: String): String {
        val key = sessionDataKey ?: throw IllegalStateException("Data Key not loaded in session")
        val decryptedBytes = decryptAesGcm(key, encryptedBase64)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Core AES-256-GCM Encryption Engine.
     * Creates a self-contained blob: [ 12-byte IV ] + [ Ciphertext + Auth Tag ]
     */
    private fun encryptAesGcm(key: ByteArray, plaintext: ByteArray): String {
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance(AES_GCM_ALGO)

        // Generate a fresh 12-byte IV for every single encryption operation
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        val ciphertextWithTag = cipher.doFinal(plaintext)

        // Combine IV and Ciphertext into a single byte array
        val combined = ByteBuffer.allocate(iv.size + ciphertextWithTag.size)
            .put(iv)
            .put(ciphertextWithTag)
            .array()

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Core AES-256-GCM Decryption Engine.
     * Extracts the IV from the front of the blob and decrypts the rest.
     */
    private fun decryptAesGcm(key: ByteArray, encryptedBase64: String): ByteArray {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(combined)

        // Extract the 12-byte IV
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        buffer.get(iv)

        // Extract the remaining ciphertext + tag
        val ciphertextWithTag = ByteArray(buffer.remaining())
        buffer.get(ciphertextWithTag)

        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance(AES_GCM_ALGO)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        return cipher.doFinal(ciphertextWithTag)
    }

    /**
     * Wipes the Data Key from memory for security (e.g., on logout).
     */
    fun clearSession() {
        sessionDataKey?.fill(0) // Zero out the byte array before garbage collection
        sessionDataKey = null
    }
}