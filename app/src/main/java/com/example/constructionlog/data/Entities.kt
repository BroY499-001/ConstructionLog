package com.constructionlog.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "project")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "construction_log",
    indices = [Index(value = ["projectId"])]
)
data class ConstructionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long = DEFAULT_PROJECT_ID,
    val date: Long,
    val weather: String = "",
    val location: String = "",
    val content: String,
    val workers: Int? = null,
    val workerNames: String = "",
    val safety: String = "",
    val stage: String = "",
    val remark: String = "",
    val createdAt: Long,
    val updateAt: Long,
    val deleted: Boolean = false,
    val deletedAt: Long? = null
) {
    companion object {
        const val DEFAULT_PROJECT_ID = 0L
    }
}

@Entity(
    tableName = "plan_task",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["done"]), Index(value = ["dueAt"])]
)
data class PlanTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val detail: String = "",
    val dueAt: Long? = null,
    val done: Boolean = false,
    val priority: Int = 1,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null
)

@Entity(
    tableName = "quality_issue",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["status"]), Index(value = ["dueAt"])]
)
data class QualityIssueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val detail: String = "",
    val severity: Int = 1,
    val status: String = IssueStatus.OPEN,
    val responsible: String = "",
    val dueAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedAt: Long? = null
)

object IssueStatus {
    const val OPEN = "OPEN"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val RESOLVED = "RESOLVED"
}

@Entity(
    tableName = "log_image",
    foreignKeys = [
        ForeignKey(
            entity = ConstructionLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["logId"])]
)
data class LogImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logId: Long,
    val imageUri: String,
    val createdAt: Long
)
