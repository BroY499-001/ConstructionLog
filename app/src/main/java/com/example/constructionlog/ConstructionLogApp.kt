package com.constructionlog.app

import android.app.Application
import com.constructionlog.app.data.AppDatabase
import com.constructionlog.app.data.LogRepository
import com.constructionlog.app.data.backup.AutoBackupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConstructionLogApp : Application() {
    @Volatile
    private var _database: AppDatabase? = null
    private val dbLock = Any()

    val database: AppDatabase
        get() = _database ?: synchronized(dbLock) {
            _database ?: AppDatabase.build(this).also { _database = it }
        }

    val repository: LogRepository
        get() = LogRepository(database)

    override fun onCreate() {
        super.onCreate()
        AutoBackupScheduler.sync(this)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val picturesDir = getExternalFilesDir("Pictures") ?: filesDir
                repository.migrateImageUrisToDirectory(picturesDir)
            }
        }
    }

    /**
     * 关闭数据库，供导入备份这类需要替换底层数据库文件的场景使用。
     */
    fun closeDatabase() {
        synchronized(dbLock) {
            _database?.close()
            _database = null
        }
    }
}
