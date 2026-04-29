package com.ben.periodt.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12 // GCM strictly requires a 12-byte IV

    // TEMPORARY: A 32-byte (256-bit) static key for our network test.
    private val TEST_DEK = SecretKeySpec(
        "PeriodtSecureTestKey123456789012".toByteArray(),
        "AES"
    )

    fun encrypt(plainJson: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)

        // 1. Generate a random Initialization Vector (IV) for every payload
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)

        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, TEST_DEK, parameterSpec)

        // 2. Encrypt the data
        val cipherText = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        // 3. Prepend the IV to the ciphertext and Base64 encode it for network transport
        val combined = iv + cipherText
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String): String {
        // 1. Decode the Base64 string back into bytes
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        // 2. Extract the IV from the beginning of the byte array
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTE)
        val cipherText = combined.copyOfRange(IV_LENGTH_BYTE, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

        // 3. Decrypt the data back to plain JSON
        cipher.init(Cipher.DECRYPT_MODE, TEST_DEK, parameterSpec)
        val plainText = cipher.doFinal(cipherText)

        return String(plainText, Charsets.UTF_8)
    }
}