package com.example.core.cloud

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.core.common.AppLogger

data class StoredOAuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtMillis: Long = 0L,
    val accountLabel: String? = null,
) {
    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
        expiresAtMillis > 0L && nowMillis >= (expiresAtMillis - 60_000L)
}

data class PendingPkceSession(
    val provider: CloudProviderType,
    val state: String,
    val codeVerifier: String,
)

class TokenStorageManager(context: Context) {

    private val prefs: SharedPreferences = createSecurePrefs(context.applicationContext)

    fun saveTokens(provider: CloudProviderType, tokens: StoredOAuthTokens) {
        prefs.edit()
            .putString(key(provider, "access"), tokens.accessToken)
            .putString(key(provider, "refresh"), tokens.refreshToken)
            .putLong(key(provider, "expires"), tokens.expiresAtMillis)
            .putString(key(provider, "account"), tokens.accountLabel)
            .apply()
    }

    fun getTokens(provider: CloudProviderType): StoredOAuthTokens? {
        val access = prefs.getString(key(provider, "access"), null) ?: return null
        return StoredOAuthTokens(
            accessToken = access,
            refreshToken = prefs.getString(key(provider, "refresh"), null),
            expiresAtMillis = prefs.getLong(key(provider, "expires"), 0L),
            accountLabel = prefs.getString(key(provider, "account"), null),
        )
    }

    fun clearTokens(provider: CloudProviderType) {
        prefs.edit()
            .remove(key(provider, "access"))
            .remove(key(provider, "refresh"))
            .remove(key(provider, "expires"))
            .remove(key(provider, "account"))
            .apply()
    }

    fun savePendingPkceSession(session: PendingPkceSession) {
        prefs.edit()
            .putString(KEY_PENDING_PROVIDER, session.provider.name)
            .putString(KEY_PENDING_STATE, session.state)
            .putString(KEY_PENDING_VERIFIER, session.codeVerifier)
            .apply()
    }

    fun consumePendingPkceSession(): PendingPkceSession? {
        val providerName = prefs.getString(KEY_PENDING_PROVIDER, null) ?: return null
        val state = prefs.getString(KEY_PENDING_STATE, null) ?: return null
        val verifier = prefs.getString(KEY_PENDING_VERIFIER, null) ?: return null
        clearPendingPkceSession()
        return PendingPkceSession(
            provider = CloudProviderType.fromString(providerName),
            state = state,
            codeVerifier = verifier,
        )
    }

    fun clearPendingPkceSession() {
        prefs.edit()
            .remove(KEY_PENDING_PROVIDER)
            .remove(KEY_PENDING_STATE)
            .remove(KEY_PENDING_VERIFIER)
            .apply()
    }

    private fun key(provider: CloudProviderType, suffix: String): String =
        "oauth_${provider.name.lowercase()}_$suffix"

    private fun createSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (throwable: Throwable) {
            AppLogger.w(
                "TokenStorageManager",
                "Falling back to standard SharedPreferences in restricted keystore environment",
                throwable,
            )
            context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    private companion object {
        const val PREFS_NAME = "qalam_oauth_secure_prefs"
        const val KEY_PENDING_PROVIDER = "pending_pkce_provider"
        const val KEY_PENDING_STATE = "pending_pkce_state"
        const val KEY_PENDING_VERIFIER = "pending_pkce_verifier"
    }
}
