package com.example.core.cloud.provider

import android.net.Uri
import com.example.core.cloud.CloudAuthException
import com.example.core.cloud.CloudDownloadHelper
import com.example.core.cloud.CloudProvider
import com.example.core.cloud.CloudProviderType
import com.example.core.cloud.OAuthPkceManager
import com.example.core.cloud.TokenStorageManager
import com.example.core.cloud.model.CloudNode
import com.example.core.cloud.model.DownloadedPdfDocument
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.safeSuspendCall
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class BoxProvider(
    private val oauthPkceManager: OAuthPkceManager,
    private val tokenStorage: TokenStorageManager,
    private val okHttpClient: OkHttpClient,
    moshi: Moshi,
    private val dispatchers: DispatcherProvider,
) : CloudProvider {

    override val providerType: CloudProviderType = CloudProviderType.BOX

    private val folderItemsAdapter = moshi.adapter(BoxFolderItemsResponse::class.java)

    override suspend fun isConnected(): Boolean = tokenStorage.getTokens(providerType) != null

    override suspend fun listFolder(folderId: String?): AppResult<List<CloudNode>> =
        withContext(dispatchers.io) {
            val tokenResult = oauthPkceManager.getValidAccessToken(providerType)
            if (tokenResult is AppResult.Error) return@withContext tokenResult
            val accessToken = (tokenResult as AppResult.Success).data

            safeSuspendCall(errorMapper = CloudDownloadHelper::mapHttpOrIoError) {
                val targetFolderId = folderId ?: "0"
                val url = "https://api.box.com/2.0/folders/${Uri.encode(targetFolderId)}/items" +
                    "?fields=id,type,name,size,sha1&limit=100"

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.code == 401) throw CloudAuthException("Box session expired")
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Box list failed: HTTP ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    val parsed = folderItemsAdapter.fromJson(body) ?: BoxFolderItemsResponse(emptyList())
                    parsed.entries
                        .filter { entry ->
                            entry.type == "folder" ||
                                (entry.type == "file" && entry.name.endsWith(".pdf", ignoreCase = true))
                        }
                        .sortedWith(compareByDescending<BoxEntry> { it.type == "folder" }.thenBy { it.name })
                        .map { entry ->
                            val isFolder = entry.type == "folder"
                            CloudNode(
                                id = entry.id,
                                name = entry.name,
                                isFolder = isFolder,
                                provider = providerType,
                                parentId = targetFolderId,
                                mimeType = if (isFolder) "folder" else "application/pdf",
                                sizeBytes = entry.size ?: 0L,
                                contentHash = entry.sha1,
                            )
                        }
                }
            }
        }

    override suspend fun downloadPdf(
        fileId: String,
        fileName: String,
        cacheDir: File,
    ): AppResult<DownloadedPdfDocument> = withContext(dispatchers.io) {
        val tokenResult = oauthPkceManager.getValidAccessToken(providerType)
        if (tokenResult is AppResult.Error) return@withContext tokenResult
        val accessToken = (tokenResult as AppResult.Success).data

        safeSuspendCall(errorMapper = CloudDownloadHelper::mapHttpOrIoError) {
            val url = "https://api.box.com/2.0/files/${Uri.encode(fileId)}/content"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val targetFile = File(
                File(cacheDir, "box"),
                "${fileId}_${CloudDownloadHelper.sanitizeFileName(fileName, fileId)}",
            )
            val (bytes, sha256) = CloudDownloadHelper.executeDownloadToFile(
                okHttpClient = okHttpClient,
                request = request,
                destinationFile = targetFile,
            )
            DownloadedPdfDocument(
                provider = providerType,
                cloudFileId = fileId,
                title = fileName,
                documentHash = sha256,
                localFilePath = targetFile.absolutePath,
                totalBytes = bytes,
            )
        }
    }

    override suspend fun disconnect(): AppResult<Unit> {
        tokenStorage.clearTokens(providerType)
        return AppResult.Success(Unit)
    }
}

@JsonClass(generateAdapter = true)
data class BoxFolderItemsResponse(
    val entries: List<BoxEntry> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BoxEntry(
    val id: String,
    val type: String,
    val name: String,
    val size: Long? = null,
    val sha1: String? = null,
)
