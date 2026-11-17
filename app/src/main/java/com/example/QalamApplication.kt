package com.example

import android.app.Application
import android.content.Context
import com.example.core.cloud.CloudProvider
import com.example.core.cloud.CloudProviderType
import com.example.core.cloud.CloudRepository
import com.example.core.cloud.OAuthPkceManager
import com.example.core.cloud.SamplePdfGenerator
import com.example.core.cloud.TokenStorageManager
import com.example.core.cloud.provider.BoxProvider
import com.example.core.cloud.provider.DropboxProvider
import com.example.core.cloud.provider.GoogleDriveProvider
import com.example.core.cloud.provider.OneDriveProvider
import com.example.core.common.DefaultDispatcherProvider
import com.example.core.common.DispatcherProvider
import com.example.core.database.QalamDatabase
import com.example.core.database.repository.LibraryDatabaseRepository
import com.example.core.database.repository.RoomLibraryDatabaseRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val database: QalamDatabase by lazy {
        QalamDatabase.create(appContext)
    }

    val libraryDatabaseRepository: LibraryDatabaseRepository by lazy {
        RoomLibraryDatabaseRepository(
            database = database,
            dispatchers = dispatcherProvider,
        )
    }

    val tokenStorageManager: TokenStorageManager by lazy {
        TokenStorageManager(appContext)
    }

    val oauthPkceManager: OAuthPkceManager by lazy {
        OAuthPkceManager(
            tokenStorage = tokenStorageManager,
            okHttpClient = okHttpClient,
            moshi = moshi,
            dispatchers = dispatcherProvider,
        )
    }

    val samplePdfGenerator: SamplePdfGenerator by lazy {
        SamplePdfGenerator(
            context = appContext,
            dispatchers = dispatcherProvider,
        )
    }

    private val realCloudProviders: Map<CloudProviderType, CloudProvider> by lazy {
        mapOf(
            CloudProviderType.GOOGLE_DRIVE to GoogleDriveProvider(
                oauthPkceManager = oauthPkceManager,
                tokenStorage = tokenStorageManager,
                okHttpClient = okHttpClient,
                moshi = moshi,
                dispatchers = dispatcherProvider,
            ),
            CloudProviderType.ONEDRIVE to OneDriveProvider(
                oauthPkceManager = oauthPkceManager,
                tokenStorage = tokenStorageManager,
                okHttpClient = okHttpClient,
                moshi = moshi,
                dispatchers = dispatcherProvider,
            ),
            CloudProviderType.DROPBOX to DropboxProvider(
                oauthPkceManager = oauthPkceManager,
                tokenStorage = tokenStorageManager,
                okHttpClient = okHttpClient,
                moshi = moshi,
                dispatchers = dispatcherProvider,
            ),
            CloudProviderType.BOX to BoxProvider(
                oauthPkceManager = oauthPkceManager,
                tokenStorage = tokenStorageManager,
                okHttpClient = okHttpClient,
                moshi = moshi,
                dispatchers = dispatcherProvider,
            ),
        )
    }

    val cloudRepository: CloudRepository by lazy {
        CloudRepository(
            context = appContext,
            tokenStorage = tokenStorageManager,
            oauthPkceManager = oauthPkceManager,
            realProviders = realCloudProviders,
            samplePdfGenerator = samplePdfGenerator,
            dispatchers = dispatcherProvider,
        )
    }
}

class QalamApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
