package com.example.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.database.entity.AnnotationEntity
import com.example.core.database.entity.OpenTabEntity
import com.example.core.database.entity.PinnedItemEntity
import com.example.core.database.entity.TocNodeEntity
import com.example.core.database.model.DocumentIdentity
import com.example.core.database.repository.RoomLibraryDatabaseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QalamDatabaseTest {

    private lateinit var database: QalamDatabase
    private lateinit var repository: RoomLibraryDatabaseRepository

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QalamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomLibraryDatabaseRepository(database, testDispatchers)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `split tab state persists and restores both panes`() = runTest {
        val tab = OpenTabEntity(
            tabId = "tab-1",
            sortOrder = 0,
            isSplit = true,
            activePaneIndex = 1,
            splitRatio = 0.45f,
            primaryTitle = "Arabic Grammar.pdf",
            primaryProvider = "GOOGLE_DRIVE",
            primaryCloudFileId = "gd-1",
            primaryDocumentHash = "hash-1",
            primaryLocalCachePath = "/cache/gd-1.pdf",
            primaryPageIndex = 12,
            primaryScrollOffset = 240f,
            primaryZoomScale = 1.5f,
            primaryLayoutMode = "TWO_PAGE_SPREAD",
            primaryReadingFilter = "SEPIA",
            secondaryTitle = "Syntax Notes.pdf",
            secondaryProvider = "LOCAL",
            secondaryCloudFileId = "loc-2",
            secondaryDocumentHash = "hash-2",
            secondaryLocalCachePath = "/cache/loc-2.pdf",
            secondaryPageIndex = 3,
        )

        val saveResult = repository.saveOpenTab(tab)
        assertTrue(saveResult is AppResult.Success)

        val tabs = repository.observeOpenTabs().first()
        assertEquals(1, tabs.size)
        assertTrue(tabs.first().isSplit)
        assertEquals(12, tabs.first().primaryPageIndex)
        assertEquals("Syntax Notes.pdf", tabs.first().secondaryTitle)
        assertEquals("TWO_PAGE_SPREAD", tabs.first().primaryLayoutMode)
    }

    @Test
    fun `togglePin adds and removes pinned favorite`() = runTest {
        val item = PinnedItemEntity(
            itemKey = PinnedItemEntity.buildKey("DROPBOX", "/Books/Kitab.pdf"),
            provider = "DROPBOX",
            cloudFileId = "/Books/Kitab.pdf",
            title = "Kitab.pdf",
            isFolder = false,
        )

        val added = repository.togglePin(item)
        assertEquals(AppResult.Success(true), added)
        assertTrue(repository.observePinnedKeys().first().contains(item.itemKey))

        val removed = repository.togglePin(item)
        assertEquals(AppResult.Success(false), removed)
        assertFalse(repository.observePinnedKeys().first().contains(item.itemKey))
    }

    @Test
    fun `annotations filter by document identity and update notes`() = runTest {
        val doc = DocumentIdentity("hash-a", "ONEDRIVE", "od-99")
        val annotation = AnnotationEntity(
            id = "ann-1",
            documentHash = doc.documentHash,
            cloudProvider = doc.cloudProvider,
            cloudFileId = doc.cloudFileId,
            pageIndex = 4,
            scrollOffset = 128.5f,
            selectedText = "Knowledge organization",
            highlightColorKey = "MINT",
        )

        repository.saveAnnotation(annotation)
        repository.updateAnnotationNote("ann-1", "Review chapter summary")

        val list = repository.observeAnnotations(doc).first()
        assertEquals(1, list.size)
        assertEquals("Review chapter summary", list.first().noteText)
        assertEquals("MINT", list.first().highlightColorKey)
    }

    @Test
    fun `toc tree groups chapters and cascades subheading delete`() = runTest {
        val doc = DocumentIdentity("hash-toc", "BOX", "bx-10")
        val chapter = TocNodeEntity(
            id = "ch-1",
            documentHash = doc.documentHash,
            cloudProvider = doc.cloudProvider,
            cloudFileId = doc.cloudFileId,
            parentId = null,
            title = "الباب الأول: المنهجية",
            pageIndex = 1,
            sortOrder = 0,
        )
        val sub = TocNodeEntity(
            id = "sub-1",
            documentHash = doc.documentHash,
            cloudProvider = doc.cloudProvider,
            cloudFileId = doc.cloudFileId,
            parentId = "ch-1",
            title = "مدخل إلى الفهرسة",
            pageIndex = 2,
            sortOrder = 0,
        )

        repository.saveTocNode(chapter)
        repository.saveTocNode(sub)

        val tree = repository.observeTocTree(doc).first()
        assertEquals(1, tree.size)
        assertEquals(1, tree.first().subHeadings.size)
        assertEquals("مدخل إلى الفهرسة", tree.first().subHeadings.first().title)

        repository.deleteTocNode("ch-1")
        val afterDelete = repository.observeTocTree(doc).first()
        assertTrue(afterDelete.isEmpty())
    }
}
