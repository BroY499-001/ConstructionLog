package com.constructionlog.app.data.backup

import android.content.Context
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

        // 任务开始提示
        withContext(Dispatchers.Main) {
            Toast.makeText(applicationContext, "正在执行自动备份...", Toast.LENGTH_SHORT).show()
        }

        val result = BackupService(applicationContext).exportAutoBackup()
        
        result.fold(
            onSuccess = {
                // 任务成功提示
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
