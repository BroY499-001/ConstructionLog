package com.constructionlog.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    authEnabled: Boolean,
    reauthSeconds: Int,
    autoBackupEnabled: Boolean,
    autoBackupSummary: String,
    onBack: () -> Unit,
    onAuthSwitchChange: (Boolean) -> Unit,
    onReauthSecondsChange: (Int) -> Unit,
    onAutoBackupSwitchChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportPdf: (Long) -> Unit,
    onClearAllData: () -> Unit,
    amapKeyConfigured: Boolean,
    onSaveAmapKey: (String) -> Unit,
    qWeatherKeyConfigured: Boolean,
    onSaveQWeatherKey: (String) -> Unit,
    qWeatherHostConfigured: Boolean,
    onSaveQWeatherHost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedPdfDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showPdfDatePicker by remember { mutableStateOf(false) }
    var amapKeyInput by remember { mutableStateOf("") }
    var qWeatherKeyInput by remember { mutableStateOf("") }
    var qWeatherHostInput by remember { mutableStateOf("") }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("设置", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onBack) {
                            Text("返回")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(0.82f)) {
                            Text("启动身份验证", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "默认关闭。开启后可用指纹或锁屏密码验证进入。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = authEnabled,
                            onCheckedChange = onAuthSwitchChange
                        )
                    }

                    Text("重新验证时间", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReauthChip(label = "30秒", selected = reauthSeconds == 30) { onReauthSecondsChange(30) }
                        ReauthChip(label = "1分钟", selected = reauthSeconds == 60) { onReauthSecondsChange(60) }
                        ReauthChip(label = "5分钟", selected = reauthSeconds == 300) { onReauthSecondsChange(300) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(0.82f)) {
                            Text("自动备份", style = MaterialTheme.typography.titleMedium)
                            Text(
                                autoBackupSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = onAutoBackupSwitchChange
                        )
                    }

                    OutlinedButton(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) {
                        Text("导出数据备份 (ZIP)")
                    }
                    OutlinedButton(onClick = onImportBackup, modifier = Modifier.fillMaxWidth()) {
                        Text("导入数据备份 (ZIP)")
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatDate(selectedPdfDateMillis),
                            onValueChange = {},
                            label = { Text("PDF导出日期") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Rounded.EditCalendar, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showPdfDatePicker = true }
                        )
                    }
                    OutlinedButton(
                        onClick = { onExportPdf(selectedPdfDateMillis) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("导出当日PDF")
                    }

                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清空全部数据")
                    }

                    Text("高德天气Key", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (amapKeyConfigured) "已配置（加密保存）" else "未配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = amapKeyInput,
                        onValueChange = { amapKeyInput = it },
                        label = { Text("输入高德 Web服务 Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            if (amapKeyInput.isNotBlank()) {
                                onSaveAmapKey(amapKeyInput.trim())
                                amapKeyInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存高德Key")
                    }

                    Text("和风历史天气Key", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (qWeatherKeyConfigured) "已配置（加密保存）" else "未配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = qWeatherKeyInput,
                        onValueChange = { qWeatherKeyInput = it },
                        label = { Text("输入和风 API Key（历史天气）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            if (qWeatherKeyInput.isNotBlank()) {
                                onSaveQWeatherKey(qWeatherKeyInput.trim())
                                qWeatherKeyInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存和风Key")
                    }

                    Text("和风 API Host", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (qWeatherHostConfigured) "已配置（明文保存）" else "未配置（将使用默认Host）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = qWeatherHostInput,
                        onValueChange = { qWeatherHostInput = it },
                        label = { Text("输入API Host，如 abc.qweatherapi.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            if (qWeatherHostInput.isNotBlank()) {
                                onSaveQWeatherHost(qWeatherHostInput.trim())
                                qWeatherHostInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存和风Host")
                    }
                }
            }
        }
    }

    if (showPdfDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedPdfDateMillis)
        DatePickerDialog(
            onDismissRequest = { showPdfDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedPdfDateMillis = it }
                        showPdfDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showPdfDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("确认清空全部数据") },
            text = { Text("该操作会删除所有日志与图片，且无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllData()
                        showClearConfirm = false
                    }
                ) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ReauthChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        enabled = true,
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    )
}
