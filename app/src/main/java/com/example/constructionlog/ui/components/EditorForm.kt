package com.constructionlog.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.constructionlog.app.data.ProjectEntity
import com.constructionlog.app.ui.EditorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorForm(
    projects: List<ProjectEntity>,
    state: EditorState,
    onProjectChange: (Long) -> Unit,
    onDateChange: (Long) -> Unit,
    onWeatherChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onWorkersChange: (String) -> Unit,
    onWorkerNamesChange: (String) -> Unit,
    onSafetyChange: (String) -> Unit,
    onStageChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onRemoveImage: (String) -> Unit,
    onSave: () -> Unit,
    onAddFromCamera: () -> Unit,
    onAddFromGallery: () -> Unit,
    onAutoFetchWeather: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var projectExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("基础信息", style = MaterialTheme.typography.titleLarge)
                    if (projects.isNotEmpty()) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = projects.firstOrNull { it.id == state.projectId }?.name ?: "未选择项目",
                                onValueChange = {},
                                label = { Text("所属项目") },
                                readOnly = true,
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { projectExpanded = true }
                            )
                            DropdownMenu(
                                expanded = projectExpanded,
                                onDismissRequest = { projectExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                projects.forEach { project ->
                                    DropdownMenuItem(
                                        text = { Text(project.name) },
                                        onClick = {
                                            onProjectChange(project.id)
                                            projectExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.EditCalendar, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("日期：${formatDate(state.date)}")
                    }
                    OutlinedTextField(
                        value = state.weather,
                        onValueChange = onWeatherChange,
                        label = { Text("天气") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (state.weatherSource.isNotBlank()) {
                        Text(
                            text = state.weatherSource,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onAutoFetchWeather(state.date) }) {
                        Text("自动获取当天天气")
                    }
                    OutlinedTextField(
                        value = state.location,
                        onValueChange = onLocationChange,
                        label = { Text("施工部位（必填）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.content,
                        onValueChange = onContentChange,
                        label = { Text("施工内容（必填）") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                    OutlinedTextField(
                        value = state.stage,
                        onValueChange = onStageChange,
                        label = { Text("阶段（可选，留空则自动识别）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("现场细节", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = state.workers,
                        onValueChange = onWorkersChange,
                        label = { Text("人员数量") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.workerNames,
                        onValueChange = onWorkerNamesChange,
                        label = { Text("人员名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.safety,
                        onValueChange = onSafetyChange,
                        label = { Text("安全文明情况") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.remark,
                        onValueChange = onRemarkChange,
                        label = { Text("备注") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("图片资料", style = MaterialTheme.typography.titleLarge)
                    ImagePicker(
                        imageUris = state.imageUris,
                        onAddFromCamera = onAddFromCamera,
                        onAddFromGallery = onAddFromGallery,
                        onRemoveImage = onRemoveImage
                    )
                }
            }
        }

        item {
            Button(
                onClick = onSave,
                enabled = state.isValid(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("保存日志", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onDateChange)
                        showDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
