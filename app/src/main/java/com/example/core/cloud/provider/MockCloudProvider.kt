package com.example.core.cloud.provider

import com.example.core.cloud.CloudProvider
import com.example.core.cloud.CloudProviderType
import com.example.core.cloud.SamplePdfGenerator
import com.example.core.cloud.model.CloudNode
import com.example.core.cloud.model.DownloadedPdfDocument
import com.example.core.common.AppResult
import java.io.File

class MockCloudProvider(
    override val providerType: CloudProviderType,
    private val samplePdfGenerator: SamplePdfGenerator,
) : CloudProvider {

    private val nodesByParent: Map<String, List<CloudNode>> = buildTreeFor(providerType)
    private val titlesById: Map<String, String> =
        nodesByParent.values.flatten().associate { it.id to it.name }

    override suspend fun isConnected(): Boolean = true

    override suspend fun listFolder(folderId: String?): AppResult<List<CloudNode>> {
        val key = folderId ?: ROOT_ID
        return AppResult.Success(nodesByParent[key].orEmpty())
    }

    override suspend fun downloadPdf(
        fileId: String,
        fileName: String,
        cacheDir: File,
    ): AppResult<DownloadedPdfDocument> {
        val resolvedTitle = titlesById[fileId] ?: fileName
        val providerCache = File(cacheDir, "demo_${providerType.name.lowercase()}")
        return samplePdfGenerator.generateMockPdf(
            provider = providerType,
            fileId = fileId,
            title = resolvedTitle,
            targetDir = providerCache,
        )
    }

    override suspend fun disconnect(): AppResult<Unit> = AppResult.Success(Unit)

    private fun buildTreeFor(provider: CloudProviderType): Map<String, List<CloudNode>> {
        val p = provider.name.lowercase()
        return when (provider) {
            CloudProviderType.GOOGLE_DRIVE -> mapOf(
                ROOT_ID to listOf(
                    folder("${p}_f_books", "مكتبة الأبحاث · Research Books", ROOT_ID),
                    folder("${p}_f_notes", "محاضرات وملخصات · Lecture Notes", ROOT_ID),
                    pdf("${p}_doc_intro", "Qalam Research Primer.pdf", ROOT_ID, 245_120L),
                ),
                "${p}_f_books" to listOf(
                    folder("${p}_f_arabic", "الأدب والبلاغة", "${p}_f_books"),
                    pdf("${p}_doc_fiqh", "أصول منهجية البحث العلمي.pdf", "${p}_f_books", 1_420_000L),
                    pdf("${p}_doc_arch", "Clean Android Architecture.pdf", "${p}_f_books", 980_500L),
                ),
                "${p}_f_arabic" to listOf(
                    pdf("${p}_doc_bayan", "مدخل إلى علم البيان.pdf", "${p}_f_arabic", 612_400L),
                ),
                "${p}_f_notes" to listOf(
                    pdf("${p}_doc_summary", "Split-Tab Comparison Notes.pdf", "${p}_f_notes", 310_000L),
                ),
            )
            CloudProviderType.ONEDRIVE -> mapOf(
                ROOT_ID to listOf(
                    folder("${p}_f_work", "المشاريع الهندسية · Engineering", ROOT_ID),
                    pdf("${p}_doc_spec", "System Design Specification.pdf", ROOT_ID, 512_000L),
                ),
                "${p}_f_work" to listOf(
                    pdf("${p}_doc_compose", "Jetpack Compose Patterns.pdf", "${p}_f_work", 845_000L),
                    pdf("${p}_doc_rtl", "توطين الواجهات العربية RTL.pdf", "${p}_f_work", 430_000L),
                ),
            )
            CloudProviderType.DROPBOX -> mapOf(
                ROOT_ID to listOf(
                    folder("${p}_f_reading", "قائمة القراءة · Reading List", ROOT_ID),
                    pdf("${p}_doc_manual", "Knowledge Graph Handbook.pdf", ROOT_ID, 720_000L),
                ),
                "${p}_f_reading" to listOf(
                    pdf("${p}_doc_history", "تاريخ تدوين المعرفة.pdf", "${p}_f_reading", 1_150_000L),
                ),
            )
            CloudProviderType.BOX -> mapOf(
                ROOT_ID to listOf(
                    folder("${p}_f_archive", "الأرشيف الموثق · Archive", ROOT_ID),
                    pdf("${p}_doc_review", "Peer Review Dossier.pdf", ROOT_ID, 495_000L),
                ),
                "${p}_f_archive" to listOf(
                    pdf("${p}_doc_index", "فهرسة المخطوطات والكتب.pdf", "${p}_f_archive", 880_000L),
                ),
            )
            else -> emptyMap()
        }
    }

    private fun folder(id: String, name: String, parentId: String): CloudNode = CloudNode(
        id = id,
        name = name,
        isFolder = true,
        provider = providerType,
        parentId = if (parentId == ROOT_ID) null else parentId,
        mimeType = "folder",
    )

    private fun pdf(id: String, name: String, parentId: String, sizeBytes: Long): CloudNode = CloudNode(
        id = id,
        name = name,
        isFolder = false,
        provider = providerType,
        parentId = if (parentId == ROOT_ID) null else parentId,
        mimeType = "application/pdf",
        sizeBytes = sizeBytes,
    )

    private companion object {
        const val ROOT_ID = "__root__"
    }
}
