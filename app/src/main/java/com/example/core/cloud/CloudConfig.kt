package com.example.core.cloud

/**
 * Centralized OAuth 2.0 PKCE client configuration for Qalam's 4 cloud providers.
 *
 * Setup instructions before releasing with real cloud accounts:
 * 1. [REDIRECT_URI]: Must match `<data android:scheme="com.aistudio.qalamreader.xmpqtz" android:host="oauth2redirect" />`
 *    in `app/src/main/AndroidManifest.xml`.
 * 2. [GOOGLE_CLIENT_ID]: Create an Android/Installed OAuth 2.0 Client ID in Google Cloud Console
 *    with custom URI scheme support enabled and Drive read-only scope (`drive.readonly`).
 * 3. [ONEDRIVE_CLIENT_ID]: Register a Mobile/Desktop public client in Microsoft Entra ID (Azure Portal)
 *    and add [REDIRECT_URI] under Mobile and desktop applications redirect URIs.
 * 4. [DROPBOX_APP_KEY]: Create a scoped Dropbox App in Dropbox App Console with `files.metadata.read`
 *    and `files.content.read` permissions and register [REDIRECT_URI].
 * 5. [BOX_CLIENT_ID]: Create a Custom App (OAuth 2.0 PKCE / Public Client) in Box Developer Console
 *    and register [REDIRECT_URI] as the OAuth 2.0 Redirect URI.
 */
object CloudConfig {
    const val REDIRECT_URI = "com.aistudio.qalamreader.xmpqtz://oauth2redirect"

    const val GOOGLE_CLIENT_ID = "REPLACE_WITH_GOOGLE_OAUTH_CLIENT_ID"
    const val ONEDRIVE_CLIENT_ID = "REPLACE_WITH_ONEDRIVE_CLIENT_ID"
    const val DROPBOX_APP_KEY = "REPLACE_WITH_DROPBOX_APP_KEY"
    const val BOX_CLIENT_ID = "REPLACE_WITH_BOX_CLIENT_ID"

    fun oauthSpecFor(provider: CloudProviderType): OAuthProviderSpec? = when (provider) {
        CloudProviderType.GOOGLE_DRIVE -> OAuthProviderSpec(
            provider = provider,
            clientId = GOOGLE_CLIENT_ID,
            authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
            tokenEndpoint = "https://oauth2.googleapis.com/token",
            scopes = listOf(
                "https://www.googleapis.com/auth/drive.readonly",
                "https://www.googleapis.com/auth/userinfo.email",
            ),
        )
        CloudProviderType.ONEDRIVE -> OAuthProviderSpec(
            provider = provider,
            clientId = ONEDRIVE_CLIENT_ID,
            authorizationEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
            tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
            scopes = listOf("Files.Read", "User.Read", "offline_access"),
        )
        CloudProviderType.DROPBOX -> OAuthProviderSpec(
            provider = provider,
            clientId = DROPBOX_APP_KEY,
            authorizationEndpoint = "https://www.dropbox.com/oauth2/authorize",
            tokenEndpoint = "https://api.dropboxapi.com/oauth2/token",
            scopes = listOf("files.metadata.read", "files.content.read", "account_info.read"),
            extraAuthParams = mapOf("token_access_type" to "offline"),
        )
        CloudProviderType.BOX -> OAuthProviderSpec(
            provider = provider,
            clientId = BOX_CLIENT_ID,
            authorizationEndpoint = "https://account.box.com/api/oauth2/authorize",
            tokenEndpoint = "https://api.box.com/oauth2/token",
            scopes = listOf("root_readonly"),
        )
        CloudProviderType.LOCAL,
        CloudProviderType.SAMPLE -> null
    }
}

enum class CloudProviderType {
    GOOGLE_DRIVE,
    ONEDRIVE,
    DROPBOX,
    BOX,
    LOCAL,
    SAMPLE;

    companion object {
        val cloudProviders: List<CloudProviderType> = listOf(
            GOOGLE_DRIVE,
            ONEDRIVE,
            DROPBOX,
            BOX,
        )

        fun fromString(raw: String?): CloudProviderType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: LOCAL
    }
}

data class OAuthProviderSpec(
    val provider: CloudProviderType,
    val clientId: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val scopes: List<String>,
    val extraAuthParams: Map<String, String> = emptyMap(),
)
