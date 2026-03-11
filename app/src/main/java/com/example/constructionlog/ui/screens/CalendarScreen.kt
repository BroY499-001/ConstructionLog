package com.constructionlog.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.constructionlog.app.data.LogWithImages
import com.constructionlog.app.data.PlanTaskEntity
import com.constructionlog.app.ui.components.CalendarHome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    logs: List<LogWithImages>,
    planTasks: List<PlanTaskEntity>,
    onEdit: (LogWithImages) -> Unit,
    onDelete: (Long) -> Unit,
    onAddPlanTask: (title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onUpdatePlanTask: (id: Long, title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onTogglePlanTask: (id: Long, done: Boolean) -> Unit,
    onDeletePlanTask: (Long) -> Unit,
    onBack: () -> Unit
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
                title = { Text("日历", style = MaterialTheme.typography.headlineSmall) },
                actions = { TextButton(onClick = onBack) { Text("日志") } }
            )
        }
    ) { padding ->
        CalendarHome(
            logs = logs,
            planTasks = planTasks,
            onEdit = onEdit,
            onDelete = onDelete,
            onAddPlanTask = onAddPlanTask,
            onUpdatePlanTask = onUpdatePlanTask,
            onTogglePlanTask = onTogglePlanTask,
            onDeletePlanTask = onDeletePlanTask,
            modifier = Modifier.padding(padding)
        )
    }
}
