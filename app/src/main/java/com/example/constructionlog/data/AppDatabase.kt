package com.example.constructionlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [ProjectEntity::class, ConstructionLogEntity::class, LogImageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    companion object {
        fun build(context: Context): AppDatabase {
            val passphrase = SQLiteDatabase.getBytes(DB_PASSPHRASE.toCharArray())
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DB_NAME
            ).openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private const val DB_NAME = "construction_logs_secure.db"
        private const val DB_PASSPHRASE = "construction_log_secure_db_v1"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE construction_log ADD COLUMN projectId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_construction_log_projectId` ON `construction_log` (`projectId`)")
            }
        }
    }
}
