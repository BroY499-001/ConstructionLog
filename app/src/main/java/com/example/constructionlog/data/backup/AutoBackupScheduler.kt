package com.constructionlog.app.data.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.constructionlog.app.data.AppSettings
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {
    private const val PERIODIC_WORK_NAME = "construction_log_auto_backup_periodic"
    private const val IMMEDIATE_WORK_NAME = "construction_log_auto_backup_immediate"

    fun sync(context: Context) {
        if (AppSettings(context).isAutoBackupEnabled()) {
            schedule(context)
        } else {
            cancel(context)
        }
    }

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork
        )
    }

    fun requestImmediate(context: Context) {
        val work = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_NAME)
    }
}
