package com.constructionlog.app.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Base64
import com.constructionlog.app.ConstructionLogApp
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupService(
    private val context: Context
) {
    fun exportBackup(targetUri: Uri): Result<Unit> = runCatching {
        exportBackupInternal { output ->
            context.contentResolver.openOutputStream(targetUri)?.use(output)
                ?: throw IllegalStateException("无法写入导出文件")
        }
    }

    fun exportBackup(targetFile: File): Result<File> = runCatching {
        targetFile.parentFile?.mkdirs()
        val tempFile = File(targetFile.parentFile ?: context.cacheDir, "${targetFile.name}.tmp")
        exportBackupInternal { output ->
            FileOutputStream(tempFile).use(output)
        }
        if (targetFile.exists() && !targetFile.delete()) {
            throw IllegalStateException("无法覆盖旧备份文件")
        }
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
        targetFile
    }

    fun exportAutoBackup(): Result<File> = exportBackup(autoBackupFile())

    fun deleteAutoBackup(): Boolean {
        val file = autoBackupFile()
        return !file.exists() || file.delete()
    }

    fun autoBackupFile(): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        return File(baseDir, "backups/$AUTO_BACKUP_FILE_NAME")
    }

    fun autoBackupDisplayName(): String = "backups/$AUTO_BACKUP_FILE_NAME"

    fun manualBackupFileName(timeMillis: Long = System.currentTimeMillis()): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss", Locale.US)
        val timestamp = Instant.ofEpochMilli(timeMillis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
        return "construction-log-backup-$timestamp.zip"
    }

    fun importBackup(sourceUri: Uri): Result<Unit> = runCatching {
        val tempDir = File(context.cacheDir, "import_backup_${System.currentTimeMillis()}")
        val tempZipFile = File(tempDir, "payload.zip")
        tempDir.mkdirs()

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { rawInput ->
                val input = BufferedInputStream(rawInput)
                if (isBinaryBackup(input)) {
                    importBinaryBackup(input, tempZipFile)
                } else {
                    importLegacyBackup(input, tempZipFile)
                }
            } ?: throw IllegalStateException("无法读取备份文件")

            unzipToDirectory(tempZipFile, tempDir)

            val metadataFile = File(tempDir, "metadata.json")
            if (!metadataFile.exists()) throw IllegalStateException("备份缺少 metadata.json")

            val dbFromBackup = File(tempDir, "database.sqlite")
            if (!dbFromBackup.exists()) throw IllegalStateException("备份缺少 database.sqlite")
            val walFromBackup = File(tempDir, "database.sqlite-wal")
            val shmFromBackup = File(tempDir, "database.sqlite-shm")

            (context.applicationContext as? ConstructionLogApp)?.database?.close()

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

            // 4. 恢复 SharedPreferences（API Key、设置等）
            val prefsDir = File(context.dataDir.absolutePath, "shared_prefs")
            val backupPrefsDir = File(tempDir, "shared_prefs")
            if (backupPrefsDir.exists()) {
                prefsDir.mkdirs()
                backupPrefsDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.copyTo(File(prefsDir, file.name), overwrite = true)
                    }
                }
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun buildRawZip(targetZipFile: File, dbFile: File): String {
        ZipOutputStream(FileOutputStream(targetZipFile)).use { zip ->
            // 1. 备份数据库
            addFileToZip(zip, dbFile, "database.sqlite")
            val walFile = File(dbFile.parentFile, "${dbFile.name}-wal")
            val shmFile = File(dbFile.parentFile, "${dbFile.name}-shm")
            if (walFile.exists()) addFileToZip(zip, walFile, "database.sqlite-wal")
            if (shmFile.exists()) addFileToZip(zip, shmFile, "database.sqlite-shm")

            // 2. 备份图片
            val imageDir = context.getExternalFilesDir("Pictures")
            if (imageDir != null && imageDir.exists()) {
                imageDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        addFileToZip(zip, file, "images/${file.name}")
                    }
                }
            }

            // 3. 备份 SharedPreferences（API Key、设置等）
            val prefsDir = File(context.dataDir.absolutePath, "shared_prefs")
            if (prefsDir.exists()) {
                prefsDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension == "xml") {
                        addFileToZip(zip, file, "shared_prefs/${file.name}")
                    }
                }
            }

            val metadata = JSONObject()
                .put("version", 2)
                .put("exportTime", System.currentTimeMillis())
                .put("app", "construction-log")
                .put("includesSharedPrefs", true)
                .toString()
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(metadata.toByteArray())
            zip.closeEntry()
        }
        return sha256Hex(targetZipFile)
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun exportBackupInternal(writeOutput: ((OutputStream) -> Unit) -> Unit) {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) {
            throw IllegalStateException("数据库不存在")
        }

        val tempZipFile = File.createTempFile("backup_export_", ".zip", context.cacheDir)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        try {
            val sha256 = buildRawZip(tempZipFile, dbFile)
            writeOutput { output ->
                writeBinaryHeader(output, iv, sha256)
                encryptFileToOutput(tempZipFile, output, iv)
                output.flush()
            }
        } finally {
            tempZipFile.delete()
        }
    }

    private fun writeBinaryHeader(output: OutputStream, iv: ByteArray, sha256: String) {
        output.write(BACKUP_MAGIC)
        output.write(iv)
        output.write(sha256.toByteArray(Charsets.US_ASCII))
    }

    private fun encryptFileToOutput(sourceFile: File, output: OutputStream, iv: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, exportSecretKey(), GCMParameterSpec(128, iv))
        CipherOutputStream(output, cipher).use { cipherOutput ->
            FileInputStream(sourceFile).use { input ->
                input.copyTo(cipherOutput)
            }
        }
    }

    private fun isBinaryBackup(input: BufferedInputStream): Boolean {
        input.mark(BACKUP_MAGIC.size)
        val header = ByteArray(BACKUP_MAGIC.size)
        val read = input.read(header)
        input.reset()
        return read == BACKUP_MAGIC.size && header.contentEquals(BACKUP_MAGIC)
    }

    private fun importBinaryBackup(input: BufferedInputStream, targetZipFile: File) {
        val dataInput = DataInputStream(input)
        val magic = ByteArray(BACKUP_MAGIC.size)
        dataInput.readFully(magic)
        if (!magic.contentEquals(BACKUP_MAGIC)) {
            throw IllegalStateException("备份格式不兼容")
        }

        val iv = ByteArray(12)
        dataInput.readFully(iv)
        val shaBytes = ByteArray(SHA256_HEX_LENGTH)
        dataInput.readFully(shaBytes)
        val expectedSha = String(shaBytes, Charsets.US_ASCII)

        val encryptedPayload = dataInput.readBytes()
        targetZipFile.outputStream().use { output ->
            output.write(decryptWithFallback(encryptedPayload, iv))
        }
        val actualSha = sha256Hex(targetZipFile)
        if (!actualSha.equals(expectedSha, ignoreCase = true)) {
            throw IllegalStateException("备份完整性校验失败")
        }
    }

    private fun importLegacyBackup(input: InputStream, targetZipFile: File) {
        val payloadText = input.bufferedReader().readText()
        val payload = JSONObject(payloadText)
        val version = payload.optInt("version", -1)
        // 支持 version 1 和 version 2
        if (version != 1 && version != 2) throw IllegalStateException("备份版本不兼容")

        val iv = Base64.decode(payload.getString("iv"), Base64.NO_WRAP)
        val encrypted = Base64.decode(payload.getString("data"), Base64.NO_WRAP)
        val expectedSha = payload.getString("sha256")

        targetZipFile.outputStream().use { it.write(decryptWithFallback(encrypted, iv)) }
        val actualSha = sha256Hex(targetZipFile)
        if (!actualSha.equals(expectedSha, ignoreCase = true)) {
            throw IllegalStateException("备份完整性校验失败")
        }
    }

    private fun unzipToDirectory(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                ensureSafeZipPath(targetDir, outFile)
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

    private fun ensureSafeZipPath(rootDir: File, outFile: File) {
        val rootPath = rootDir.canonicalFile.toPath()
        val outPath = outFile.canonicalFile.toPath()
        if (!outPath.startsWith(rootPath)) {
            throw IllegalStateException("备份内容路径非法: ${outFile.path}")
        }
    }

    private fun decrypt(data: ByteArray, iv: ByteArray, secretKey: SecretKeySpec): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }

    private fun decryptWithFallback(data: ByteArray, iv: ByteArray): ByteArray {
        var lastError: Throwable? = null
        candidateSecretKeys().forEach { secretKey ->
            try {
                return decrypt(data, iv, secretKey)
            } catch (error: AEADBadTagException) {
                lastError = error
            } catch (error: IllegalStateException) {
                lastError = error
            }
        }
        throw IllegalStateException("备份解密失败，当前应用与备份使用的标识不兼容", lastError)
    }

    private fun exportSecretKey(): SecretKeySpec = secretKeyForSeed(STABLE_BACKUP_KEY_SEED)

    private fun candidateSecretKeys(): List<SecretKeySpec> = listOf(
        secretKeyForSeed(STABLE_BACKUP_KEY_SEED),
        secretKeyForSeed("${context.packageName}:construction-log-backup"),
        secretKeyForSeed("$LEGACY_PACKAGE_NAME:construction-log-backup")
    ).distinctBy { it.encoded?.toList() ?: emptyList() }

    private fun secretKeyForSeed(seed: String): SecretKeySpec {
        val hash = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
        return SecretKeySpec(hash.copyOfRange(0, 16), "AES")
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHexString()
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val DB_NAME = "construction_logs_secure.db"
        private val BACKUP_MAGIC = byteArrayOf('C'.code.toByte(), 'L'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte(), 2)
        private const val SHA256_HEX_LENGTH = 64
        private const val AUTO_BACKUP_FILE_NAME = "construction-log-auto-backup.zip"
        private const val LEGACY_PACKAGE_NAME = "com.example.constructionlog"
        private const val STABLE_BACKUP_KEY_SEED = "construction-log-backup-key-v1"
    }
}
