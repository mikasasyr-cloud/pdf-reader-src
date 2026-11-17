package com.example.core.cloud

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.core.cloud.model.DownloadedPdfDocument
import com.example.core.common.AppError
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.safeSuspendCall
import kotlinx.coroutines.withContext
import java.io.File

class SamplePdfGenerator(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) {

    suspend fun ensureSampleGuidePdf(): AppResult<DownloadedPdfDocument> =
        generateOrCopyPdf(
            fileName = "sample_guide.pdf",
            cloudFileId = "sample_guide",
            title = "Qalam Bilingual Guide · دليل قلم",
            provider = CloudProviderType.SAMPLE,
        )

    suspend fun generateMockPdf(
        provider: CloudProviderType,
        fileId: String,
        title: String,
        targetDir: File,
    ): AppResult<DownloadedPdfDocument> = withContext(dispatchers.io) {
        safeSuspendCall(
            errorMapper = { AppError.Storage(it.localizedMessage ?: "Failed to create sample PDF", it) },
        ) {
            targetDir.mkdirs()
            val safeName = CloudDownloadHelper.sanitizeFileName(title, fileId)
            val file = File(targetDir, "${fileId}_$safeName")
            if (!file.exists() || file.length() == 0L) {
                writeBilingualPdf(
                    destination = file,
                    documentTitle = title,
                    providerBadge = provider.name,
                )
            }
            val hash = CloudDownloadHelper.computeSha256(file)
            DownloadedPdfDocument(
                provider = provider,
                cloudFileId = fileId,
                title = title,
                documentHash = hash,
                localFilePath = file.absolutePath,
                totalBytes = file.length(),
            )
        }
    }

    private suspend fun generateOrCopyPdf(
        fileName: String,
        cloudFileId: String,
        title: String,
        provider: CloudProviderType,
    ): AppResult<DownloadedPdfDocument> = withContext(dispatchers.io) {
        safeSuspendCall(
            errorMapper = { AppError.Storage(it.localizedMessage ?: "Failed to prepare sample PDF", it) },
        ) {
            val sampleDir = File(context.filesDir, "sample")
            sampleDir.mkdirs()
            val targetFile = File(sampleDir, fileName)

            if (!targetFile.exists() || targetFile.length() == 0L) {
                val copiedFromAssets = runCatching {
                    context.assets.open(fileName).use { input ->
                        CloudDownloadHelper.writeStreamAndHash(input, targetFile)
                    }
                    true
                }.getOrDefault(false)

                if (!copiedFromAssets) {
                    writeBilingualPdf(
                        destination = targetFile,
                        documentTitle = title,
                        providerBadge = "QALAM GUIDE",
                    )
                }
            }

            val hash = CloudDownloadHelper.computeSha256(targetFile)
            DownloadedPdfDocument(
                provider = provider,
                cloudFileId = cloudFileId,
                title = title,
                documentHash = hash,
                localFilePath = targetFile.absolutePath,
                totalBytes = targetFile.length(),
            )
        }
    }

    private fun writeBilingualPdf(
        destination: File,
        documentTitle: String,
        providerBadge: String,
    ) {
        val pages = buildSamplePages(documentTitle, providerBadge)
        destination.parentFile?.mkdirs()

        runCatching {
            val pdfDocument = PdfDocument()
            val cairoTypeface = runCatching {
                ResourcesCompat.getFont(context, R.font.cairo)
            }.getOrNull() ?: Typeface.DEFAULT

            val pageWidth = 595
            val pageHeight = 842

            pages.forEachIndexed { index, spec ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Warm ivory editorial background
                canvas.drawColor(Color.rgb(252, 250, 244))

                val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(22, 78, 93)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 14f, accentPaint)

                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(154, 91, 19)
                    textSize = 12f
                    typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
                }
                canvas.drawText("$providerBadge  •  PAGE ${index + 1} / ${pages.size}", 44f, 52f, badgePaint)

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(22, 78, 93)
                    textSize = 22f
                    typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
                }
                canvas.drawText(spec.englishHeading, 44f, 94f, titlePaint)

