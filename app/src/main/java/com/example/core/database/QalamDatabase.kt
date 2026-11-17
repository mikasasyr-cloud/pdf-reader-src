package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.database.dao.AnnotationDao
import com.example.core.database.dao.OpenTabDao
import com.example.core.database.dao.PinnedItemDao
import com.example.core.database.dao.TocNodeDao
import com.example.core.database.entity.AnnotationEntity
import com.example.core.database.entity.OpenTabEntity
import com.example.core.database.entity.PinnedItemEntity
import com.example.core.database.entity.TocNodeEntity

@Database(
    entities = [
        OpenTabEntity::class,
        PinnedItemEntity::class,
        AnnotationEntity::class,
        TocNodeEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class QalamDatabase : RoomDatabase() {
    abstract fun openTabDao(): OpenTabDao
    abstract fun pinnedItemDao(): PinnedItemDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun tocNodeDao(): TocNodeDao

    companion object {
        private const val DB_NAME = "qalam_library.db"

        fun create(context: Context): QalamDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                QalamDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
