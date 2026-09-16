package com.personal.ai.shivai.core.security

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class QuarantineManager(private val context: Context) {

    private val quarantineDir = File(context.filesDir, "security_quarantine").apply {
        if (!exists()) mkdirs()
    }
    private val manifestFile = File(quarantineDir, "quarantine_manifest.json")

    private val _quarantinedFiles = MutableStateFlow<List<QuarantinedFile>>(emptyList())
    val quarantinedFiles: StateFlow<List<QuarantinedFile>> = _quarantinedFiles.asStateFlow()

    init {
        loadManifest()
    }

    suspend fun quarantine(
        sourceFile: File,
        reason: String,
        risk: SecurityRiskLevel,
        sha256: String
    ): Result<QuarantinedFile> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Source file does not exist"))
            }

            val id = "Q-${UUID.randomUUID().toString().take(8)}"
            val destinationFile = File(quarantineDir, "$id.quarantined")

            val success = sourceFile.renameTo(destinationFile)
            if (!success) {
                sourceFile.copyTo(destinationFile, overwrite = true)
                sourceFile.delete()
            }

            val item = QuarantinedFile(
                id = id,
                originalPath = sourceFile.absolutePath,
                quarantinePath = destinationFile.absolutePath,
                fileName = sourceFile.name,
                sha256 = sha256,
                reason = reason,
                riskLevel = risk
            )

            val current = _quarantinedFiles.value.toMutableList()
            current.add(item)
            _quarantinedFiles.value = current
            saveManifest()

            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restore(id: String): Result<String> = withContext(Dispatchers.IO) {
        val item = _quarantinedFiles.value.firstOrNull { it.id == id }
            ?: return@withContext Result.failure(NoSuchElementException("Quarantined file not found: $id"))

        val qFile = File(item.quarantinePath)
        val origFile = File(item.originalPath)

        if (!qFile.exists()) {
            return@withContext Result.failure(IllegalStateException("Quarantine file missing on disk"))
        }

        origFile.parentFile?.mkdirs()
        val ok = qFile.renameTo(origFile)
        if (!ok) {
            qFile.copyTo(origFile, overwrite = true)
            qFile.delete()
        }

        val updated = _quarantinedFiles.value.filterNot { it.id == id }
        _quarantinedFiles.value = updated
        saveManifest()

        Result.success("Restored ${item.fileName} to ${item.originalPath}")
    }

    suspend fun deletePermanently(id: String): Result<String> = withContext(Dispatchers.IO) {
        val item = _quarantinedFiles.value.firstOrNull { it.id == id }
            ?: return@withContext Result.failure(NoSuchElementException("Quarantined file not found: $id"))

        val qFile = File(item.quarantinePath)
        if (qFile.exists()) {
            qFile.delete()
        }

        val updated = _quarantinedFiles.value.filterNot { it.id == id }
        _quarantinedFiles.value = updated
        saveManifest()

        Result.success("Permanently deleted ${item.fileName}")
    }

    private fun loadManifest() {
        if (!manifestFile.exists()) return
        try {
            val json = manifestFile.readText()
            val array = JSONArray(json)
            val list = mutableListOf<QuarantinedFile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    QuarantinedFile(
                        id = obj.getString("id"),
                        originalPath = obj.getString("originalPath"),
                        quarantinePath = obj.getString("quarantinePath"),
                        fileName = obj.getString("fileName"),
                        sha256 = obj.getString("sha256"),
                        reason = obj.getString("reason"),
                        riskLevel = SecurityRiskLevel.valueOf(obj.getString("riskLevel")),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            _quarantinedFiles.value = list
        } catch (_: Exception) {}
    }

    private fun saveManifest() {
        try {
            val array = JSONArray()
            for (item in _quarantinedFiles.value) {
                array.put(JSONObject().apply {
                    put("id", item.id)
                    put("originalPath", item.originalPath)
                    put("quarantinePath", item.quarantinePath)
                    put("fileName", item.fileName)
                    put("sha256", item.sha256)
                    put("reason", item.reason)
                    put("riskLevel", item.riskLevel.name)
                    put("timestamp", item.timestamp)
                })
            }
            manifestFile.writeText(array.toString(2))
        } catch (_: Exception) {}
    }
}
