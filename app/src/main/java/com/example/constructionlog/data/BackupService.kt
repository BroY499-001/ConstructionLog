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
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupService(private val context: Context) {

    fun exportBackup(targetUri: Uri): Result<Unit> = runCatching {
        doExport { writer ->
            context.contentResolver.openOutputStream(targetUri)?.use(writer)
                ?: throw IllegalStateException("无法写入导出文件")
        }
    }

    fun exportBackup(targetFile: File): Result<File> = runCatching {
        targetFile.parentFile?.mkdirs()
        val tmp = File(targetFile.parentFile ?: context.cacheDir, "${targetFile.name}.tmp")
        doExport { writer -> FileOutputStream(tmp).use(writer) }
        if (targetFile.exists() && !targetFile.delete()) throw IllegalStateException("无法覆盖旧备份文件")
        if (!tmp.renameTo(targetFile)) { tmp.copyTo(targetFile, overwrite = true); tmp.delete() }
        targetFile
    }

    fun exportAutoBackup(): Result<File> = exportBackup(autoBackupFile())
    fun deleteAutoBackup(): Boolean { val f = autoBackupFile(); return !f.exists() || f.delete() }

    fun autoBackupFile(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ConstructionLog")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, AUTO_BACKUP_NAME)
    }

    fun autoBackupDisplayName(): String = "下载/ConstructionLog/$AUTO_BACKUP_NAME"

    fun manualBackupFileName(ms: Long = System.currentTimeMillis()): String {
        val ts = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss", Locale.US))
        return "construction-log-backup-$ts.zip"
    }

    // ── import ────────────────────────────────────────────────

    fun importBackup(sourceUri: Uri): Result<Unit> = runCatching {
        val td = File(context.cacheDir, "import_${System.currentTimeMillis()}")
        val tz = File(td, "payload.zip"); td.mkdirs()
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { raw ->
                val b = BufferedInputStream(raw)
                if (isBinary(b)) importBinary(b, tz) else importLegacy(b, tz)
            } ?: throw IllegalStateException("无法读取备份文件")
            unzip(tz, td)
            if (!File(td, "metadata.json").exists()) throw IllegalStateException("备份缺少 metadata.json")
            val dbSrc = File(td, "database.sqlite")
            if (!dbSrc.exists()) throw IllegalStateException("备份缺少 database.sqlite")
            app()?.closeDatabase()
            val db = context.getDatabasePath(DB_NAME); db.parentFile?.mkdirs()
            listOf(db, File(db.parentFile, "$DB_NAME-wal"), File(db.parentFile, "$DB_NAME-shm")).forEach { if (it.exists()) it.delete() }
            dbSrc.copyTo(db, overwrite = true)
            File(td, "database.sqlite-wal").let { if (it.exists()) it.copyTo(File(db.parentFile, "$DB_NAME-wal"), true) }
            File(td, "database.sqlite-shm").let { if (it.exists()) it.copyTo(File(db.parentFile, "$DB_NAME-shm"), true) }
            context.getExternalFilesDir("Pictures")?.let { pd ->
                pd.mkdirs(); pd.listFiles()?.forEach { it.delete() }
                File(td, "images").takeIf { it.exists() }?.listFiles()?.forEach { f -> if (f.isFile) f.copyTo(File(pd, f.name), true) }
            }
            File(td, "shared_prefs").takeIf { it.exists() }?.let { sp ->
                val pd = File(context.dataDir.absolutePath, "shared_prefs"); pd.mkdirs()
                sp.listFiles()?.forEach { f -> if (f.isFile) f.copyTo(File(pd, f.name), true) }
            }
        } finally { td.deleteRecursively() }
    }

    // ── export core (v4: AES/CTR + HMAC, 真正流式) ────────────

    private fun doExport(writeOutput: ((OutputStream) -> Unit) -> Unit) {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) throw IllegalStateException("数据库不存在")
        app()?.closeDatabase()
        try {
            val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
            writeOutput { out ->
                // header: CLBK + version(4) + iv(16)
                out.write(MAGIC); out.write(byteArrayOf(VER_V4.toByte())); out.write(iv)
                // AES/CTR 是真正的流式加密，update() 立即返回密文
                val keys = deriveKeys(exportSeed())
                val cipher = Cipher.getInstance("AES/CTR/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, keys.first, IvParameterSpec(iv))
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(keys.second)
                // mac 覆盖 iv + 密文
                mac.update(iv)
                // 流式: ZIP → cipher.update → mac.update → output
                val zipBuf = object : OutputStream() {
                    override fun write(b: Int) { write(byteArrayOf(b.toByte()), 0, 1) }
                    override fun write(b: ByteArray, off: Int, len: Int) {
                        val enc = cipher.update(b, off, len) ?: return
                        mac.update(enc)
                        out.write(enc)
                    }
                }
                buildZip(zipBuf, dbFile)
                val last = cipher.doFinal()
                if (last != null && last.isNotEmpty()) { mac.update(last); out.write(last) }
                out.write(mac.doFinal()) // 32 bytes HMAC tag
                out.flush()
            }
        } finally { app()?.database }
    }

    private fun buildZip(out: OutputStream, dbFile: File) {
        ZipOutputStream(out).use { z ->
            zipAdd(z, dbFile, "database.sqlite")
            File(dbFile.parentFile, "${dbFile.name}-wal").let { if (it.exists()) zipAdd(z, it, "database.sqlite-wal") }
            File(dbFile.parentFile, "${dbFile.name}-shm").let { if (it.exists()) zipAdd(z, it, "database.sqlite-shm") }
            context.getExternalFilesDir("Pictures")?.takeIf { it.exists() }?.listFiles()?.forEach { f ->
                if (f.isFile) zipAdd(z, f, "images/${f.name}")
            }
            File(context.dataDir.absolutePath, "shared_prefs").takeIf { it.exists() }?.listFiles()?.forEach { f ->
                if (f.isFile && f.extension == "xml") zipAdd(z, f, "shared_prefs/${f.name}")
            }
            val m = JSONObject().put("version", 4).put("exportTime", System.currentTimeMillis())
                .put("app", "construction-log").put("includesSharedPrefs", true).toString().toByteArray()
            val me = ZipEntry("metadata.json").apply {
                method = ZipEntry.STORED; size = m.size.toLong(); compressedSize = m.size.toLong()
                crc = java.util.zip.CRC32().also { it.update(m) }.value
            }
            z.putNextEntry(me); z.write(m); z.closeEntry()
        }
    }

    private fun zipAdd(z: ZipOutputStream, file: File, name: String) {
        val e = ZipEntry(name).apply {
            method = ZipEntry.STORED; size = file.length(); compressedSize = file.length()
            crc = crc32(file)
        }
        z.putNextEntry(e); FileInputStream(file).use { it.copyTo(z, BUF) }; z.closeEntry()
    }

    private fun crc32(file: File): Long {
        val c = java.util.zip.CRC32()
        FileInputStream(file).use { i -> val b = ByteArray(BUF); var n: Int; while (i.read(b).also { n = it } != -1) c.update(b, 0, n) }
        return c.value
    }

    // ── import helpers ────────────────────────────────────────

    private fun isBinary(i: BufferedInputStream): Boolean {
        i.mark(MAGIC.size + 1); val h = ByteArray(MAGIC.size + 1); val n = i.read(h); i.reset()
        return n == h.size && h.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
    }

    private fun importBinary(input: BufferedInputStream, dst: File) {
        val d = DataInputStream(input)
        val p = ByteArray(MAGIC.size); d.readFully(p)
        if (!p.contentEquals(MAGIC)) throw IllegalStateException("备份格式不兼容")
        val ver = d.readUnsignedByte()
        when (ver) {
            VER_V4 -> importV4(d, dst)
            VER_V2, VER_V3 -> importV2V3(d, dst, ver)
            else -> throw IllegalStateException("不支持的备份版本: $ver")
        }
    }

    /** V4: AES/CTR + HMAC-SHA256, 流式解密 */
    private fun importV4(d: DataInputStream, dst: File) {
        val iv = ByteArray(16); d.readFully(iv)
        // 先把密文+HMAC 写到临时文件
        val enc = File(dst.parentFile ?: context.cacheDir, "v4_enc_${System.currentTimeMillis()}.tmp")
        try {
            enc.outputStream().use { o -> val b = ByteArray(BUF); var n: Int; while (d.read(b).also { n = it } != -1) o.write(b, 0, n) }
            val encLen = enc.length()
            if (encLen < HMAC_LEN) throw IllegalStateException("备份数据太短")
            // 验证 HMAC
            var last: Throwable? = null
            candidateSeeds().forEach { seed ->
                runCatching { decryptV4(enc, dst, iv, seed, encLen) }.onSuccess { return }.onFailure { last = it; if (dst.exists()) dst.delete() }
            }
            throw IllegalStateException("备份解密失败", last)
        } finally { if (enc.exists()) enc.delete() }
    }

    private fun decryptV4(enc: File, dst: File, iv: ByteArray, seed: String, encLen: Long) {
        val keys = deriveKeys(seed)
        val cipherDataLen = encLen - HMAC_LEN
        // 1. 验证 HMAC(iv + ciphertext)
        val mac = Mac.getInstance("HmacSHA256"); mac.init(keys.second)
        mac.update(iv)
        FileInputStream(enc).use { i ->
            val b = ByteArray(BUF); var remaining = cipherDataLen
            while (remaining > 0) {
                val toRead = minOf(BUF.toLong(), remaining).toInt()
                val n = i.read(b, 0, toRead); if (n == -1) break
                mac.update(b, 0, n); remaining -= n
            }
        }
        val computed = mac.doFinal()
        // 读取文件末尾的 HMAC tag
        val stored = ByteArray(HMAC_LEN)
        FileInputStream(enc).use { i -> i.skip(cipherDataLen); i.read(stored) }
        if (!MessageDigest.isEqual(computed, stored)) throw IllegalStateException("HMAC 验证失败")

        // 2. 流式解密
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keys.first, IvParameterSpec(iv))
        FileInputStream(enc).use { i -> FileOutputStream(dst).use { o ->
            val b = ByteArray(BUF); var remaining = cipherDataLen
            while (remaining > 0) {
                val toRead = minOf(BUF.toLong(), remaining).toInt()
                val n = i.read(b, 0, toRead); if (n == -1) break
                cipher.update(b, 0, n)?.let { o.write(it) }
                remaining -= n
            }
            cipher.doFinal()?.let { if (it.isNotEmpty()) o.write(it) }
        }}
    }

    /** V2/V3: 旧版 AES/GCM 格式兼容（写到临时文件再解密） */
    private fun importV2V3(d: DataInputStream, dst: File, ver: Int) {
        val iv = ByteArray(12); d.readFully(iv)
        if (ver == VER_V2) d.readFully(ByteArray(SHA_LEN))
        val enc = File(dst.parentFile ?: context.cacheDir, "enc_${System.currentTimeMillis()}.tmp")
        try {
            enc.outputStream().use { o -> val b = ByteArray(BUF); var n: Int; while (d.read(b).also { n = it } != -1) o.write(b, 0, n) }
            var last: Throwable? = null
            candidateSeeds().forEach { seed ->
                runCatching { decryptGcm(enc, dst, iv, gcmKey(seed)) }.onSuccess { return }.onFailure { last = it; if (dst.exists()) dst.delete() }
            }
            throw IllegalStateException("备份解密失败", last)
        } finally { if (enc.exists()) enc.delete() }
    }

    private fun importLegacy(input: InputStream, dst: File) {
        val tj = File(dst.parentFile ?: context.cacheDir, "leg_${System.currentTimeMillis()}.json")
        try {
            tj.outputStream().use { o -> val b = ByteArray(BUF); var n: Int; while (input.read(b).also { n = it } != -1) o.write(b, 0, n) }
            val j = JSONObject(tj.readText()); val iv = Base64.decode(j.getString("iv"), Base64.NO_WRAP)
            val ds = j.getString("data")
            val enc = File(dst.parentFile ?: context.cacheDir, "leg_enc_${System.currentTimeMillis()}.bin")
            try {
                enc.outputStream().use { o ->
                    val ch = BUF / 3 * 4; var off = 0
                    while (off < ds.length) { val end = minOf(off + ch, ds.length); o.write(Base64.decode(ds.substring(off, end), Base64.NO_WRAP)); off = end }
                }
                var last: Throwable? = null
                candidateSeeds().forEach { seed ->
                    runCatching { decryptGcm(enc, dst, iv, gcmKey(seed)) }.onSuccess { return }.onFailure { last = it; if (dst.exists()) dst.delete() }
                }
                throw IllegalStateException("备份解密失败", last)
            } finally { if (enc.exists()) enc.delete() }
        } finally { if (tj.exists()) tj.delete() }
    }

    private fun unzip(zip: File, dir: File) {
        ZipInputStream(FileInputStream(zip)).use { z ->
            var e = z.nextEntry; while (e != null) {
                val o = File(dir, e.name); safePath(dir, o)
                if (e.isDirectory) o.mkdirs() else { o.parentFile?.mkdirs(); FileOutputStream(o).use { z.copyTo(it, BUF) } }
                z.closeEntry(); e = z.nextEntry
            }
        }
    }

    private fun safePath(root: File, out: File) {
        if (!out.canonicalFile.toPath().startsWith(root.canonicalFile.toPath()))
            throw IllegalStateException("备份内容路径非法: ${out.path}")
    }

    // ── crypto helpers ────────────────────────────────────────

    /** GCM 解密（旧版兼容，流式读写避免 OOM） */
    private fun decryptGcm(src: File, dst: File, iv: ByteArray, key: SecretKeySpec) {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        FileInputStream(src).use { i -> FileOutputStream(dst).use { o ->
            val b = ByteArray(BUF); var n: Int
            while (i.read(b).also { n = it } != -1) c.update(b, 0, n)?.let { o.write(it) }
            c.doFinal()?.let { o.write(it) }
        }}
    }

    /** V4 密钥派生: SHA-512 → 前16字节 AES key + 后32字节 HMAC key */
    private fun deriveKeys(seed: String): Pair<SecretKeySpec, SecretKeySpec> {
        val h = MessageDigest.getInstance("SHA-512").digest(seed.toByteArray())
        return SecretKeySpec(h, 0, 16, "AES") to SecretKeySpec(h, 16, 32, "HmacSHA256")
    }

    /** 旧版 GCM 密钥 (SHA-256 前16字节) */
    private fun gcmKey(seed: String): SecretKeySpec {
        val h = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
        return SecretKeySpec(h.copyOfRange(0, 16), "AES")
    }

    private fun exportSeed() = STABLE_SEED
    private fun candidateSeeds() = listOf(STABLE_SEED, "${context.packageName}:construction-log-backup", "$LEG_PKG:construction-log-backup").distinct()
    private fun app() = context.applicationContext as? ConstructionLogApp

    companion object {
        private const val DB_NAME = "construction_logs_secure.db"
        private val MAGIC = byteArrayOf('C'.code.toByte(), 'L'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
        private const val VER_V2 = 2
        private const val VER_V3 = 3
        private const val VER_V4 = 4
        private const val SHA_LEN = 64
        private const val HMAC_LEN = 32
        private const val AUTO_BACKUP_NAME = "construction-log-auto-backup.zip"
        private const val LEG_PKG = "com.example.constructionlog"
        private const val STABLE_SEED = "construction-log-backup-key-v1"
        private const val BUF = 64 * 1024
    }
}
