package com.example.core.cloud

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.cloud.model.CloudAccountStatus
import com.example.core.cloud.model.CloudNode
import com.example.core.cloud.model.DownloadedPdfDocument
import com.example.core.cloud.provider.MockCloudProvider
import com.example.core.common.AppError
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.safeSuspendCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

private val Context.qalamSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "qalam_user_preferences",
)

class CloudRepository(
    private val context: Context,
    private val tokenStorage: TokenStorageManager,
    private val oauthPkceManager: OAuthPkceManager,
    private val realProviders: Map<CloudProviderType, CloudProvider>,
    private val samplePdfGenerator: SamplePdfGenerator,
    private val dispatchers: DispatcherProvider,
) {
    private val dataStore = context.applicationContext.qalamSettingsDataStore
    private val pdfCacheDir = File(context.cacheDir, "pdf_cache")

    private val mockProviders: Map<CloudProviderType, MockCloudProvider> =
        CloudProviderType.cloudProviders.associateWith { type ->
            MockCloudProvider(type, samplePdfGenerator)
        }

    private val _accountStatuses = MutableStateFlow<List<CloudAccountStatus>>(emptyList())
    val accountStatuses: StateFlow<List<CloudAccountStatus>> = _accountStatuses.asStateFlow()

    val isDemoMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DEMO_MODE] ?: true
    }

    val themeModeName: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val dynamicColorEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    }

    suspend fun setDemoMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DEMO_MODE] = enabled
        }
        refreshAccountStatuses()
    }

    suspend fun setThemeMode(modeName: String) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = modeName
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun refreshAccountStatuses() {
        val demo = isDemoMode.first()
        val list = CloudProviderType.cloudProviders.map { type ->
            if (demo) {
                CloudAccountStatus(
                    provider = type,
                    isConnected = true,
                    accountLabel = "Sandbox",
                    isDemo = true,
                )
            } else {
                val tokens = tokenStorage.getTokens(type)
                CloudAccountStatus(
                    provider = type,
                    isConnected = tokens != null,
                    accountLabel = tokens?.accountLabel,
                    isDemo = false,
                )
            }
        }
        _accountStatuses.value = list
    }

    fun startOAuthLogin(activityContext: Context, provider: CloudProviderType): AppResult<Unit> =
        oauthPkceManager.launchAuthorization(activityContext, provider)

    suspend fun handleOAuthRedirect(uri: Uri): AppResult<CloudProviderType> {
        val result = oauthPkceManager.handleRedirectUri(uri)
        if (result is AppResult.Success) {
            refreshAccountStatuses()
        }
        return result
    }

    suspend fun disconnectProvider(provider: CloudProviderType): AppResult<Unit> {
        val res = realProviders[provider]?.disconnect() ?: AppResult.Success(Unit)
        refreshAccountStatuses()
        return res
    }

    suspend fun listFolder(
        provider: CloudProviderType,
        folderId: String? = null,
    ): AppResult<List<CloudNode>> {
        val activeProvider = resolveProvider(provider)
            ?: return AppResult.Error(AppError.NotFound("Unsupported provider: $provider"))
        return activeProvider.listFolder(folderId)
    }

    suspend fun openCloudPdf(
        provider: CloudProviderType,
        fileId: String,
        fileName: String,
    ): AppResult<DownloadedPdfDocument> {
        if (provider == CloudProviderType.SAMPLE) {
            return samplePdfGenerator.ensureSampleGuidePdf()
        }
        val activeProvider = resolveProvider(provider)
            ?: return AppResult.Error(AppError.NotFound("Unsupported provider: $provider"))
        return activeProvider.downloadPdf(fileId, fileName, pdfCacheDir)
    }

    suspend fun openSampleGuide(): AppResult<DownloadedPdfDocument> =
        samplePdfGenerator.ensureSampleGuidePdf()

    suspend fun importLocalPdf(uri: Uri): AppResult<DownloadedPdfDocument> =
        withContext(dispatchers.io) {
            safeSuspendCall(
                errorMapper = { AppError.Storage(it.localizedMessage ?: "Failed to import PDF", it) },
            ) {
                val displayName = queryDisplayName(uri) ?: "document.pdf"
                val localDir = File(pdfCacheDir, "local")
                localDir.mkdirs()
                val safeKey = uri.toString().hashCode().toUInt().toString(16)
                val destFile = File(
                    localDir,
                    "${safeKey}_${CloudDownloadHelper.sanitizeFileName(displayName, safeKey)}",
                )
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Unable to open selected file")
                val (bytes, sha256) = CloudDownloadHelper.writeStreamAndHash(inputStream, destFile)
                DownloadedPdfDocument(
                    provider = CloudProviderType.LOCAL,
                    cloudFileId = uri.toString(),
                    title = displayName,
                    documentHash = sha256,
                    localFilePath = destFile.absolutePath,
                    totalBytes = bytes,
                )
            }
        }

    suspend fun clearPdfCache(): AppResult<Unit> = withContext(dispatchers.io) {
        safeSuspendCall(
            errorMapper = { AppError.Storage(it.localizedMessage ?: "Failed to clear cache", it) },
        ) {
            if (pdfCacheDir.exists()) {
                pdfCacheDir.deleteRecursively()
            }
            Unit
        }
    }

    suspend fun calculateCacheSizeBytes(): Long = withContext(dispatchers.io) {
        if (!pdfCacheDir.exists()) return@withContext 0L
        pdfCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private suspend fun resolveProvider(provider: CloudProviderType): CloudProvider? {
        val demo = isDemoMode.first()
        return if (demo) mockProviders[provider] else realProviders[provider]
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else {
                        null
                    }
                }
        }.getOrNull()
    }

    private companion object {
        val KEY_DEMO_MODE = booleanPreferencesKey("demo_mode_enabled")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
    }
}
