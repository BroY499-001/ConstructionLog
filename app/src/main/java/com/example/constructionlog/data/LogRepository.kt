package com.constructionlog.app.data

import kotlinx.coroutines.flow.Flow
import java.io.File
import android.net.Uri
import androidx.room.withTransaction

class LogRepository(
    private val database: AppDatabase
) {
    private val dao: LogDao = database.logDao()

    fun observeProjects(): Flow<List<ProjectEntity>> = dao.observeProjects()

    fun observeLogs(projectId: Long): Flow<List<LogWithImages>> = dao.observeActiveLogs(projectId)

    fun observeTrash(projectId: Long): Flow<List<LogWithImages>> = dao.observeDeletedLogs(projectId)

    fun observePlanTasks(projectId: Long): Flow<List<PlanTaskEntity>> = dao.observePlanTasks(projectId)

    fun observeQualityIssues(projectId: Long): Flow<List<QualityIssueEntity>> = dao.observeQualityIssues(projectId)

    fun observeAcceptanceForms(projectId: Long): Flow<List<AcceptanceFormWithDetails>> =
        dao.observeAcceptanceForms(projectId)

    suspend fun addProject(name: String): Long {
        val value = name.trim()
        require(value.isNotBlank()) { "项目名称不能为空" }
        require(dao.countProjectByName(value) == 0) { "项目名称已存在" }
        val now = System.currentTimeMillis()
        return dao.insertProject(
            ProjectEntity(
                name = value,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun renameProject(id: Long, name: String) {
        val value = name.trim()
        require(value.isNotBlank()) { "项目名称不能为空" }
        val current = dao.getProjectById(id) ?: throw IllegalArgumentException("项目不存在")
        if (current.name != value) {
            require(dao.countProjectByName(value) == 0) { "项目名称已存在" }
        }
        dao.updateProject(
            current.copy(
                name = value,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteProject(id: Long) {
        val count = dao.countLogsByProject(id)
        require(count == 0) { "项目下还有日志，不能删除" }
        dao.deleteProject(id)
    }

    suspend fun getById(id: Long): LogWithImages? = dao.getLogById(id)

    suspend fun getLogsInDateRange(projectId: Long, start: Long, end: Long): List<LogWithImages> =
        dao.getLogsInDateRange(projectId, start, end)

    suspend fun save(
        existingId: Long?,
        projectId: Long,
        date: Long,
        weather: String,
        location: String,
        content: String,
        workers: Int?,
        workerNames: String,
        safety: String,
        stage: String,
        remark: String,
        imageUris: List<String>
    ) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            val logId = if (existingId == null) {
                dao.insertLog(
                    ConstructionLogEntity(
                        projectId = projectId,
                        date = date,
                        weather = weather,
                        location = location,
                        content = content,
                        workers = workers,
                        workerNames = workerNames,
                        safety = safety,
                        stage = stage,
                        remark = remark,
                        createdAt = now,
                        updateAt = now
                    )
                )
            } else {
                val current = dao.getLogById(existingId)?.log
                    ?: throw IllegalArgumentException("Log not found: $existingId")
                dao.updateLog(
                    current.copy(
                        projectId = projectId,
                        date = date,
                        weather = weather,
                        location = location,
                        content = content,
                        workers = workers,
                        workerNames = workerNames,
                        safety = safety,
                        stage = stage,
                        remark = remark,
                        updateAt = now
                    )
                )
                existingId
            }

            dao.deleteImagesByLogId(logId)
            dao.insertImages(imageUris.map { uri ->
                LogImageEntity(logId = logId, imageUri = uri, createdAt = now)
            })
        }
    }

    suspend fun saveAcceptanceForm(
        existingId: Long?,
        projectId: Long,
        type: String,
        stage: String,
        date: Long,
        weather: String,
        location: String,
        inspector: String,
        remark: String,
        items: List<AcceptanceItemEntity>,
        materials: List<AcceptanceMaterialEntity>,
        imageUris: List<String>
    ): Long {
        return database.withTransaction {
            val now = System.currentTimeMillis()
            val formId = if (existingId == null) {
                dao.insertAcceptanceForm(
                    AcceptanceFormEntity(
                        projectId = projectId,
                        type = type,
                        stage = stage,
                        date = date,
                        weather = weather,
                        location = location,
                        inspector = inspector,
                        remark = remark,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                val current = dao.getAcceptanceFormById(existingId)?.form
                    ?: throw IllegalArgumentException("Acceptance form not found: $existingId")
                dao.updateAcceptanceForm(
                    current.copy(
                        projectId = projectId,
                        type = type,
                        stage = stage,
                        date = date,
                        weather = weather,
                        location = location,
                        inspector = inspector,
                        remark = remark,
                        updatedAt = now
                    )
                )
                existingId
            }

            dao.deleteAcceptanceItemsByFormId(formId)
            dao.deleteAcceptanceMaterialsByFormId(formId)
            dao.deleteAcceptanceImagesByFormId(formId)

            if (items.isNotEmpty()) {
                dao.insertAcceptanceItems(items.mapIndexed { index, item ->
                    item.copy(formId = formId, orderIndex = index)
                })
            }
            if (materials.isNotEmpty()) {
                dao.insertAcceptanceMaterials(materials.mapIndexed { index, item ->
                    item.copy(formId = formId, orderIndex = index)
                })
            }
            if (imageUris.isNotEmpty()) {
                dao.insertAcceptanceImages(imageUris.map { uri ->
                    AcceptanceImageEntity(formId = formId, imageUri = uri, createdAt = now)
                })
            }

            formId
        }
    }

    suspend fun deleteAcceptanceForm(id: Long) {
        dao.deletePlanTaskByAcceptanceFormId(id)
        dao.deleteAcceptanceForm(id)
    }

    suspend fun getAcceptanceFormById(id: Long): AcceptanceFormWithDetails? {
        return dao.getAcceptanceFormById(id)
    }

    suspend fun moveToTrash(id: Long) {
        dao.softDeleteLog(id, System.currentTimeMillis())
    }

    suspend fun restore(id: Long) {
        dao.restoreLog(id, System.currentTimeMillis())
    }

    suspend fun deleteForever(id: Long) {
        dao.hardDeleteLog(id)
    }

    suspend fun cleanupTrash(days: Long = 30) {
        val expiry = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000
        dao.cleanupExpiredDeleted(expiry)
    }

    suspend fun clearAllData() {
        dao.clearAllPlanTasks()
        dao.clearAllQualityIssues()
        dao.clearAllLogs()
        dao.clearAllAcceptanceItems()
        dao.clearAllAcceptanceMaterials()
        dao.clearAllAcceptanceImages()
        dao.clearAllAcceptanceForms()
        dao.clearAllProjects()
    }

    suspend fun addPlanTask(
        projectId: Long,
        title: String,
        detail: String,
        dueAt: Long?,
        priority: Int,
        acceptanceFormId: Long? = null
    ) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "计划标题不能为空" }
        val now = System.currentTimeMillis()
        dao.insertPlanTask(
            PlanTaskEntity(
                projectId = projectId,
                acceptanceFormId = acceptanceFormId,
                title = cleanTitle,
                detail = detail.trim(),
                dueAt = dueAt,
                priority = priority.coerceIn(1, 3),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun setPlanTaskDone(id: Long, done: Boolean) {
        val now = System.currentTimeMillis()
        dao.updatePlanTaskDone(id, done, if (done) now else null, now)
    }

    suspend fun deletePlanTask(id: Long) {
        dao.deletePlanTask(id)
    }

    suspend fun getPlanTaskByAcceptanceFormId(acceptanceFormId: Long): PlanTaskEntity? {
        return dao.getPlanTaskByAcceptanceFormId(acceptanceFormId)
    }

    suspend fun deletePlanTaskByAcceptanceFormId(acceptanceFormId: Long) {
        dao.deletePlanTaskByAcceptanceFormId(acceptanceFormId)
    }

    suspend fun updatePlanTask(id: Long, title: String, detail: String, dueAt: Long?, priority: Int) {
        val current = dao.getPlanTaskById(id) ?: throw IllegalArgumentException("计划不存在")
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "计划标题不能为空" }
        dao.updatePlanTask(
            current.copy(
                title = cleanTitle,
                detail = detail.trim(),
                dueAt = dueAt,
                priority = priority.coerceIn(1, 3),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addQualityIssue(
        projectId: Long,
        title: String,
        detail: String,
        severity: Int,
        responsible: String,
        dueAt: Long?
    ) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank()) { "问题标题不能为空" }
        val now = System.currentTimeMillis()
        dao.insertQualityIssue(
            QualityIssueEntity(
                projectId = projectId,
                title = cleanTitle,
                detail = detail.trim(),
                severity = severity.coerceIn(1, 3),
                responsible = responsible.trim(),
                dueAt = dueAt,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateQualityIssueStatus(id: Long, status: String) {
        require(status == IssueStatus.OPEN || status == IssueStatus.IN_PROGRESS || status == IssueStatus.RESOLVED) {
            "状态不合法"
        }
        val now = System.currentTimeMillis()
        dao.updateQualityIssueStatus(id, status, if (status == IssueStatus.RESOLVED) now else null, now)
    }

    suspend fun deleteQualityIssue(id: Long) {
        dao.deleteQualityIssue(id)
    }

    /**
     * 清理物理存储中不再被数据库引用的孤立图片文件。
     * @param picturesDir 存储图片的物理目录
     */
    suspend fun cleanupOrphanedImages(picturesDir: File) {
        if (!picturesDir.exists() || !picturesDir.isDirectory) return

        // 1. 获取数据库中所有正在被引用的图片路径（去重）
        val referencedUris = (dao.getAllReferencedImageUris() + dao.getAllAcceptanceImageUris()).toSet()
        
        // 2. 遍历物理目录下的所有文件
        picturesDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val fileUri = Uri.fromFile(file).toString()
                // 3. 如果物理文件不在数据库引用列表中，则删除该文件
                if (!referencedUris.contains(fileUri)) {
                    file.delete()
                }
            }
        }
    }

    suspend fun migrateImageUrisToDirectory(picturesDir: File) {
        if (!picturesDir.exists() || !picturesDir.isDirectory) return

        dao.getAllImages().forEach { image ->
            val uri = Uri.parse(image.imageUri)
            if (uri.scheme != "file") return@forEach

            val fileName = File(uri.path ?: return@forEach).name
            if (fileName.isBlank()) return@forEach

            val migratedFile = File(picturesDir, fileName)
            if (!migratedFile.exists()) return@forEach

            val migratedUri = Uri.fromFile(migratedFile).toString()
            if (migratedUri != image.imageUri) {
                dao.updateImageUri(image.id, migratedUri)
            }
        }

        dao.getAllAcceptanceImages().forEach { image ->
            val uri = Uri.parse(image.imageUri)
            if (uri.scheme != "file") return@forEach

            val fileName = File(uri.path ?: return@forEach).name
            if (fileName.isBlank()) return@forEach

            val migratedFile = File(picturesDir, fileName)
            if (!migratedFile.exists()) return@forEach

            val migratedUri = Uri.fromFile(migratedFile).toString()
            if (migratedUri != image.imageUri) {
                dao.updateAcceptanceImageUri(image.id, migratedUri)
            }
        }
    }
}
