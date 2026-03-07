package com.example.constructionlog

import android.app.Application
import com.example.constructionlog.data.AppDatabase
import com.example.constructionlog.data.LogRepository
import com.example.constructionlog.data.backup.AutoBackupScheduler

class ConstructionLogApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: LogRepository by lazy { LogRepository(database.logDao()) }

    override fun onCreate() {
        super.onCreate()
        AutoBackupScheduler.sync(this)
    }
}
