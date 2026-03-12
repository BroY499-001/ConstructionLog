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

@Entity(
    tableName = "acceptance_form",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["date"]), Index(value = ["stage"])]
)
data class AcceptanceFormEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val type: String,
    val stage: String,
    val date: Long,
    val weather: String = "",
    val location: String = "",
    val inspector: String = "",
    val remark: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "acceptance_item",
    foreignKeys = [
        ForeignKey(
            entity = AcceptanceFormEntity::class,
            parentColumns = ["id"],
            childColumns = ["formId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["formId"]), Index(value = ["category"])]
)
data class AcceptanceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val formId: Long,
    val orderIndex: Int,
    val category: String,
    val subItem: String,
    val standard: String,
    val basis: String,
    val status: String = AcceptanceStatus.PENDING,
    val note: String = "",
    val imageUris: String = ""
)

@Entity(
    tableName = "acceptance_material",
    foreignKeys = [
        ForeignKey(
            entity = AcceptanceFormEntity::class,
            parentColumns = ["id"],
            childColumns = ["formId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["formId"])]
)
data class AcceptanceMaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val formId: Long,
    val orderIndex: Int,
    val name: String,
    val brand: String,
    val spec: String,
    val status: String = AcceptanceStatus.PENDING,
    val note: String = ""
)

@Entity(
    tableName = "acceptance_image",
    foreignKeys = [
        ForeignKey(
            entity = AcceptanceFormEntity::class,
            parentColumns = ["id"],
            childColumns = ["formId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["formId"])]
)
data class AcceptanceImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val formId: Long,
    val imageUri: String,
    val createdAt: Long
)

object AcceptanceStatus {
    const val PENDING = "PENDING"
    const val PASS = "PASS"
    const val FAIL = "FAIL"
    const val NA = "NA"
}
