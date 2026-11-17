package com.example

import android.app.Application
import com.example.core.common.DefaultDispatcherProvider
import com.example.core.common.DispatcherProvider

class QalamApplication : Application() {
    val dispatcherProvider: DispatcherProvider by lazy { DefaultDispatcherProvider }
}
