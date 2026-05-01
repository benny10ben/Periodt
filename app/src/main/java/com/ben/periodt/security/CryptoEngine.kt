package com.ben.periodt.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    // 1. Generate the AES Key from the User's Password
    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        // We use 600,000 iterations to purposefully slow down brute-force attacks
        val iterationCount = 600_000
        val keyLength = 256

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength)
        val secretKey = factory.generateSecret(spec)

        return SecretKeySpec(secretKey.encoded, "AES")
    }

    // 2. Generate a random 16-byte Salt for new users
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun encrypt(plainJson: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(ALGORITHM)

        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)

        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val cipherText = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        val combined = iv + cipherText

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String, secretKey: SecretKey): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTE)
        val cipherText = combined.copyOfRange(IV_LENGTH_BYTE, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        val plainText = cipher.doFinal(cipherText)

        return String(plainText, Charsets.UTF_8)
    }
}