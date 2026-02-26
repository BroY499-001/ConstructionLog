package com.example.constructionlog.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class QWeatherKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun hasApiKey(): Boolean = prefs.getString(KEY_QWEATHER_API_KEY, null) != null
    fun hasApiHost(): Boolean = prefs.getString(KEY_QWEATHER_API_HOST, null)?.isNotBlank() == true

    fun saveApiKey(plainKey: String) {
        val value = plainKey.trim()
        require(value.isNotBlank()) { "Key 不能为空" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        val payload = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString(KEY_QWEATHER_API_KEY, payload).apply()
    }

    fun getApiKey(): String? {
        val payload = prefs.getString(KEY_QWEATHER_API_KEY, null) ?: return null
        val parts = payload.split(':')
        if (parts.size != 2) return null

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        val plain = cipher.doFinal(encrypted)
        return plain.toString(Charsets.UTF_8)
    }

    fun saveApiHost(hostInput: String) {
        val normalized = normalizeHost(hostInput)
        prefs.edit().putString(KEY_QWEATHER_API_HOST, normalized).apply()
    }

    fun getApiHost(): String? {
        val raw = prefs.getString(KEY_QWEATHER_API_HOST, null) ?: return null
        return raw.trim().ifBlank { null }
    }

    private fun normalizeHost(hostInput: String): String {
        val value = hostInput.trim()
        require(value.isNotBlank()) { "API Host 不能为空" }
        val withScheme = if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
        return withScheme.removeSuffix("/")
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val PREF_NAME = "qweather_secure_config"
        private const val KEY_QWEATHER_API_KEY = "qweather_api_key"
        private const val KEY_QWEATHER_API_HOST = "qweather_api_host"

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "construction_log_qweather_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
