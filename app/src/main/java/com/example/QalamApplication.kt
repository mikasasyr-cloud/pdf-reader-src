package com.example

import android.app.Application
import android.content.Context
import com.example.core.common.DefaultDispatcherProvider
import com.example.core.common.DispatcherProvider
import com.example.core.database.QalamDatabase
import com.example.core.database.repository.LibraryDatabaseRepository
import com.example.core.database.repository.RoomLibraryDatabaseRepository

class AppContainer(context: Context) {
    val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

    val database: QalamDatabase by lazy {
        QalamDatabase.create(context)
    }

    val libraryDatabaseRepository: LibraryDatabaseRepository by lazy {
        RoomLibraryDatabaseRepository(
            database = database,
            dispatchers = dispatcherProvider,
        )
    }
}

class QalamApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
