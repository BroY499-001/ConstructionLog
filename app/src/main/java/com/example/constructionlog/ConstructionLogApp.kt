package com.example.constructionlog

import android.app.Application
import com.example.constructionlog.data.AppDatabase
import com.example.constructionlog.data.LogRepository

class ConstructionLogApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: LogRepository by lazy { LogRepository(database.logDao()) }
}
