package com.example.constructionlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM project ORDER BY updatedAt DESC, id DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM project WHERE name = :name")
    suspend fun countProjectByName(name: String): Int

    @Query("SELECT * FROM project WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM project WHERE id = :id")
    suspend fun deleteProject(id: Long)

    @Query("SELECT COUNT(*) FROM construction_log WHERE projectId = :projectId")
    suspend fun countLogsByProject(projectId: Long): Int

    @Transaction
    @Query("SELECT * FROM construction_log WHERE deleted = 0 AND projectId = :projectId ORDER BY date DESC")
    fun observeActiveLogs(projectId: Long): Flow<List<LogWithImages>>

    @Transaction
    @Query("SELECT * FROM construction_log WHERE deleted = 1 AND projectId = :projectId ORDER BY deletedAt DESC")
    fun observeDeletedLogs(projectId: Long): Flow<List<LogWithImages>>

    @Transaction
    @Query("SELECT * FROM construction_log WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): LogWithImages?

    @Transaction
    @Query("SELECT * FROM construction_log WHERE deleted = 0 AND projectId = :projectId AND date >= :start AND date < :end ORDER BY date ASC")
    suspend fun getLogsInDateRange(projectId: Long, start: Long, end: Long): List<LogWithImages>

    @Insert
    suspend fun insertLog(log: ConstructionLogEntity): Long

    @Update
    suspend fun updateLog(log: ConstructionLogEntity)

    @Insert
    suspend fun insertImages(images: List<LogImageEntity>)

    @Query("DELETE FROM log_image WHERE logId = :logId")
    suspend fun deleteImagesByLogId(logId: Long)

    @Query("UPDATE construction_log SET deleted = 1, deletedAt = :deletedAt, updateAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteLog(id: Long, deletedAt: Long)

    @Query("UPDATE construction_log SET deleted = 0, deletedAt = NULL, updateAt = :time WHERE id = :id")
    suspend fun restoreLog(id: Long, time: Long)

    @Query("DELETE FROM construction_log WHERE id = :id")
    suspend fun hardDeleteLog(id: Long)

    @Query("DELETE FROM construction_log WHERE deleted = 1 AND deletedAt < :expiry")
    suspend fun cleanupExpiredDeleted(expiry: Long)

    @Query("DELETE FROM construction_log")
    suspend fun clearAllLogs()

    @Query("DELETE FROM project")
    suspend fun clearAllProjects()
}
