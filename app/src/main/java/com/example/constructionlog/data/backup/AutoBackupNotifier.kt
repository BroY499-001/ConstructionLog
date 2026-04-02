package com.constructionlog.app.data.backup

import android.content.Context
import android.content.Intent

object AutoBackupNotifier {
    const val ACTION_RESULT = "com.constructionlog.app.action.AUTO_BACKUP_RESULT"
    const val EXTRA_SUCCESS = "success"
    const val EXTRA_ERROR_MESSAGE = "error_message"

    fun notifySuccess(context: Context) {
        context.sendBroadcast(
            Intent(ACTION_RESULT)
                .setPackage(context.packageName)
                .putExtra(EXTRA_SUCCESS, true)
        )
    }

    fun notifyFailure(context: Context, throwable: Throwable) {
        context.sendBroadcast(
            Intent(ACTION_RESULT)
                .setPackage(context.packageName)
                .putExtra(EXTRA_SUCCESS, false)
                .putExtra(EXTRA_ERROR_MESSAGE, throwable.message ?: "未知错误")
        )
    }
}
