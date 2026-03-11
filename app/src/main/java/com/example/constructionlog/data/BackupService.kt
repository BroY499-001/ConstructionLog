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

    /**
     * 将自动备份路径改为公共下载文件夹
     */
    fun autoBackupFile(): File {
        // 使用公共下载目录：/storage/emulated/0/Download
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val backupDir = File(downloadDir, "ConstructionLog")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return File(backupDir, AUTO_BACKUP_FILE_NAME)
    }

    fun autoBackupDisplayName(): String = "下载/ConstructionLog/$AUTO_BACKUP_FILE_NAME"

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

    private fun writeZipToStream(output: OutputStream, dbFile: File) {
        ZipOutputStream(output).use { zip ->
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

            val prefsDir = File(context.dataDir.absolutePath, "shared_prefs")
            if (prefsDir.exists()) {
                prefsDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension == "xml") {
                        addFileToZip(zip, file, "shared_prefs/${file.name}")
                    }
                }
            }

            val metadata = JSONObject()
                .put("version", 3)
                .put("exportTime", System.currentTimeMillis())
                .put("app", "construction-log")
                .put("includesSharedPrefs", true)
                .toString()
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(metadata.toByteArray())
            zip.closeEntry()
        }
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

        val tempZip = File(context.cacheDir, "backup_${System.currentTimeMillis()}.zip.tmp")
        try {
            tempZip.outputStream().use { fos ->
                writeZipToStream(fos, dbFile)
            }

            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            writeOutput { output ->
                writeBinaryHeaderV3(output, iv)
                encryptFileToOutput(tempZip, output, iv)
                output.flush()
            }
        } finally {
            if (tempZip.exists()) tempZip.delete()
        }
    }

    private fun writeBinaryHeaderV3(output: OutputStream, iv: ByteArray) {
        output.write(BACKUP_MAGIC_PREFIX)
        output.write(byteArrayOf(BACKUP_VERSION_V3.toByte()))
        output.write(iv)
    }

    private fun encryptFileToOutput(file: File, output: OutputStream, iv: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, exportSecretKey(), GCMParameterSpec(128, iv))
        
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                val encrypted = cipher.update(buffer, 0, read)
                if (encrypted != null) output.write(encrypted)
            }
            val final = cipher.doFinal()
            if (final != null) output.write(final)
        }
    }

    private fun isBinaryBackup(input: BufferedInputStream): Boolean {
        input.mark(BACKUP_MAGIC_PREFIX.size + 1)
        val header = ByteArray(BACKUP_MAGIC_PREFIX.size + 1)
        val read = input.read(header)
        input.reset()
        if (read != header.size) return false
        val prefix = header.copyOfRange(0, BACKUP_MAGIC_PREFIX.size)
        return prefix.contentEquals(BACKUP_MAGIC_PREFIX)
    }

    private fun importBinaryBackup(input: BufferedInputStream, targetZipFile: File) {
        val dataInput = DataInputStream(input)
        val prefix = ByteArray(BACKUP_MAGIC_PREFIX.size)
        dataInput.readFully(prefix)
        if (!prefix.contentEquals(BACKUP_MAGIC_PREFIX)) {
            throw IllegalStateException("备份格式不兼容")
        }
        val version = dataInput.readUnsignedByte()

        val iv = ByteArray(12)
        dataInput.readFully(iv)
        if (version == BACKUP_VERSION_V2) {
            val shaBytes = ByteArray(SHA256_HEX_LENGTH)
            dataInput.readFully(shaBytes)
        }

        val encryptedTemp = File(targetZipFile.parentFile ?: context.cacheDir, "backup_payload_${System.currentTimeMillis()}.enc")
        try {
            encryptedTemp.outputStream().use { output ->
                dataInput.copyTo(output)
            }

            var lastError: Throwable? = null
            candidateSecretKeys().forEach { secretKey ->
                runCatching {
                    decryptFileToFile(encryptedTemp, targetZipFile, iv, secretKey)
                }.onSuccess {
                    return
                }.onFailure { error ->
                    lastError = error
                    if (targetZipFile.exists()) targetZipFile.delete()
                }
            }
            throw IllegalStateException("备份解密失败，当前应用与备份使用的标识不兼容", lastError)
        } finally {
            if (encryptedTemp.exists()) encryptedTemp.delete()
        }
    }

    private fun importLegacyBackup(input: InputStream, targetZipFile: File) {
        val payloadText = input.bufferedReader().readText()
        val payload = JSONObject(payloadText)
        val iv = Base64.decode(payload.getString("iv"), Base64.NO_WRAP)
        val encrypted = Base64.decode(payload.getString("data"), Base64.NO_WRAP)
        targetZipFile.outputStream().use { it.write(decryptWithFallback(encrypted, iv)) }
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
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw IllegalStateException("备份解密失败", lastError)
    }

    private fun decryptFileToFile(sourceFile: File, targetFile: File, iv: ByteArray, secretKey: SecretKeySpec) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    val decrypted = cipher.update(buffer, 0, read)
                    if (decrypted != null) output.write(decrypted)
                }
                val final = cipher.doFinal()
                if (final != null) output.write(final)
            }
        }
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

    companion object {
        private const val DB_NAME = "construction_logs_secure.db"
        private val BACKUP_MAGIC_PREFIX = byteArrayOf('C'.code.toByte(), 'L'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
        private const val BACKUP_VERSION_V2 = 2
        private const val BACKUP_VERSION_V3 = 3
        private const val SHA256_HEX_LENGTH = 64
        private const val AUTO_BACKUP_FILE_NAME = "construction-log-auto-backup.zip"
        private const val LEGACY_PACKAGE_NAME = "com.example.constructionlog"
        private const val STABLE_BACKUP_KEY_SEED = "construction-log-backup-key-v1"
    }
}
