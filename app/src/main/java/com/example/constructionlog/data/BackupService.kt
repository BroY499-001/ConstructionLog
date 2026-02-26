package com.example.constructionlog.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupService(
    private val context: Context
) {
    fun exportBackup(targetUri: Uri): Result<Unit> = runCatching {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) {
            throw IllegalStateException("数据库不存在")
        }

        val zipBytes = buildRawZip(dbFile)
        val sha256 = sha256Hex(zipBytes)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val encrypted = encrypt(zipBytes, iv)

        val payload = JSONObject()
            .put("version", 1)
            .put("algorithm", "AES/GCM/NoPadding")
            .put("sha256", sha256)
            .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .toString()

        context.contentResolver.openOutputStream(targetUri)?.use { output ->
            output.write(payload.toByteArray())
            output.flush()
        } ?: throw IllegalStateException("无法写入导出文件")
    }

    fun importBackup(sourceUri: Uri): Result<Unit> = runCatching {
        val payloadText = context.contentResolver.openInputStream(sourceUri)?.use {
            it.bufferedReader().readText()
        } ?: throw IllegalStateException("无法读取备份文件")

        val payload = JSONObject(payloadText)
        val version = payload.optInt("version", -1)
        if (version != 1) throw IllegalStateException("备份版本不兼容")

        val iv = Base64.decode(payload.getString("iv"), Base64.NO_WRAP)
        val encrypted = Base64.decode(payload.getString("data"), Base64.NO_WRAP)
        val expectedSha = payload.getString("sha256")

        val zipBytes = decrypt(encrypted, iv)
        val actualSha = sha256Hex(zipBytes)
        if (!actualSha.equals(expectedSha, ignoreCase = true)) {
            throw IllegalStateException("备份完整性校验失败")
        }

        val tempDir = File(context.cacheDir, "import_backup_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        ByteArrayInputStream(zipBytes).use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val outFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fileOut ->
                            zip.copyTo(fileOut)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        val metadataFile = File(tempDir, "metadata.json")
        if (!metadataFile.exists()) throw IllegalStateException("备份缺少 metadata.json")

        val dbFromBackup = File(tempDir, "database.sqlite")
        if (!dbFromBackup.exists()) throw IllegalStateException("备份缺少 database.sqlite")
        val walFromBackup = File(tempDir, "database.sqlite-wal")
        val shmFromBackup = File(tempDir, "database.sqlite-shm")

        val dbFile = context.getDatabasePath(DB_NAME)
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()
        val walFile = File(dbFile.parentFile, "$DB_NAME-wal")
        val shmFile = File(dbFile.parentFile, "$DB_NAME-shm")
        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()
        dbFromBackup.copyTo(dbFile, overwrite = true)
        if (walFromBackup.exists()) walFromBackup.copyTo(walFile, overwrite = true)
        if (shmFromBackup.exists()) shmFromBackup.copyTo(shmFile, overwrite = true)

        val targetImageDir = context.getExternalFilesDir("Pictures")
        if (targetImageDir != null) {
            targetImageDir.mkdirs()
            targetImageDir.listFiles()?.forEach { it.delete() }
            val backupImageDir = File(tempDir, "images")
            if (backupImageDir.exists()) {
                backupImageDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.copyTo(File(targetImageDir, file.name), overwrite = true)
                    }
                }
            }
        }

        tempDir.deleteRecursively()
    }

    private fun buildRawZip(dbFile: File): ByteArray {
        val zipOutput = ByteArrayOutputStream()
        ZipOutputStream(zipOutput).use { zip ->
            addFileToZip(zip, dbFile, "database.sqlite")
            val walFile = File(dbFile.parentFile, "${dbFile.name}-wal")
            val shmFile = File(dbFile.parentFile, "${dbFile.name}-shm")
            if (walFile.exists()) addFileToZip(zip, walFile, "database.sqlite-wal")
            if (shmFile.exists()) addFileToZip(zip, shmFile, "database.sqlite-shm")

            val imageDir = context.getExternalFilesDir("Pictures")
            if (imageDir != null && imageDir.exists()) {
                imageDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        addFileToZip(zip, file, "images/${file.name}")
                    }
                }
            }

            val metadata = JSONObject()
                .put("version", 1)
                .put("exportTime", System.currentTimeMillis())
                .put("app", "construction-log")
                .toString()
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(metadata.toByteArray())
            zip.closeEntry()
        }
        return zipOutput.toByteArray()
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun encrypt(data: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }

    private fun decrypt(data: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }

    private fun secretKey(): SecretKeySpec {
        val base = "${context.packageName}:construction-log-backup"
        val hash = MessageDigest.getInstance("SHA-256").digest(base.toByteArray())
        return SecretKeySpec(hash.copyOfRange(0, 16), "AES")
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val DB_NAME = "construction_logs_secure.db"
    }
}
