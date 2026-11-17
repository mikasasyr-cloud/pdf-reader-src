package com.example.core.common

import android.util.Log

object AppLogger {
    private const val PREFIX = "Qalam"

    fun d(tag: String, message: String) {
        runCatching { Log.d("$PREFIX:$tag", message) }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) {
                Log.w("$PREFIX:$tag", message, throwable)
            } else {
                Log.w("$PREFIX:$tag", message)
            }
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) {
                Log.e("$PREFIX:$tag", message, throwable)
            } else {
                Log.e("$PREFIX:$tag", message)
            }
        }
    }
}
