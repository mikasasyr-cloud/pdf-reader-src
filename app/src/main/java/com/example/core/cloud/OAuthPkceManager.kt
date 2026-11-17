package com.example.core.cloud

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import com.example.core.common.AppError
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.safeSuspendCall
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom

class OAuthPkceManager(
    private val tokenStorage: TokenStorageManager,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val dispatchers: DispatcherProvider,
) {
    private val tokenAdapter = moshi.adapter(OAuthTokenResponse::class.java)

    /**
     * Launches the provider's official OAuth 2.0 Authorization Code + PKCE sign-in page
     * directly inside Android Custom Tabs (no Device Code Flow).
     */
    fun launchAuthorization(context: Context, provider: CloudProviderType): AppResult<Unit> {
        val spec = CloudConfig.oauthSpecFor(provider)
            ?: return AppResult.Error(AppError.Unknown("Provider $provider does not require OAuth"))

        if (spec.clientId.startsWith("REPLACE_WITH_")) {
            return AppResult.Error(
                AppError.Unauthorized(
                    "OAuth Client ID is not configured for ${provider.name}. Enable Demo Mode or set client IDs in CloudConfig.kt.",
                ),
            )
        }

        val codeVerifier = generateCodeVerifier()
        val codeChallenge = computeS256Challenge(codeVerifier)
        val state = generateRandomState()

        tokenStorage.savePendingPkceSession(
            PendingPkceSession(
                provider = provider,
                state = state,
                codeVerifier = codeVerifier,
            ),
        )

        val uriBuilder = Uri.parse(spec.authorizationEndpoint).buildUpon()
            .appendQueryParameter("client_id", spec.clientId)
            .appendQueryParameter("redirect_uri", CloudConfig.REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .appendQueryParameter("scope", spec.scopes.joinToString(" "))

        spec.extraAuthParams.forEach { (k, v) ->
            uriBuilder.appendQueryParameter(k, v)
        }

        return try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, uriBuilder.build())
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Error(
                AppError.Unknown(
                    message = throwable.localizedMessage ?: "Failed to open browser for sign-in",
                    cause = throwable,
                ),
            )
        }
    }

    /**
     * Handles `com.aistudio.qalamreader.xmpqtz://oauth2redirect?code=...&state=...`
     * and exchanges the authorization code for access + refresh tokens.
     */
    suspend fun handleRedirectUri(uri: Uri): AppResult<CloudProviderType> = withContext(dispatchers.io) {
        val errorParam = uri.getQueryParameter("error")
        if (!errorParam.isNullOrBlank()) {
            tokenStorage.clearPendingPkceSession()
            val desc = uri.getQueryParameter("error_description") ?: errorParam
            return@withContext AppResult.Error(AppError.Unauthorized(desc))
        }

        val code = uri.getQueryParameter("code")
        val returnedState = uri.getQueryParameter("state")
        val pending = tokenStorage.consumePendingPkceSession()
            ?: return@withContext AppResult.Error(AppError.Unauthorized("No active OAuth session"))

        if (code.isNullOrBlank() || returnedState != pending.state) {
            return@withContext AppResult.Error(AppError.Unauthorized("Invalid OAuth state or code"))
        }

        val spec = CloudConfig.oauthSpecFor(pending.provider)
            ?: return@withContext AppResult.Error(AppError.Unknown("Missing OAuth spec"))

        exchangeCode(spec, code, pending.codeVerifier)
    }

    suspend fun getValidAccessToken(provider: CloudProviderType): AppResult<String> = withContext(dispatchers.io) {
        val current = tokenStorage.getTokens(provider)
            ?: return@withContext AppResult.Error(AppError.Unauthorized("${provider.name} is not connected"))

        if (!current.isExpired()) {
            return@withContext AppResult.Success(current.accessToken)
        }

        val refreshToken = current.refreshToken
            ?: return@withContext AppResult.Error(AppError.Unauthorized("Session expired; please reconnect"))

        val spec = CloudConfig.oauthSpecFor(provider)
            ?: return@withContext AppResult.Error(AppError.Unauthorized("Missing OAuth config"))

        when (val refreshed = refreshTokens(spec, refreshToken, current.accountLabel)) {
            is AppResult.Success -> AppResult.Success(refreshed.data.accessToken)
            is AppResult.Error -> refreshed
            AppResult.Loading -> AppResult.Loading
        }
    }

    private suspend fun exchangeCode(
        spec: OAuthProviderSpec,
        code: String,
        codeVerifier: String,
    ): AppResult<CloudProviderType> = safeSuspendCall(
        errorMapper = { AppError.Network(it.localizedMessage ?: "Token exchange failed", it) },
    ) {
        val formBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("client_id", spec.clientId)
            .add("redirect_uri", CloudConfig.REDIRECT_URI)
            .add("code_verifier", codeVerifier)
            .build()

        val request = Request.Builder()
            .url(spec.tokenEndpoint)
            .post(formBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: $rawBody")
            }
            val parsed = tokenAdapter.fromJson(rawBody)
                ?: throw IllegalStateException("Empty OAuth token response")
            val expiresAt = parsed.expiresIn?.let { System.currentTimeMillis() + it * 1000L } ?: 0L
            tokenStorage.saveTokens(
                spec.provider,
                StoredOAuthTokens(
                    accessToken = parsed.accessToken,
                    refreshToken = parsed.refreshToken,
                    expiresAtMillis = expiresAt,
                    accountLabel = "Connected",
                ),
            )
            spec.provider
        }
    }

    private suspend fun refreshTokens(
        spec: OAuthProviderSpec,
        refreshToken: String,
        existingLabel: String?,
    ): AppResult<StoredOAuthTokens> = safeSuspendCall(
        errorMapper = { AppError.Unauthorized(it.localizedMessage ?: "Token refresh failed", it) },
    ) {
        val formBody = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", spec.clientId)
            .build()

        val request = Request.Builder()
            .url(spec.tokenEndpoint)
            .post(formBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: $rawBody")
            }
            val parsed = tokenAdapter.fromJson(rawBody)
                ?: throw IllegalStateException("Empty refresh response")
            val expiresAt = parsed.expiresIn?.let { System.currentTimeMillis() + it * 1000L } ?: 0L
            val updated = StoredOAuthTokens(
                accessToken = parsed.accessToken,
                refreshToken = parsed.refreshToken ?: refreshToken,
                expiresAtMillis = expiresAt,
                accountLabel = existingLabel,
            )
            tokenStorage.saveTokens(spec.provider, updated)
            updated
        }
    }

    companion object {
        fun generateCodeVerifier(): String {
            val bytes = ByteArray(64)
            SecureRandom().nextBytes(bytes)
            return Base64.encodeToString(
                bytes,
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
            )
        }

        fun computeS256Challenge(verifier: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.toByteArray(Charsets.US_ASCII))
            return Base64.encodeToString(
                digest,
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
            )
        }

        fun generateRandomState(): String {
            val bytes = ByteArray(24)
            SecureRandom().nextBytes(bytes)
            return Base64.encodeToString(
                bytes,
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class OAuthTokenResponse(
    @com.squareup.moshi.Json(name = "access_token") val accessToken: String,
    @com.squareup.moshi.Json(name = "refresh_token") val refreshToken: String? = null,
    @com.squareup.moshi.Json(name = "expires_in") val expiresIn: Long? = null,
    @com.squareup.moshi.Json(name = "token_type") val tokenType: String? = null,
)
