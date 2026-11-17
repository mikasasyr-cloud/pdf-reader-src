package com.example.core.database.repository

import com.example.core.common.AppError
import com.example.core.common.AppResult
import com.example.core.common.DispatcherProvider
import com.example.core.common.safeSuspendCall
import com.example.core.database.QalamDatabase
import com.example.core.database.entity.AnnotationEntity
import com.example.core.database.entity.OpenTabEntity
import com.example.core.database.entity.PinnedItemEntity
import com.example.core.database.entity.TocNodeEntity
import com.example.core.database.model.DocumentIdentity
import com.example.core.database.model.TocTreeChapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface LibraryDatabaseRepository {
    fun observeOpenTabs(): Flow<List<OpenTabEntity>>
    suspend fun getOpenTabsOnce(): AppResult<List<OpenTabEntity>>
    suspend fun saveOpenTab(tab: OpenTabEntity): AppResult<Unit>
    suspend fun saveOpenTabs(tabs: List<OpenTabEntity>): AppResult<Unit>
    suspend fun deleteOpenTab(tabId: String): AppResult<Unit>

    fun observePinnedItems(): Flow<List<PinnedItemEntity>>
    fun observePinnedKeys(): Flow<Set<String>>
    suspend fun togglePin(item: PinnedItemEntity): AppResult<Boolean>
    suspend fun unpinByKey(itemKey: String): AppResult<Unit>

    fun observeAnnotations(identity: DocumentIdentity): Flow<List<AnnotationEntity>>
    suspend fun saveAnnotation(annotation: AnnotationEntity): AppResult<Unit>
    suspend fun updateAnnotationNote(id: String, noteText: String): AppResult<Unit>
    suspend fun deleteAnnotation(id: String): AppResult<Unit>

    fun observeTocTree(identity: DocumentIdentity): Flow<List<TocTreeChapter>>
    suspend fun saveTocNode(node: TocNodeEntity): AppResult<Unit>
    suspend fun renameTocNode(id: String, title: String): AppResult<Unit>
    suspend fun reorderTocNodes(orderedIds: List<String>): AppResult<Unit>
    suspend fun deleteTocNode(id: String): AppResult<Unit>
}

class RoomLibraryDatabaseRepository(
    private val database: QalamDatabase,
    private val dispatchers: DispatcherProvider,
) : LibraryDatabaseRepository {

    private val openTabDao = database.openTabDao()
    private val pinnedItemDao = database.pinnedItemDao()
    private val annotationDao = database.annotationDao()
    private val tocNodeDao = database.tocNodeDao()

    override fun observeOpenTabs(): Flow<List<OpenTabEntity>> =
        openTabDao.observeAllTabs()

    override suspend fun getOpenTabsOnce(): AppResult<List<OpenTabEntity>> =
        withContext(dispatchers.io) {
            safeStorageCall { openTabDao.getAllTabsOnce() }
        }

    override suspend fun saveOpenTab(tab: OpenTabEntity): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall { openTabDao.upsertTab(tab) }
        }

    override suspend fun saveOpenTabs(tabs: List<OpenTabEntity>): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall { openTabDao.upsertTabs(tabs) }
        }

    override suspend fun deleteOpenTab(tabId: String): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall { openTabDao.deleteTabById(tabId) }
        }

    override fun observePinnedItems(): Flow<List<PinnedItemEntity>> =
        pinnedItemDao.observeAllPinned()

    override fun observePinnedKeys(): Flow<Set<String>> =
        pinnedItemDao.observePinnedKeys().map { it.toSet() }

    override suspend fun togglePin(item: PinnedItemEntity): AppResult<Boolean> =
        withContext(dispatchers.io) {
            safeStorageCall {
                val currentlyPinned = pinnedItemDao.isPinned(item.itemKey)
                if (currentlyPinned) {
                    pinnedItemDao.deleteByKey(item.itemKey)
                    false
                } else {
                    pinnedItemDao.upsert(item)
                    true
                }
            }
        }

    override suspend fun unpinByKey(itemKey: String): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall { pinnedItemDao.deleteByKey(itemKey) }
        }

    override fun observeAnnotations(identity: DocumentIdentity): Flow<List<AnnotationEntity>> =
        annotationDao.observeForDocument(
            documentHash = identity.documentHash,
            cloudProvider = identity.cloudProvider,
            cloudFileId = identity.cloudFileId,
        )

    override suspend fun saveAnnotation(annotation: AnnotationEntity): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall { annotationDao.upsert(annotation) }
        }

    override suspend fun updateAnnotationNote(id: String, noteText: String): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall {
                annotationDao.updateNote(
                    id = id,
                    noteText = noteText,
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }

    override suspend fun deleteAnnotation(id: String): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall { annotationDao.deleteById(id) }
        }

    override fun observeTocTree(identity: DocumentIdentity): Flow<List<TocTreeChapter>> =
        tocNodeDao.observeForDocument(
            documentHash = identity.documentHash,
            cloudProvider = identity.cloudProvider,
            cloudFileId = identity.cloudFileId,
        ).map { nodes -> buildTocTree(nodes) }

    override suspend fun saveTocNode(node: TocNodeEntity): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall { tocNodeDao.upsert(node) }
        }

    override suspend fun renameTocNode(id: String, title: String): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall { tocNodeDao.renameNode(id = id, title = title) }
        }

    override suspend fun reorderTocNodes(orderedIds: List<String>): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall {
                orderedIds.forEachIndexed { index, id ->
                    tocNodeDao.updateSortOrder(id = id, sortOrder = index)
                }
            }
        }

    override suspend fun deleteTocNode(id: String): AppResult<Unit> =
        withContext(dispatchers.io) {
            safeStorageCall { tocNodeDao.deleteById(id) }
        }

    private fun buildTocTree(nodes: List<TocNodeEntity>): List<TocTreeChapter> {
        val chapters = nodes
            .filter { it.parentId == null }
            .sortedWith(compareBy<TocNodeEntity> { it.sortOrder }.thenBy { it.pageIndex })
        val childrenByParent = nodes
            .filter { it.parentId != null }
            .groupBy { it.parentId }

        return chapters.map { chapter ->
            val subHeadings = childrenByParent[chapter.id]
                .orEmpty()
                .sortedWith(compareBy<TocNodeEntity> { it.sortOrder }.thenBy { it.pageIndex })
            TocTreeChapter(
                chapter = chapter,
                subHeadings = subHeadings,
            )
        }
    }

    private suspend inline fun <T> safeStorageCall(crossinline block: suspend () -> T): AppResult<T> =
        safeSuspendCall(
            errorMapper = { throwable ->
                AppError.Storage(
                    message = throwable.localizedMessage ?: "Database operation failed",
                    cause = throwable,
                )
            },
            block = block,
        )
}
