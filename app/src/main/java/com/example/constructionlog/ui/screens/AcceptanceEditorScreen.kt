package com.constructionlog.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.constructionlog.app.data.AcceptanceStatus
import com.constructionlog.app.data.ProjectEntity
import com.constructionlog.app.ui.AcceptanceEditorState
import com.constructionlog.app.ui.AcceptanceItemDraft
import com.constructionlog.app.ui.AcceptanceTemplates
import com.constructionlog.app.ui.components.ImagePicker
import com.constructionlog.app.ui.components.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptanceEditorScreen(
    projects: List<ProjectEntity>,
    state: AcceptanceEditorState,
    onProjectChange: (Long) -> Unit,
    onDateChange: (Long) -> Unit,
    onTypeChange: (String) -> Unit,
    onStageChange: (String) -> Unit,
    onWeatherChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onInspectorChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onUpdateItem: (Int, AcceptanceItemDraft) -> Unit,
    onAddItemImageFromCamera: (Int) -> Unit,
    onAddItemImageFromGallery: (Int) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onRemoveImage: (String) -> Unit,
    onSave: () -> Unit,
    onAddFromCamera: () -> Unit,
    onAddFromGallery: () -> Unit,
    onAutoFetchWeather: (Long) -> Unit,
    onBack: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var projectExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var stageExpanded by remember { mutableStateOf(false) }
    val typeOptions = listOf("\u9636\u6BB5\u9A8C\u6536", "\u7AE3\u5DE5\u9A8C\u6536")
    val stageOptionsBase = AcceptanceTemplates.templateNames()
    val stageOptions = if (stageOptionsBase.contains(state.stage)) stageOptionsBase else stageOptionsBase + state.stage

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
                    Text(
                        text = if (state.editingId == null) "\u65B0\u5EFA\u9A8C\u6536" else "\u7F16\u8F91\u9A8C\u6536",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    TextButton(onClick = onBack) { Text("\u8FD4\u56DE") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BasicInfoCard(
                    projects = projects,
                    state = state,
                    projectExpanded = projectExpanded,
                    onProjectExpandedChange = { projectExpanded = it },
                    typeExpanded = typeExpanded,
                    onTypeExpandedChange = { typeExpanded = it },
                    stageExpanded = stageExpanded,
                    onStageExpandedChange = { stageExpanded = it },
                    typeOptions = typeOptions,
                    stageOptions = stageOptions,
                    onProjectChange = onProjectChange,
                    onDateClick = { showDatePicker = true },
                    onTypeChange = onTypeChange,
                    onStageChange = onStageChange,
                    onWeatherChange = onWeatherChange,
                    onLocationChange = onLocationChange,
                    onInspectorChange = onInspectorChange,
                    onRemarkChange = onRemarkChange,
                    onAutoFetchWeather = { onAutoFetchWeather(state.date) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("\u9A8C\u6536\u5B50\u9879", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onAddItem) { Text("+ \u6DFB\u52A0") }
                }
            }

            if (state.items.isEmpty()) {
                item {
                    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "\u6682\u65E0\u9A8C\u6536\u5B50\u9879",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            itemsIndexed(state.items) { index, item ->
                AcceptanceItemCard(
                    item = item,
                    index = index,
                    onUpdate = onUpdateItem,
                    onAddImageFromCamera = onAddItemImageFromCamera,
                    onAddImageFromGallery = onAddItemImageFromGallery,
                    onRemove = onRemoveItem
                )
            }

            item {
                ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("\u56FE\u7247\u8D44\u6599", style = MaterialTheme.typography.titleLarge)
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
                    Text("\u4FDD\u5B58\u9A8C\u6536", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                ) { Text("\u786E\u5B9A") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("\u53D6\u6D88") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun BasicInfoCard(
    projects: List<ProjectEntity>,
    state: AcceptanceEditorState,
    projectExpanded: Boolean,
    onProjectExpandedChange: (Boolean) -> Unit,
    typeExpanded: Boolean,
    onTypeExpandedChange: (Boolean) -> Unit,
    stageExpanded: Boolean,
    onStageExpandedChange: (Boolean) -> Unit,
    typeOptions: List<String>,
    stageOptions: List<String>,
    onProjectChange: (Long) -> Unit,
    onDateClick: () -> Unit,
    onTypeChange: (String) -> Unit,
    onStageChange: (String) -> Unit,
    onWeatherChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onInspectorChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onAutoFetchWeather: () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("\u57FA\u672C\u4FE1\u606F", style = MaterialTheme.typography.titleLarge)
            if (projects.isNotEmpty()) {
                DropdownField(
                    value = projects.firstOrNull { it.id == state.projectId }?.name ?: "\u672A\u9009\u62E9\u9879\u76EE",
                    label = "\u6240\u5C5E\u9879\u76EE",
                    expanded = projectExpanded,
                    onExpandedChange = onProjectExpandedChange,
                    options = projects.map { it.name },
                    onSelect = { idx -> onProjectChange(projects[idx].id) }
                )
            }
            OutlinedButton(
                onClick = onDateClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.EditCalendar, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("\u9A8C\u6536\u65F6\u95F4\uFF1A${formatDate(state.date)}")
            }
            DropdownField(
                value = state.type,
                label = "\u9A8C\u6536\u7C7B\u578B",
                expanded = typeExpanded,
                onExpandedChange = onTypeExpandedChange,
                options = typeOptions,
                onSelect = { idx -> onTypeChange(typeOptions[idx]) }
            )
            DropdownField(
                value = state.stage,
                label = "\u9A8C\u6536\u9636\u6BB5",
                expanded = stageExpanded,
                onExpandedChange = onStageExpandedChange,
                options = stageOptions,
                onSelect = { idx -> onStageChange(stageOptions[idx]) }
            )
            OutlinedTextField(
                value = state.weather,
                onValueChange = onWeatherChange,
                label = { Text("\u5929\u6C14") },
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
            TextButton(onClick = onAutoFetchWeather) {
                Text("\u81EA\u52A8\u83B7\u53D6\u5F53\u5929\u5929\u6C14")
            }
            OutlinedTextField(
                value = state.location,
                onValueChange = onLocationChange,
                label = { Text("\u9A8C\u6536\u5730\u70B9\uFF08\u5FC5\u586B\uFF09") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.inspector,
                onValueChange = onInspectorChange,
                label = { Text("\u9A8C\u6536\u4EBA\u5458") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = state.remark,
                onValueChange = onRemarkChange,
                label = { Text("\u5907\u6CE8") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DropdownField(
    value: String,
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onExpandedChange(true) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            options.forEachIndexed { idx, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(idx)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun AcceptanceItemCard(
    item: AcceptanceItemDraft,
    index: Int,
    onUpdate: (Int, AcceptanceItemDraft) -> Unit,
    onAddImageFromCamera: (Int) -> Unit,
    onAddImageFromGallery: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    val statusColor = when (item.status) {
        AcceptanceStatus.PASS -> Color(0xFF4CAF50)
        AcceptanceStatus.FAIL -> Color(0xFFF44336)
        AcceptanceStatus.NA -> Color(0xFF9E9E9E)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        // 顶部状态色条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(statusColor, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column {
                        Text(item.category.ifBlank { "\u672A\u5206\u7C7B" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.subItem.ifBlank { "\u672A\u547D\u540D" },
                            style = MaterialTheme.typography.titleSmall)
                    }
                }
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "\u5220\u9664",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.category,
                    onValueChange = { onUpdate(index, item.copy(category = it)) },
                    label = { Text("\u9879\u76EE\u5206\u7C7B") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = item.subItem,
                    onValueChange = { onUpdate(index, item.copy(subItem = it)) },
                    label = { Text("\u9A8C\u6536\u5B50\u9879") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = item.standard,
                onValueChange = { onUpdate(index, item.copy(standard = it)) },
                label = { Text("\u9A8C\u6536\u6807\u51C6") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = item.basis,
                onValueChange = { onUpdate(index, item.copy(basis = it)) },
                label = { Text("\u4F9D\u636E") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 材料信息
            if (item.materialName.isNotBlank() || item.materialBrand.isNotBlank() || item.materialSpec.isNotBlank()) {
                SectionLabel("\u6750\u6599\u4FE1\u606F")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.materialName,
                    onValueChange = { onUpdate(index, item.copy(materialName = it)) },
                    label = { Text("\u6750\u6599") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = item.materialBrand,
                    onValueChange = { onUpdate(index, item.copy(materialBrand = it)) },
                    label = { Text("\u54C1\u724C") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = item.materialSpec,
                    onValueChange = { onUpdate(index, item.copy(materialSpec = it)) },
                    label = { Text("\u89C4\u683C") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // 状态选择 - 图标+颜色
            SectionLabel("\u9A8C\u6536\u7ED3\u679C")
            StatusSelector(
                value = item.status,
                onChange = { onUpdate(index, item.copy(status = it)) }
            )

            // 图片 + 备注
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${item.imageUris.size} \u5F20",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalIconButton(onClick = { onAddImageFromCamera(index) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = "\u62CD\u7167", modifier = Modifier.size(18.dp))
                }
                FilledTonalIconButton(onClick = { onAddImageFromGallery(index) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Image, contentDescription = "\u76F8\u518C", modifier = Modifier.size(18.dp))
                }
            }
            OutlinedTextField(
                value = item.note,
                onValueChange = { onUpdate(index, item.copy(note = it)) },
                label = { Text("\u5907\u6CE8") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun StatusSelector(
    value: String,
    onChange: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusOption(
            icon = Icons.Rounded.HourglassEmpty,
            label = "\u5F85\u9A8C",
            color = Color(0xFFFF9800),
            selected = value == AcceptanceStatus.PENDING,
            onClick = { onChange(AcceptanceStatus.PENDING) }
        )
        StatusOption(
            icon = Icons.Rounded.CheckCircle,
            label = "\u5408\u683C",
            color = Color(0xFF4CAF50),
            selected = value == AcceptanceStatus.PASS,
            onClick = { onChange(AcceptanceStatus.PASS) }
        )
        StatusOption(
            icon = Icons.Rounded.Cancel,
            label = "\u4E0D\u5408\u683C",
            color = Color(0xFFF44336),
            selected = value == AcceptanceStatus.FAIL,
            onClick = { onChange(AcceptanceStatus.FAIL) }
        )
        StatusOption(
            icon = Icons.Rounded.HelpOutline,
            label = "N/A",
            color = Color(0xFF9E9E9E),
            selected = value == AcceptanceStatus.NA,
            onClick = { onChange(AcceptanceStatus.NA) }
        )
    }
}

@Composable
private fun StatusOption(
    icon: ImageVector,
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) color.copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val borderColor = if (selected) color else MaterialTheme.colorScheme.outlineVariant

    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = bgColor,
            contentColor = contentColor
        )
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
