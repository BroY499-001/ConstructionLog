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
        QualityIssueEntity::class,
        AcceptanceFormEntity::class,
        AcceptanceItemEntity::class,
        AcceptanceMaterialEntity::class,
        AcceptanceImageEntity::class
    ],
    version = 6,
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
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `acceptance_form` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `stage` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `weather` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `inspector` TEXT NOT NULL,
                        `remark` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`projectId`) REFERENCES `project`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_acceptance_form_projectId` ON `acceptance_form` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_acceptance_form_date` ON `acceptance_form` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_acceptance_form_stage` ON `acceptance_form` (`stage`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `acceptance_item` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `formId` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `subItem` TEXT NOT NULL,
                        `standard` TEXT NOT NULL,
                        `basis` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        FOREIGN KEY(`formId`) REFERENCES `acceptance_form`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_acceptance_item_formId` ON `acceptance_item` (`formId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_acceptance_item_category` ON `acceptance_item` (`category`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `acceptance_material` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `formId` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `brand` TEXT NOT NULL,
                        `spec` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        FOREIGN KEY(`formId`) REFERENCES `acceptance_form`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_acceptance_material_formId` ON `acceptance_material` (`formId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `acceptance_image` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `formId` INTEGER NOT NULL,
                        `imageUri` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`formId`) REFERENCES `acceptance_form`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_acceptance_image_formId` ON `acceptance_image` (`formId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE acceptance_item ADD COLUMN imageUris TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plan_task ADD COLUMN acceptanceFormId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_task_acceptanceFormId` ON `plan_task` (`acceptanceFormId`)")
            }
        }
    }
}
