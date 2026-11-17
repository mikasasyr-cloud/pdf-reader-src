package com.example.core.cloud

import com.example.core.common.AppError
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

internal object CloudDownloadHelper {

    fun sanitizeFileName(raw: String, fallbackId: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9._\\-\\u0600-\\u06FF]"), "_").take(80)
        val base = cleaned.ifBlank { fallbackId }
        return if (base.endsWith(".pdf", ignoreCase = true)) base else "$base.pdf"
    }

    fun executeDownloadToFile(
        okHttpClient: OkHttpClient,
        request: Request,
        destinationFile: File,
    ): Pair<Long, String> {
        destinationFile.parentFile?.mkdirs()
        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) {
                throw CloudAuthException("HTTP ${response.code}: Unauthorized")
            }
            if (response.code == 404) {
                throw CloudNotFoundException("HTTP 404: File not found")
            }
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} downloading file")
            }
            val body = response.body ?: throw IOException("Empty download response body")
            return writeStreamAndHash(body.byteStream(), destinationFile)
        }
    }

    fun writeStreamAndHash(inputStream: InputStream, destinationFile: File): Pair<Long, String> {
        destinationFile.parentFile?.mkdirs()
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L
        val buffer = ByteArray(8192)
        inputStream.use { input ->
            destinationFile.outputStream().use { output ->
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    totalBytes += read
                }
            }
        }
        val hashHex = digest.digest().joinToString("") { "%02x".format(it) }
        return totalBytes to hashHex
    }

    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun mapHttpOrIoError(throwable: Throwable): AppError = when (throwable) {
        is CloudAuthException -> AppError.Unauthorized(throwable.message ?: "Unauthorized", throwable)
        is CloudNotFoundException -> AppError.NotFound(throwable.message ?: "Not found", throwable)
        is IOException -> AppError.Network(throwable.localizedMessage ?: "Network error", throwable)
        else -> AppError.Unknown(throwable.localizedMessage ?: "Cloud error", throwable)
    }
}

internal class CloudAuthException(message: String) : Exception(message)
internal class CloudNotFoundException(message: String) : Exception(message)
