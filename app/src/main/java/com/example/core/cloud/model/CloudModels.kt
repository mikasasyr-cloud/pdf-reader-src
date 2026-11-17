package com.example.core.cloud.model

import com.example.core.cloud.CloudProviderType

data class CloudNode(
    val id: String,
    val name: String,
    val isFolder: Boolean,
    val provider: CloudProviderType,
    val parentId: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long = 0L,
    val modifiedAtMillis: Long = 0L,
    val contentHash: String? = null,
    val downloadUrl: String? = null,
)

data class CloudAccountStatus(
    val provider: CloudProviderType,
    val isConnected: Boolean,
    val accountLabel: String? = null,
    val isDemo: Boolean = false,
)

data class DownloadedPdfDocument(
    val provider: CloudProviderType,
    val cloudFileId: String,
    val title: String,
    val documentHash: String,
    val localFilePath: String,
    val totalBytes: Long,
)
