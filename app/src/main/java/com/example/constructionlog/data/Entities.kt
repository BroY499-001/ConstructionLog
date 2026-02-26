package com.example.constructionlog.data

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