                val arabicHeadingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(25, 28, 29)
                    textSize = 20f
                    textAlign = Paint.Align.RIGHT
                    typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
                }
                canvas.drawText(spec.arabicHeading, (pageWidth - 44).toFloat(), 132f, arabicHeadingPaint)

                val dividerPaint = Paint().apply {
                    color = Color.rgb(212, 206, 194)
                    strokeWidth = 1.5f
                }
                canvas.drawLine(44f, 152f, (pageWidth - 44).toFloat(), 152f, dividerPaint)

                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(42, 46, 48)
                    textSize = 14f
                    typeface = cairoTypeface
                }
                var y = 188f
                spec.englishLines.forEach { line ->
                    canvas.drawText(line, 44f, y, bodyPaint)
                    y += 24f
                }

                y += 22f
                val arabicBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(34, 38, 40)
                    textSize = 15f
                    textAlign = Paint.Align.RIGHT
                    typeface = cairoTypeface
                }
                spec.arabicLines.forEach { line ->
                    canvas.drawText(line, (pageWidth - 44).toFloat(), y, arabicBodyPaint)
                    y += 28f
                }

                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(114, 120, 123)
                    textSize = 11f
                    typeface = cairoTypeface
                }
                canvas.drawText("Qalam PDF Knowledge Workspace", 44f, (pageHeight - 30).toFloat(), footerPaint)

                pdfDocument.finishPage(page)
            }

            destination.outputStream().use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
        }

        if (!destination.exists() || destination.length() < 512L) {
            writeMinimalValidPdf(destination, pages)
        }
    }

    private fun writeMinimalValidPdf(destination: File, pages: List<SamplePageSpec>) {
        val sb = StringBuilder()
        val offsets = mutableListOf<Int>()

        fun appendObj(id: Int, body: String) {
            offsets.add(sb.toString().toByteArray(Charsets.US_ASCII).size)
            sb.append("$id 0 obj\n$body\nendobj\n")
        }

        sb.append("%PDF-1.4\n")
        val pageCount = pages.size
        // 1 = Catalog, 2 = Pages, 3 = Font, 4..(3+pageCount) = Page objects, (4+pageCount).. = Content objects
        val fontObjId = 3
        val firstPageObjId = 4
        val firstContentObjId = firstPageObjId + pageCount

        appendObj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        val kidsRefs = (0 until pageCount).joinToString(" ") { "${firstPageObjId + it} 0 R" }
        appendObj(2, "<< /Type /Pages /Kids [ $kidsRefs ] /Count $pageCount >>")
        appendObj(fontObjId, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")

        pages.forEachIndexed { index, _ ->
            val pageId = firstPageObjId + index
            val contentId = firstContentObjId + index
            appendObj(
                pageId,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                    "/Resources << /Font << /F1 $fontObjId 0 R >> >> /Contents $contentId 0 R >>",
            )
        }

        pages.forEachIndexed { index, spec ->
            val contentId = firstContentObjId + index
            val escapedHeading = spec.englishHeading
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
            val stream = buildString {
                append("BT\n/F1 16 Tf\n50 760 Td\n($escapedHeading) Tj\n")
                var dy = -28
                spec.englishLines.forEach { line ->
                    val safeLine = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                    append("0 $dy Td\n/F1 12 Tf\n($safeLine) Tj\n")
                    dy = -20
                }
                append("ET")
            }
            val streamBytes = stream.toByteArray(Charsets.US_ASCII).size
            appendObj(contentId, "<< /Length $streamBytes >>\nstream\n$stream\nendstream")
        }

        val xrefOffset = sb.toString().toByteArray(Charsets.US_ASCII).size
        val totalObjs = offsets.size + 1
        sb.append("xref\n0 $totalObjs\n0000000000 65535 f \n")
        offsets.forEach { offset ->
            sb.append(String.format(java.util.Locale.US, "%010d 00000 n \n", offset))
        }
        sb.append("trailer\n<< /Size $totalObjs /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

        destination.writeBytes(sb.toString().toByteArray(Charsets.US_ASCII))
    }

    private fun buildSamplePages(title: String, badge: String): List<SamplePageSpec> = listOf(
        SamplePageSpec(
            englishHeading = "1. $title",
            arabicHeading = "١. مرحباً بك في مساحة قلم للقراءة المعرفية",
            englishLines = listOf(
                "Source: $badge",
                "Qalam combines Chrome-inspired multi-tab switching with a calm,",
                "distraction-free PDF canvas designed for deep research.",
                "Tap anywhere on the page to hide chrome bars and focus.",
            ),
            arabicLines = listOf(
                "يجمع تطبيق قلم بين شريط التبويبات السريع وواجهة القراءة الهادئة.",
                "اضغط ضغطة واحدة على الصفحة لإخفاء الأشرطة والتركيز التام.",
                "يدعم التطبيق القراءة باللغتين العربية (RTL) والإنجليزية (LTR).",
            ),
        ),
        SamplePageSpec(
            englishHeading = "2. Split-Tab & Two-Page Spread",
            arabicHeading = "٢. التبويب المقسم وعرض الصفحتين المتقابلتين",
            englishLines = listOf(
                "Open two books side-by-side inside a single Split-Tab.",
                "Drag the center divider to adjust reading proportions.",
                "Switch between Continuous, Horizontal Swipe, and Two-Page Spread.",
                "In Arabic RTL, two-page spreads mirror naturally from right to left.",
            ),
            arabicLines = listOf(
                "افتح كتابين متجاورين داخل تبويب واحد للمقارنة والدراسة.",
                "اسحب الفاصل الأوسط لضبط مساحة كل ملف بمرونة كاملة.",
                "يحترم عرض الصفحتين اتجاه التجليد العربي من اليمين إلى اليسار.",
            ),
        ),
        SamplePageSpec(
            englishHeading = "3. Tree TOC & Knowledge Structuring",
            arabicHeading = "٣. الفهرس الشجري للأبواب والعناوين الفرعية",
            englishLines = listOf(
                "Organize large PDFs into Main Chapters and nested Sub-Headings.",
                "Every custom TOC node jumps directly to its anchored page.",
                "Rename, reorder, or prune branches safely at any time.",
            ),
            arabicLines = listOf(
                "نظّم الكتب الطويلة إلى أبواب رئيسية وعناوين فرعية متداخلة.",
                "ينقلك كل عنصر في الفهرس مباشرةً إلى الصفحة المرتبطة به.",
                "تُحفظ الفهارس محلياً لكل مستند عبر معرف فريد ومستقر.",
            ),
        ),
        SamplePageSpec(
            englishHeading = "4. Highlights & Anchored Notes",
            arabicHeading = "٤. التظليلات الملونة والملاحظات المربوطة",
            englishLines = listOf(
                "Highlight key passages in Solar Yellow, Mint Green, Sky Blue, or Rose.",
                "Attach structured study notes to any page and jump straight back.",
                "Reading filters: Original, Warm Sepia, and Night Inverted mode.",
            ),
            arabicLines = listOf(
                "ظلّل المقاطع المهمة بأربعة ألوان هادئة مع إمكانية إضافة ملاحظة.",
                "استعرض جميع ملاحظاتك من لوحة الملاحظات وانتقل لموضعها فوراً.",
                "جرّب فلتر السيبيا الدافئ أو الوضع الليلي المريح للعين.",
            ),
        ),
        SamplePageSpec(
            englishHeading = "5. Unified Cloud Trees & Pinned Favorites",
            arabicHeading = "٥. التخزين السحابي الموحّد والمفضلة المثبتة",
            englishLines = listOf(
                "Connect Google Drive, OneDrive, Dropbox, or Box via OAuth PKCE.",
                "Folders lazy-load on demand to keep large libraries responsive.",
                "Pin frequently used folders and books to the top of the sidebar.",
            ),
            arabicLines = listOf(
                "تصفح ملفاتك في Google Drive وOneDrive وDropbox وBox.",
                "يتم تحميل كل مجلد عند فتحه فقط لتسريع التصفح وتوفير البيانات.",
                "ثبّت أهم المجلدات والكتب للوصول إليها بنقرة واحدة.",
            ),
        ),
        SamplePageSpec(
            englishHeading = "6. Bilingual Reference & Keyboard Shortcuts",
            arabicHeading = "٦. المرجع الثنائي اللغة والاختصارات السريعة",
            englishLines = listOf(
                "Switch instantly between Arabic (RTL) and English (LTR) in Settings.",
                "The collapsible library sidebar docks automatically on the right",
                "for Arabic and on the left for English on phones and tablets.",
            ),
            arabicLines = listOf(
                "بدّل بين العربية والإنجليزية فورياً من الإعدادات دون فقدان موضعك.",
                "يظهر الشريط الجانبي يميناً في العربية ويساراً في الإنجليزية تلقائياً.",
                "نتمنى لك قراءة مثمرة ومنظمة مع تطبيق قلم.",
            ),
        ),
    )

    private data class SamplePageSpec(
        val englishHeading: String,
        val arabicHeading: String,
        val englishLines: List<String>,
        val arabicLines: List<String>,
    )
}
