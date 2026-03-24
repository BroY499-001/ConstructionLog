package com.constructionlog.app.data.backup

import android.content.Context
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

        // 尝试释放图片缓存，降低内存压力，但不强制 GC
        runCatching { applicationContext.imageLoader.memoryCache?.clear() }

        val result = BackupService(applicationContext).exportAutoBackup()

        result.fold(
            onSuccess = {
                AutoBackupNotifier.notifySuccess(applicationContext)
                Result.success()
            },
            onFailure = {
                AutoBackupNotifier.notifyFailure(applicationContext, it)
                Result.retry()
            }
        )
    }
}
