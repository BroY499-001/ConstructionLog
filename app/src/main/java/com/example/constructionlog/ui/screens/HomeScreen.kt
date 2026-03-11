package com.constructionlog.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.constructionlog.app.data.LogWithImages
import com.constructionlog.app.data.PlanTaskEntity
import com.constructionlog.app.data.ProjectEntity
import com.constructionlog.app.ui.components.CalendarHome
import com.constructionlog.app.ui.components.DockBar
import com.constructionlog.app.ui.components.ProjectManagerDialog
import com.constructionlog.app.ui.components.ReminderListPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    logs: List<LogWithImages>,
    planTasks: List<PlanTaskEntity>,
    projects: List<ProjectEntity>,
    projectsLoaded: Boolean,
    selectedProjectId: Long?,
    onSelectProject: (Long) -> Unit,
    onAddProject: (String) -> Unit,
    onRenameProject: (Long, String) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onStartCreate: () -> Unit,
    onStartEdit: (LogWithImages) -> Unit,
    onDelete: (Long) -> Unit,
    onAddPlanTask: (title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onUpdatePlanTask: (id: Long, title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onTogglePlanTask: (id: Long, done: Boolean) -> Unit,
    onDeletePlanTask: (Long) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    onImportBackup: () -> Unit
) {
    var showProjectManager by remember { mutableStateOf(false) }
    var showReminderPage by remember { mutableStateOf(false) }
    var showProjectSwitchMenu by remember { mutableStateOf(false) }
    var requiredProjectName by remember { mutableStateOf("") }
    val selectedProjectName = projects.firstOrNull { it.id == selectedProjectId }?.name ?: "未选择项目"

    BackHandler(enabled = showProjectManager || showReminderPage || showProjectSwitchMenu) {
        when {
            showProjectSwitchMenu -> showProjectSwitchMenu = false
            showProjectManager -> showProjectManager = false
            showReminderPage -> showReminderPage = false
        }
    }

    val pageGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFF7F9FC),
            Color(0xFFF2F5FA),
            Color(0xFFEEF3FB)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    title = {
                        Column {
                            Text(
                                text = if (showReminderPage) "提醒" else "装修日记",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            if (!showReminderPage) {
                                Box {
                                    Text(
                                        text = "项目：$selectedProjectName  ▾",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable { showProjectSwitchMenu = true }
                                    )
                                    DropdownMenu(
                                        expanded = showProjectSwitchMenu,
                                        onDismissRequest = { showProjectSwitchMenu = false }
                                    ) {
                                        projects.forEach { project ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        if (project.id == selectedProjectId) {
                                                            "${project.name}（当前）"
                                                        } else {
                                                            project.name
                                                        }
                                                    )
                                                },
                                                onClick = {
                                                    onSelectProject(project.id)
                                                    showProjectSwitchMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        if (showReminderPage) {
                            TextButton(onClick = { showReminderPage = false }) { Text("完成") }
                        } else {
                            IconButton(onClick = onOpenTrash) {
                                Icon(imageVector = Icons.Rounded.Delete, contentDescription = "回收站")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Crossfade(targetState = showReminderPage, label = "home-page") { showReminder ->
                if (showReminder) {
                    ReminderListPage(
                        tasks = planTasks,
                        onAddPlanTask = onAddPlanTask,
                        onUpdatePlanTask = onUpdatePlanTask,
                        onTogglePlanTask = onTogglePlanTask,
                        onDeletePlanTask = onDeletePlanTask,
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    CalendarHome(
                        logs = logs,
                        planTasks = planTasks,
                        onEdit = onStartEdit,
                        onDelete = onDelete,
                        onAddPlanTask = onAddPlanTask,
                        onUpdatePlanTask = onUpdatePlanTask,
                        onTogglePlanTask = onTogglePlanTask,
                        onDeletePlanTask = onDeletePlanTask,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            DockBar(
                visible = !showProjectManager && !showReminderPage,
                showProjectManager = showProjectManager,
                showReminderPage = showReminderPage,
                onShowProjectManager = { showProjectManager = true },
                onShowReminderPage = { showReminderPage = true },
                onStartCreate = onStartCreate,
                onOpenSettings = onOpenSettings
            )
        }

        if (showProjectManager) {
            ProjectManagerDialog(
                projects = projects,
                selectedProjectId = selectedProjectId,
                onDismiss = { showProjectManager = false },
                onSelectProject = onSelectProject,
                onAddProject = onAddProject,
                onRenameProject = onRenameProject,
                onDeleteProject = onDeleteProject
            )
        }

        if (projectsLoaded && projects.isEmpty()) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("先创建项目") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("当前没有项目，需先创建一个项目才能开始记录装修日记。")
                        OutlinedTextField(
                            value = requiredProjectName,
                            onValueChange = { requiredProjectName = it },
                            label = { Text("项目名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (requiredProjectName.isNotBlank()) {
                                onAddProject(requiredProjectName.trim())
                                requiredProjectName = ""
                            }
                        },
                        enabled = requiredProjectName.isNotBlank()
                    ) {
                        Text("创建项目")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onImportBackup) { Text("导入旧数据") }
                }
            )
        }
    }
}
