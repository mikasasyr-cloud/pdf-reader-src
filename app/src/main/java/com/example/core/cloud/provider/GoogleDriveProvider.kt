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

class GoogleDriveProvider(
    private val oauthPkceManager: OAuthPkceManager,
    private val tokenStorage: TokenStorageManager,
    private val okHttpClient: OkHttpClient,
    moshi: Moshi,
    private val dispatchers: DispatcherProvider,
) : CloudProvider {

    override val providerType: CloudProviderType = CloudProviderType.GOOGLE_DRIVE

    private val listAdapter = moshi.adapter(DriveFileListResponse::class.java)

    override suspend fun isConnected(): Boolean = tokenStorage.getTokens(providerType) != null

    override suspend fun listFolder(folderId: String?): AppResult<List<CloudNode>> =
        withContext(dispatchers.io) {
            val tokenResult = oauthPkceManager.getValidAccessToken(providerType)
            if (tokenResult is AppResult.Error) return@withContext tokenResult
            val accessToken = (tokenResult as AppResult.Success).data

            safeSuspendCall(errorMapper = CloudDownloadHelper::mapHttpOrIoError) {
                val parent = folderId ?: "root"
                val query = "'$parent' in parents and trashed = false and " +
                    "(mimeType = '$FOLDER_MIME' or mimeType = '$PDF_MIME')"
                val url = Uri.parse("https://www.googleapis.com/drive/v3/files").buildUpon()
                    .appendQueryParameter("q", query)
                    .appendQueryParameter("pageSize", "100")
                    .appendQueryParameter("orderBy", "folder,name")
                    .appendQueryParameter(
                        "fields",
                        "files(id,name,mimeType,size,modifiedTime,md5Checksum)",
                    )
                    .build()
                    .toString()

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.code == 401) throw CloudAuthException("Google Drive session expired")
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Google Drive list failed: HTTP ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    val parsed = listAdapter.fromJson(body) ?: DriveFileListResponse(emptyList())
                    parsed.files.map { file ->
                        CloudNode(
                            id = file.id,
                            name = file.name,
                            isFolder = file.mimeType == FOLDER_MIME,
                            provider = providerType,
                            parentId = parent,
                            mimeType = file.mimeType,
                            sizeBytes = file.size?.toLongOrNull() ?: 0L,
                            contentHash = file.md5Checksum,
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
            val url = "https://www.googleapis.com/drive/v3/files/${Uri.encode(fileId)}?alt=media"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val targetFile = File(
                File(cacheDir, "google_drive"),
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

    private companion object {
        const val FOLDER_MIME = "application/vnd.google-apps.folder"
        const val PDF_MIME = "application/pdf"
    }
}

@JsonClass(generateAdapter = true)
data class DriveFileListResponse(
    val files: List<DriveFileItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DriveFileItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: String? = null,
    val modifiedTime: String? = null,
    val md5Checksum: String? = null,
)
