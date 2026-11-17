package com.example.core.cloud

import com.example.core.cloud.model.CloudNode
import com.example.core.cloud.model.DownloadedPdfDocument
import com.example.core.common.AppResult
import java.io.File

interface CloudProvider {
    val providerType: CloudProviderType

    suspend fun isConnected(): Boolean

    /**
     * Lazy-loads ONLY the direct subfolders and PDF files inside [folderId].
     * When [folderId] is null, lists the root folder.
     */
    suspend fun listFolder(folderId: String? = null): AppResult<List<CloudNode>>

    suspend fun downloadPdf(
        fileId: String,
        fileName: String,
        cacheDir: File,
    ): AppResult<DownloadedPdfDocument>

    suspend fun disconnect(): AppResult<Unit>
}
