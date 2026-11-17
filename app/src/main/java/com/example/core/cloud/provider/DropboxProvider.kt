package com.example.core.cloud.provider

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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class DropboxProvider(
    private val oauthPkceManager: OAuthPkceManager,
    private val tokenStorage: TokenStorageManager,
    private val okHttpClient: OkHttpClient,
    moshi: Moshi,
    private val dispatchers: DispatcherProvider,
) : CloudProvider {

    override val providerType: CloudProviderType = CloudProviderType.DROPBOX

    private val listAdapter = moshi.adapter(DropboxListFolderResponse::class.java)

    override suspend fun isConnected(): Boolean = tokenStorage.getTokens(providerType) != null

    override suspend fun listFolder(folderId: String?): AppResult<List<CloudNode>> =
        withContext(dispatchers.io) {
            val tokenResult = oauthPkceManager.getValidAccessToken(providerType)
            if (tokenResult is AppResult.Error) return@withContext tokenResult
            val accessToken = (tokenResult as AppResult.Success).data

            safeSuspendCall(errorMapper = CloudDownloadHelper::mapHttpOrIoError) {
                val path = folderId ?: ""
                val payload = JSONObject()
                    .put("path", path)
                    .put("recursive", false)
                    .put("include_deleted", false)
                    .toString()

                val request = Request.Builder()
                    .url("https://api.dropboxapi.com/2/files/list_folder")
                    .header("Authorization", "Bearer $accessToken")
                    .post(payload.toRequestBody(JSON_MEDIA))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.code == 401) throw CloudAuthException("Dropbox session expired")
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Dropbox list failed: HTTP ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    val parsed = listAdapter.fromJson(body) ?: DropboxListFolderResponse(emptyList())
                    parsed.entries
                        .filter { entry ->
                            entry.tag == "folder" || entry.name.endsWith(".pdf", ignoreCase = true)
                        }
                        .sortedWith(compareByDescending<DropboxEntry> { it.tag == "folder" }.thenBy { it.name })
                        .map { entry ->
                            val isFolder = entry.tag == "folder"
                            CloudNode(
                                id = entry.pathLower ?: entry.pathDisplay ?: entry.id ?: entry.name,
                                name = entry.name,
                                isFolder = isFolder,
                                provider = providerType,
                                parentId = folderId,
                                mimeType = if (isFolder) "folder" else "application/pdf",
                                sizeBytes = entry.size ?: 0L,
                                contentHash = entry.contentHash,
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
            val apiArg = JSONObject().put("path", fileId).toString()
            val request = Request.Builder()
                .url("https://content.dropboxapi.com/2/files/download")
                .header("Authorization", "Bearer $accessToken")
                .header("Dropbox-API-Arg", apiArg)
                .post("".toRequestBody(null))
                .build()

            val safeKey = fileId.hashCode().toUInt().toString(16)
            val targetFile = File(
                File(cacheDir, "dropbox"),
                "${safeKey}_${CloudDownloadHelper.sanitizeFileName(fileName, safeKey)}",
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

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}

@JsonClass(generateAdapter = true)
data class DropboxListFolderResponse(
    val entries: List<DropboxEntry> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DropboxEntry(
    @Json(name = ".tag") val tag: String,
    val name: String,
    val id: String? = null,
    @Json(name = "path_lower") val pathLower: String? = null,
    @Json(name = "path_display") val pathDisplay: String? = null,
    val size: Long? = null,
    @Json(name = "content_hash") val contentHash: String? = null,
)
