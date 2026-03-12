package com.constructionlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.constructionlog.app.data.AcceptanceFormWithDetails
import com.constructionlog.app.data.AcceptanceStatus
import com.constructionlog.app.data.ProjectEntity
import com.constructionlog.app.ui.components.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptanceHomeScreen(
    forms: List<AcceptanceFormWithDetails>,
    projects: List<ProjectEntity>,
    selectedProjectId: Long?,
    onSelectProject: (Long) -> Unit,
    onCreateWaterElectric: () -> Unit,
    onEdit: (AcceptanceFormWithDetails) -> Unit,
    onDelete: (AcceptanceFormWithDetails) -> Unit,
    onBack: () -> Unit
) {
    var showProjectSwitchMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<AcceptanceFormWithDetails?>(null) }
    val selectedProjectName = projects.firstOrNull { it.id == selectedProjectId }?.name ?: "未选择项目"

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
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Column {
                            Text("验收模块", style = MaterialTheme.typography.headlineSmall)
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
                    },
                    actions = {
                        TextButton(onClick = onBack) { Text("返回") }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("阶段验收", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "用于记录水电等阶段性验收，支持合格/不合格标记和图片留存。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = onCreateWaterElectric) { Text("新建水电验收表") }
                        }
                    }
                }

                if (forms.isEmpty()) {
                    item {
                        ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("暂无验收记录", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "点击上方按钮创建水电验收表。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(forms, key = { it.form.id }) { item ->
                        AcceptanceFormCard(
                            item = item,
                            onEdit = { onEdit(item) },
                            onDelete = { pendingDelete = item }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条验收记录？") },
            text = { Text("删除后无法恢复，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun AcceptanceFormCard(
    item: AcceptanceFormWithDetails,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val total = item.items.size
    val pass = item.items.count { it.status == AcceptanceStatus.PASS }
    val fail = item.items.count { it.status == AcceptanceStatus.FAIL }
    val pending = item.items.count { it.status == AcceptanceStatus.PENDING }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.form.stage,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${formatDate(item.form.date)}  ${item.form.weather.ifBlank { "天气未知" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "删除验收",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FactCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "合格 $pass / $total  ·  不合格 $fail  ·  待验 $pending",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "图片 ${item.images.size} 张  ·  材料 ${item.materials.size} 项",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
