// com/ben/periodt/security/DbKeyManager.kt (same as before)
package com.ben.periodt.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random

private const val KS_ALIAS = "db_key_wrap"
private const val KS_PROVIDER = "AndroidKeyStore"
private const val PREFS = "secure_prefs"
private const val KEY_WRAPPED_DB = "wrapped_db_key"
private const val KEY_WRAPPED_IV = "wrapped_db_iv"

object DbKeyManager {
    fun getOrCreateDbPassphrase(context: Context): ByteArray {
        val ks = KeyStore.getInstance(KS_PROVIDER).apply { load(null) }
        val secretKey = if (!ks.containsAlias(KS_ALIAS)) generateKeystoreAesKey()
        else (ks.getEntry(KS_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey

        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wrapped = sp.getString(KEY_WRAPPED_DB, null)?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }
        val iv = sp.getString(KEY_WRAPPED_IV, null)?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }

        return if (wrapped != null && iv != null) {
            unwrap(secretKey, wrapped, iv)
        } else {
            val raw = Random.Default.nextBytes(32)
            val (ct, usedIv) = wrap(secretKey, raw)
            sp.edit()
                .putString(KEY_WRAPPED_DB, android.util.Base64.encodeToString(ct, android.util.Base64.NO_WRAP))
                .putString(KEY_WRAPPED_IV, android.util.Base64.encodeToString(usedIv, android.util.Base64.NO_WRAP))
                .apply()
            raw
        }
    }

    private fun generateKeystoreAesKey(): SecretKey {
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KS_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .build()
        kg.init(spec)
        return kg.generateKey()
    }

    private fun wrap(key: SecretKey, raw: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ct = cipher.doFinal(raw)
        return ct to iv
    }

    private fun unwrap(key: SecretKey, wrapped: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(wrapped)
    }
}
