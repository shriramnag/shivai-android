package com.personal.ai.shivai.core.files

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class FileEntryInfo(
    val name: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val extension: String
)

data class FileOperationResult(
    val isSuccess: Boolean,
    val message: String,
    val path: String = "",
    val data: String? = null
)

class FileAgent(private val context: Context) {

    private val allowedBaseDirectories: List<File> by lazy {
        listOfNotNull(
            context.filesDir,
            context.cacheDir,
            context.getExternalFilesDir(null),
            context.externalCacheDir
        )
    }

    private fun isPathAllowed(target: File): Boolean {
        val canonicalTarget = target.canonicalPath
        return allowedBaseDirectories.any { base ->
            canonicalTarget.startsWith(base.canonicalPath)
        }
    }

    suspend fun listFiles(subPath: String = ""): List<FileEntryInfo> = withContext(Dispatchers.IO) {
        val base = context.filesDir
        val target = if (subPath.isBlank()) base else File(base, subPath)
        if (!isPathAllowed(target) || !target.exists() || !target.isDirectory) {
            return@withContext emptyList()
        }

        target.listFiles()?.map { file ->
            FileEntryInfo(
                name = file.name,
                absolutePath = file.absolutePath,
                sizeBytes = if (file.isDirectory) 0L else file.length(),
                isDirectory = file.isDirectory,
                lastModified = file.lastModified(),
                extension = file.extension
            )
        } ?: emptyList()
    }

    suspend fun readFile(relativePath: String, maxBytes: Int = 100_000): FileOperationResult = withContext(Dispatchers.IO) {
        val target = File(context.filesDir, relativePath)
        if (!isPathAllowed(target)) {
            return@withContext FileOperationResult(false, "Access denied: Path is outside allowed application sandbox.")
        }
        if (!target.exists() || !target.isFile) {
            return@withContext FileOperationResult(false, "File does not exist or is not a regular file: $relativePath")
        }

        try {
            val content = target.inputStream().use { stream ->
                val buffer = ByteArray(maxBytes)
                val readBytes = stream.read(buffer)
                if (readBytes > 0) String(buffer, 0, readBytes) else ""
            }
            FileOperationResult(true, "File read successfully.", path = target.absolutePath, data = content)
        } catch (e: Exception) {
            FileOperationResult(false, "Failed to read file: ${e.message}", path = target.absolutePath)
        }
    }

    suspend fun writeFile(relativePath: String, content: String): FileOperationResult = withContext(Dispatchers.IO) {
        val target = File(context.filesDir, relativePath)
        if (!isPathAllowed(target)) {
            return@withContext FileOperationResult(false, "Access denied: Path is outside allowed application sandbox.")
        }

        try {
            target.parentFile?.mkdirs()
            target.writeText(content)
            FileOperationResult(true, "File written successfully.", path = target.absolutePath)
        } catch (e: Exception) {
            FileOperationResult(false, "Failed to write file: ${e.message}", path = target.absolutePath)
        }
    }

    suspend fun deleteFile(relativePath: String): FileOperationResult = withContext(Dispatchers.IO) {
        val target = File(context.filesDir, relativePath)
        if (!isPathAllowed(target)) {
            return@withContext FileOperationResult(false, "Access denied: Path is outside allowed application sandbox.")
        }
        if (!target.exists()) {
            return@withContext FileOperationResult(false, "File does not exist: $relativePath")
        }

        val deleted = target.delete()
        if (deleted) {
            FileOperationResult(true, "File deleted successfully.", path = target.absolutePath)
        } else {
            FileOperationResult(false, "File deletion failed on filesystem.", path = target.absolutePath)
        }
    }

    suspend fun calculateSha256(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.isFile) return@withContext ""
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
