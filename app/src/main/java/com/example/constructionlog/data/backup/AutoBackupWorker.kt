package com.example.constructionlog.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.constructionlog.data.AppSettings
import com.example.constructionlog.data.BackupService
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

        BackupService(applicationContext).exportAutoBackup()
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() }
            )
    }
}
