package com.constructionlog.app.data

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

    @Query("SELECT DISTINCT imageUri FROM log_image")
    suspend fun getAllReferencedImageUris(): List<String>

    @Query("SELECT * FROM log_image")
    suspend fun getAllImages(): List<LogImageEntity>

    @Query("UPDATE log_image SET imageUri = :imageUri WHERE id = :id")
    suspend fun updateImageUri(id: Long, imageUri: String)

    @Transaction
    @Query("SELECT * FROM acceptance_form WHERE projectId = :projectId ORDER BY date DESC, updatedAt DESC")
    fun observeAcceptanceForms(projectId: Long): Flow<List<AcceptanceFormWithDetails>>

    @Transaction
    @Query("SELECT * FROM acceptance_form WHERE id = :id LIMIT 1")
    suspend fun getAcceptanceFormById(id: Long): AcceptanceFormWithDetails?

    @Insert
    suspend fun insertAcceptanceForm(form: AcceptanceFormEntity): Long

    @Update
    suspend fun updateAcceptanceForm(form: AcceptanceFormEntity)

    @Insert
    suspend fun insertAcceptanceItems(items: List<AcceptanceItemEntity>)

    @Insert
    suspend fun insertAcceptanceMaterials(items: List<AcceptanceMaterialEntity>)

    @Insert
    suspend fun insertAcceptanceImages(items: List<AcceptanceImageEntity>)

    @Query("DELETE FROM acceptance_item WHERE formId = :formId")
    suspend fun deleteAcceptanceItemsByFormId(formId: Long)

    @Query("DELETE FROM acceptance_material WHERE formId = :formId")
    suspend fun deleteAcceptanceMaterialsByFormId(formId: Long)

    @Query("DELETE FROM acceptance_image WHERE formId = :formId")
    suspend fun deleteAcceptanceImagesByFormId(formId: Long)

    @Query("DELETE FROM acceptance_form WHERE id = :id")
    suspend fun deleteAcceptanceForm(id: Long)

    @Query("SELECT DISTINCT imageUri FROM acceptance_image")
    suspend fun getAllAcceptanceImageUris(): List<String>

    @Query("SELECT * FROM acceptance_image")
    suspend fun getAllAcceptanceImages(): List<AcceptanceImageEntity>

    @Query("UPDATE acceptance_image SET imageUri = :imageUri WHERE id = :id")
    suspend fun updateAcceptanceImageUri(id: Long, imageUri: String)

    @Query("UPDATE construction_log SET deleted = 1, deletedAt = :deletedAt, updateAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteLog(id: Long, deletedAt: Long)

    @Query("UPDATE construction_log SET deleted = 0, deletedAt = NULL, updateAt = :time WHERE id = :id")
    suspend fun restoreLog(id: Long, time: Long)

    @Query("DELETE FROM construction_log WHERE id = :id")
    suspend fun hardDeleteLog(id: Long)

    @Query("DELETE FROM construction_log WHERE deleted = 1 AND deletedAt < :expiry")
    suspend fun cleanupExpiredDeleted(expiry: Long)

    @Query(
        "SELECT * FROM plan_task WHERE projectId = :projectId " +
            "ORDER BY done ASC, " +
            "CASE WHEN done = 0 THEN CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END ELSE 0 END ASC, " +
            "CASE WHEN done = 0 THEN dueAt END ASC, " +
            "CASE WHEN done = 1 THEN completedAt END DESC, " +
            "updatedAt DESC"
    )
    fun observePlanTasks(projectId: Long): Flow<List<PlanTaskEntity>>

    @Insert
    suspend fun insertPlanTask(task: PlanTaskEntity): Long

    @Query("SELECT * FROM plan_task WHERE id = :id LIMIT 1")
    suspend fun getPlanTaskById(id: Long): PlanTaskEntity?

    @Update
    suspend fun updatePlanTask(task: PlanTaskEntity)

    @Query("UPDATE plan_task SET done = :done, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePlanTaskDone(id: Long, done: Boolean, completedAt: Long?, updatedAt: Long)

    @Query("DELETE FROM plan_task WHERE id = :id")
    suspend fun deletePlanTask(id: Long)

    @Query("SELECT * FROM quality_issue WHERE projectId = :projectId ORDER BY CASE status WHEN 'OPEN' THEN 0 WHEN 'IN_PROGRESS' THEN 1 ELSE 2 END ASC, severity DESC, updatedAt DESC")
    fun observeQualityIssues(projectId: Long): Flow<List<QualityIssueEntity>>

    @Insert
    suspend fun insertQualityIssue(issue: QualityIssueEntity): Long

    @Query("UPDATE quality_issue SET status = :status, resolvedAt = :resolvedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateQualityIssueStatus(id: Long, status: String, resolvedAt: Long?, updatedAt: Long)

    @Query("DELETE FROM quality_issue WHERE id = :id")
    suspend fun deleteQualityIssue(id: Long)

    @Query("DELETE FROM construction_log")
    suspend fun clearAllLogs()

    @Query("DELETE FROM project")
    suspend fun clearAllProjects()

    @Query("DELETE FROM plan_task")
    suspend fun clearAllPlanTasks()

    @Query("DELETE FROM quality_issue")
    suspend fun clearAllQualityIssues()

    @Query("DELETE FROM acceptance_item")
    suspend fun clearAllAcceptanceItems()

    @Query("DELETE FROM acceptance_material")
    suspend fun clearAllAcceptanceMaterials()

    @Query("DELETE FROM acceptance_image")
    suspend fun clearAllAcceptanceImages()

    @Query("DELETE FROM acceptance_form")
    suspend fun clearAllAcceptanceForms()
}
