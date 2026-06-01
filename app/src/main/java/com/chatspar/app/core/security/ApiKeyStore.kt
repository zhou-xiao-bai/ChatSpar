package com.chatspar.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.chatspar.app.domain.model.AiProviderConfig
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface ApiKeyStore {
    fun saveApiKey(apiKey: String) {
        saveApiKey(ApiKeyAlias.LEGACY, apiKey)
    }

    fun getApiKey(): String? {
        return getApiKey(ApiKeyAlias.LEGACY)
    }

    fun clearApiKey() {
        clearApiKey(ApiKeyAlias.LEGACY)
    }

    fun saveApiKey(alias: String, apiKey: String)
    fun getApiKey(alias: String): String?
    fun clearApiKey(alias: String)
    fun clearAllApiKeys()
}

class AndroidKeystoreApiKeyStore(
    context: Context,
) : ApiKeyStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun saveApiKey(alias: String, apiKey: String) {
        val normalizedAlias = alias.normalizedApiKeyAlias()
        if (apiKey.isBlank()) {
            clearApiKey(normalizedAlias)
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val encrypted = cipher.doFinal(apiKey.toByteArray(StandardCharsets.UTF_8))
        preferences.edit()
            .putString(ciphertextPreferenceKey(normalizedAlias), Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(ivPreferenceKey(normalizedAlias), Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putStringSet(
                KEY_ALIASES,
                preferences.getStringSet(KEY_ALIASES, emptySet()).orEmpty() + normalizedAlias,
            )
            .apply()
    }

    override fun getApiKey(alias: String): String? {
        val normalizedAlias = alias.normalizedApiKeyAlias()
        val ciphertext = preferences.getString(ciphertextPreferenceKey(normalizedAlias), null)
            ?: preferences.getLegacyCiphertextForAlias(normalizedAlias)
            ?: return null
        val iv = preferences.getString(ivPreferenceKey(normalizedAlias), null)
            ?: preferences.getLegacyIvForAlias(normalizedAlias)
            ?: return null

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP)),
            )
            val decrypted = cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP))
            String(decrypted, StandardCharsets.UTF_8)
        }.getOrElse { throwable ->
            if (throwable is GeneralSecurityException || throwable is IllegalArgumentException) {
                clearApiKey(normalizedAlias)
                null
            } else {
                throw throwable
            }
        }
    }

    override fun clearApiKey(alias: String) {
        val normalizedAlias = alias.normalizedApiKeyAlias()
        preferences.edit()
            .remove(ciphertextPreferenceKey(normalizedAlias))
            .remove(ivPreferenceKey(normalizedAlias))
            .removeLegacyKeysForAlias(normalizedAlias)
            .putStringSet(
                KEY_ALIASES,
                preferences.getStringSet(KEY_ALIASES, emptySet()).orEmpty() - normalizedAlias,
            )
            .apply()
    }

    override fun clearAllApiKeys() {
        val aliases = preferences.getStringSet(KEY_ALIASES, emptySet()).orEmpty()
        preferences.edit().apply {
            aliases.forEach { alias ->
                remove(ciphertextPreferenceKey(alias))
                remove(ivPreferenceKey(alias))
            }
            remove(OLD_KEY_CIPHERTEXT)
            remove(OLD_KEY_IV)
            remove(KEY_ALIASES)
        }.apply()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE,
        )
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    private fun ciphertextPreferenceKey(alias: String): String {
        return "$KEY_CIPHERTEXT_PREFIX$alias"
    }

    private fun ivPreferenceKey(alias: String): String {
        return "$KEY_IV_PREFIX$alias"
    }

    private fun android.content.SharedPreferences.getLegacyCiphertextForAlias(alias: String): String? {
        if (alias != ApiKeyAlias.LEGACY) {
            return null
        }
        return getString(OLD_KEY_CIPHERTEXT, null)
    }

    private fun android.content.SharedPreferences.getLegacyIvForAlias(alias: String): String? {
        if (alias != ApiKeyAlias.LEGACY) {
            return null
        }
        return getString(OLD_KEY_IV, null)
    }

    private fun android.content.SharedPreferences.Editor.removeLegacyKeysForAlias(
        alias: String,
    ): android.content.SharedPreferences.Editor {
        if (alias == ApiKeyAlias.LEGACY) {
            remove(OLD_KEY_CIPHERTEXT)
            remove(OLD_KEY_IV)
        }
        return this
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "chatspar_api_key"
        const val PREFERENCES_NAME = "encrypted_api_key"
        const val KEY_CIPHERTEXT_PREFIX = "ciphertext_"
        const val KEY_IV_PREFIX = "iv_"
        const val OLD_KEY_CIPHERTEXT = "ciphertext"
        const val OLD_KEY_IV = "iv"
        const val KEY_ALIASES = "aliases"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}

object ApiKeyAlias {
    const val LEGACY = AiProviderConfig.LEGACY_API_KEY_ALIAS
}

fun String.normalizedApiKeyAlias(): String {
    return trim()
        .ifBlank { ApiKeyAlias.LEGACY }
        .map { character ->
            if (character.isLetterOrDigit() || character == '_' || character == '-') {
                character
            } else {
                '_'
            }
        }
        .joinToString(separator = "")
}
