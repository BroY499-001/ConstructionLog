package com.constructionlog.app

import android.app.Application
import com.constructionlog.app.data.AppDatabase
import com.constructionlog.app.data.LogRepository
import com.constructionlog.app.data.backup.AutoBackupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConstructionLogApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: LogRepository by lazy { LogRepository(database.logDao()) }

    override fun onCreate() {
        super.onCreate()
        AutoBackupScheduler.sync(this)
        CoroutineScope(Dispatchers.IO).launch {
            val picturesDir = getExternalFilesDir("Pictures") ?: filesDir
            repository.migrateImageUrisToDirectory(picturesDir)
        }
    }
}
