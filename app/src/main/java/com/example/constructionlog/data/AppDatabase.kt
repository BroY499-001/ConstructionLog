package com.constructionlog.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        ProjectEntity::class,
        ConstructionLogEntity::class,
        LogImageEntity::class,
        PlanTaskEntity::class,
        QualityIssueEntity::class
    ],
    version = 3,
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
                .addMigrations(MIGRATION_2_3)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE construction_log ADD COLUMN stage TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `plan_task` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `detail` TEXT NOT NULL,
                        `dueAt` INTEGER,
                        `done` INTEGER NOT NULL,
                        `priority` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        FOREIGN KEY(`projectId`) REFERENCES `project`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_task_projectId` ON `plan_task` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_task_done` ON `plan_task` (`done`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_task_dueAt` ON `plan_task` (`dueAt`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quality_issue` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `detail` TEXT NOT NULL,
                        `severity` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `responsible` TEXT NOT NULL,
                        `dueAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `resolvedAt` INTEGER,
                        FOREIGN KEY(`projectId`) REFERENCES `project`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quality_issue_projectId` ON `quality_issue` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quality_issue_status` ON `quality_issue` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quality_issue_dueAt` ON `quality_issue` (`dueAt`)")
            }
        }
    }
}
