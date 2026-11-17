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
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class OneDriveProvider(
    private val oauthPkceManager: OAuthPkceManager,
    private val tokenStorage: TokenStorageManager,
    private val okHttpClient: OkHttpClient,
    moshi: Moshi,
    private val dispatchers: DispatcherProvider,
) : CloudProvider {

    override val providerType: CloudProviderType = CloudProviderType.ONEDRIVE

    private val childrenAdapter = moshi.adapter(GraphChildrenResponse::class.java)

    override suspend fun isConnected(): Boolean = tokenStorage.getTokens(providerType) != null

    override suspend fun listFolder(folderId: String?): AppResult<List<CloudNode>> =
        withContext(dispatchers.io) {
            val tokenResult = oauthPkceManager.getValidAccessToken(providerType)
            if (tokenResult is AppResult.Error) return@withContext tokenResult
            val accessToken = (tokenResult as AppResult.Success).data

            safeSuspendCall(errorMapper = CloudDownloadHelper::mapHttpOrIoError) {
                val baseUrl = if (folderId.isNullOrBlank()) {
                    "https://graph.microsoft.com/v1.0/me/drive/root/children"
                } else {
                    "https://graph.microsoft.com/v1.0/me/drive/items/${Uri.encode(folderId)}/children"
                }
                val url = "$baseUrl?\$select=id,name,size,folder,file,@microsoft.graph.downloadUrl"

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.code == 401) throw CloudAuthException("OneDrive session expired")
                    if (!response.isSuccessful) {
                        throw IllegalStateException("OneDrive list failed: HTTP ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    val parsed = childrenAdapter.fromJson(body) ?: GraphChildrenResponse(emptyList())
                    parsed.value
                        .filter { item ->
                            item.folder != null || item.name.endsWith(".pdf", ignoreCase = true) ||
                                item.file?.mimeType == "application/pdf"
                        }
                        .sortedWith(compareByDescending<GraphDriveItem> { it.folder != null }.thenBy { it.name })
                        .map { item ->
                            CloudNode(
                                id = item.id,
                                name = item.name,
                                isFolder = item.folder != null,
                                provider = providerType,
                                parentId = folderId,
                                mimeType = if (item.folder != null) "folder" else item.file?.mimeType ?: "application/pdf",
                                sizeBytes = item.size ?: 0L,
                                downloadUrl = item.downloadUrl,
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
            val url = "https://graph.microsoft.com/v1.0/me/drive/items/${Uri.encode(fileId)}/content"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val targetFile = File(
                File(cacheDir, "onedrive"),
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
data class GraphChildrenResponse(
    val value: List<GraphDriveItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GraphDriveItem(
    val id: String,
    val name: String,
    val size: Long? = null,
    val folder: GraphFolderFacet? = null,
    val file: GraphFileFacet? = null,
    @Json(name = "@microsoft.graph.downloadUrl") val downloadUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class GraphFolderFacet(
    val childCount: Int? = null,
)

@JsonClass(generateAdapter = true)
data class GraphFileFacet(
    val mimeType: String? = null,
)
