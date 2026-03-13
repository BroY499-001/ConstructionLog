package com.constructionlog.app.data.backup

import android.content.Context
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.imageLoader
import com.constructionlog.app.data.AppSettings
import com.constructionlog.app.data.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!AppSettings(applicationContext).isAutoBackupEnabled()) {
            return@withContext Result.success()
        }

        // 释放图片缓存，降低备份时的内存压力
        runCatching { applicationContext.imageLoader.memoryCache?.clear() }
        System.gc()

        withContext(Dispatchers.Main) {
            Toast.makeText(applicationContext, "正在执行自动备份...", Toast.LENGTH_SHORT).show()
        }

        val result = BackupService(applicationContext).exportAutoBackup()

        result.fold(
            onSuccess = {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "自动备份已完成", Toast.LENGTH_SHORT).show()
                }
                Result.success()
            },
            onFailure = {
                Result.retry()
            }
        )
    }
}
