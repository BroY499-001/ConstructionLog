package com.example.constructionlog.data

import kotlinx.coroutines.flow.Flow
import java.io.File
import android.net.Uri

class LogRepository(
    private val dao: LogDao
) {
    fun observeProjects(): Flow<List<ProjectEntity>> = dao.observeProjects()

    fun observeLogs(projectId: Long): Flow<List<LogWithImages>> = dao.observeActiveLogs(projectId)

    fun observeTrash(projectId: Long): Flow<List<LogWithImages>> = dao.observeDeletedLogs(projectId)

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
        remark: String,
        imageUris: List<String>
    ) {
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
                    remark = remark,
                    updateAt = now
                )
            )
            existingId
        }

        dao.deleteImagesByLogId(logId)
        dao.insertImages(imageUris.map { uri -> LogImageEntity(logId = logId, imageUri = uri, createdAt = now) })
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
        dao.clearAllLogs()
        dao.clearAllProjects()
    }

    /**
     * 清理物理存储中不再被数据库引用的孤立图片文件。
     * @param picturesDir 存储图片的物理目录
     */
    suspend fun cleanupOrphanedImages(picturesDir: File) {
        if (!picturesDir.exists() || !picturesDir.isDirectory) return

        // 1. 获取数据库中所有正在被引用的图片路径（去重）
        val referencedUris = dao.getAllReferencedImageUris().toSet()
        
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
}
