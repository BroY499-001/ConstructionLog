package com.example.constructionlog.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import coil.compose.AsyncImage
import com.example.constructionlog.data.LogWithImages
import com.example.constructionlog.data.ProjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年MM月")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppScreen(
    mode: ScreenMode,
    logs: List<LogWithImages>,
    trash: List<LogWithImages>,
    editorState: EditorState,
    projects: List<ProjectEntity>,
    selectedProjectId: Long?,
    onShowList: () -> Unit,
    onShowTrash: () -> Unit,
    onStartCreate: () -> Unit,
    onStartEdit: (LogWithImages) -> Unit,
    onSelectProject: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRestore: (Long) -> Unit,
    onDeleteForever: (Long) -> Unit,
    onEditorProjectChange: (Long) -> Unit,
    onAddProject: (String) -> Unit,
    onRenameProject: (Long, String) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onDateChange: (Long) -> Unit,
    onWeatherChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onWorkersChange: (String) -> Unit,
    onWorkerNamesChange: (String) -> Unit,
    onSafetyChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onRemoveImage: (String) -> Unit,
    onSave: () -> Unit,
    authEnabled: Boolean,
    reauthSeconds: Int,
    onAuthEnabledChange: (Boolean) -> Unit,
    onReauthSecondsChange: (Int) -> Unit,
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
    onAddFromCamera: () -> Unit,
    onAddFromGallery: () -> Unit,
    onAutoFetchWeather: (Long) -> Unit
) {
    var showSettingsPage by remember { mutableStateOf(false) }
    var showProjectManager by remember { mutableStateOf(false) }
    var showEnableConfirmDialog by remember { mutableStateOf(false) }
    var showQuickCreateMenu by remember { mutableStateOf(false) }
    var showProjectSwitchMenu by remember { mutableStateOf(false) }
    var requiredProjectName by remember { mutableStateOf("") }
    val selectedProjectName = projects.firstOrNull { it.id == selectedProjectId }?.name ?: "未选择项目"
    val shouldHandleBack = showSettingsPage || showProjectManager || showQuickCreateMenu || showProjectSwitchMenu || mode != ScreenMode.LIST

    BackHandler(enabled = shouldHandleBack) {
        when {
            showQuickCreateMenu -> showQuickCreateMenu = false
            showProjectSwitchMenu -> showProjectSwitchMenu = false
            showProjectManager -> showProjectManager = false
            showSettingsPage -> showSettingsPage = false
            mode != ScreenMode.LIST -> onShowList()
        }
    }

    val pageGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFE7F2EF),
            Color(0xFFF5EFE2),
            Color(0xFFF9F7F1)
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
                    title = {
                        if (showSettingsPage) {
                            Text(
                                text = "设置",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        } else {
                            Column {
                                Text(
                                    text = when (mode) {
                                        ScreenMode.LIST -> "施工日志"
                                        ScreenMode.EDITOR -> if (editorState.editingId == null) "新建施工记录" else "编辑施工记录"
                                        ScreenMode.TRASH -> "回收站"
                                    },
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                if (mode == ScreenMode.LIST) {
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
                        }
                    },
                    actions = {
                        if (showSettingsPage) {
                            TextButton(onClick = { showSettingsPage = false }) { Text("完成") }
                        } else if (mode != ScreenMode.LIST) {
                            TextButton(onClick = onShowList) { Text("日志") }
                        }
                    }
                )
            }
        ) { padding ->
            if (showSettingsPage) {
                SettingsPage(
                    authEnabled = authEnabled,
                    reauthSeconds = reauthSeconds,
                    onBack = { showSettingsPage = false },
                    onAuthSwitchChange = { enabled ->
                        if (enabled && !authEnabled) {
                            showEnableConfirmDialog = true
                        } else if (!enabled) {
                            onAuthEnabledChange(false)
                        }
                    },
                    onReauthSecondsChange = onReauthSecondsChange,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    onExportPdf = onExportPdf,
                    onClearAllData = onClearAllData,
                    amapKeyConfigured = amapKeyConfigured,
                    onSaveAmapKey = onSaveAmapKey,
                    qWeatherKeyConfigured = qWeatherKeyConfigured,
                    onSaveQWeatherKey = onSaveQWeatherKey,
                    qWeatherHostConfigured = qWeatherHostConfigured,
                    onSaveQWeatherHost = onSaveQWeatherHost,
                    modifier = Modifier.padding(padding)
                )
            } else {
                Crossfade(
                    targetState = mode,
                    label = "mode"
                ) { currentMode ->
                    when (currentMode) {
                        ScreenMode.LIST -> CalendarHome(
                            logs = logs,
                            onEdit = onStartEdit,
                            onDelete = onDelete,
                            modifier = Modifier.padding(padding)
                        )

                        ScreenMode.TRASH -> TrashList(
                            items = trash,
                            onRestore = onRestore,
                            onDeleteForever = onDeleteForever,
                            modifier = Modifier.padding(padding)
                        )

                        ScreenMode.EDITOR -> Editor(
                            projects = projects,
                            state = editorState,
                            onProjectChange = onEditorProjectChange,
                            onDateChange = onDateChange,
                            onWeatherChange = onWeatherChange,
                            onLocationChange = onLocationChange,
                            onContentChange = onContentChange,
                            onWorkersChange = onWorkersChange,
                            onWorkerNamesChange = onWorkerNamesChange,
                            onSafetyChange = onSafetyChange,
                            onRemarkChange = onRemarkChange,
                            onRemoveImage = onRemoveImage,
                            onSave = onSave,
                            onAddFromCamera = onAddFromCamera,
                            onAddFromGallery = onAddFromGallery,
                            onAutoFetchWeather = onAutoFetchWeather,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }

        val showDock = mode == ScreenMode.LIST && !showSettingsPage && !showProjectManager
        AnimatedVisibility(
            visible = showDock,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 2 },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(140)) + slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
        ) {
            val dockActiveIndex = when {
                showProjectManager -> 0
                mode == ScreenMode.TRASH -> 1
                mode == ScreenMode.EDITOR -> 2
                else -> -1
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .navigationBarsPadding()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                DockIconButton(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = "项目管理",
                                    active = dockActiveIndex == 0,
                                    onClick = { showProjectManager = true }
                                )
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                DockIconButton(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "回收站",
                                    active = dockActiveIndex == 1,
                                    onClick = onShowTrash
                                )
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Box {
                                    DockIconButton(
                                        imageVector = Icons.Rounded.EditCalendar,
                                        contentDescription = "新增",
                                        active = dockActiveIndex == 2,
                                        onClick = onStartCreate,
                                        modifier = Modifier.combinedClickable(
                                            onClick = onStartCreate,
                                            onLongClick = { showQuickCreateMenu = true }
                                        )
                                    )
                                    DropdownMenu(
                                        expanded = showQuickCreateMenu,
                                        onDismissRequest = { showQuickCreateMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("纯文字记录") },
                                            onClick = {
                                                showQuickCreateMenu = false
                                                onStartCreate()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("拍照后记录") },
                                            onClick = {
                                                showQuickCreateMenu = false
                                                onStartCreate()
                                            }
                                        )
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                DockIconButton(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "设置",
                                    active = dockActiveIndex == 3,
                                    onClick = { showSettingsPage = true }
                                )
                            }
                        }
                    }
                }
            }
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

        if (showEnableConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showEnableConfirmDialog = false },
                title = { Text("开启启动验证") },
                text = {
                    Text("开启后，App 冷启动或切到后台超过 5 分钟时，都需要验证指纹或锁屏密码。验证取消或失败将退出应用。")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onAuthEnabledChange(true)
                            showEnableConfirmDialog = false
                        }
                    ) {
                        Text("确认开启")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEnableConfirmDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (projects.isEmpty()) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("先创建项目") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("当前没有项目，需先创建一个项目才能开始记录施工日志。")
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
                    TextButton(onClick = onImportBackup) {
                        Text("导入旧数据")
                    }
                }
            )
        }
    }
}

@Composable
private fun DockIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.92f
            active -> 1.06f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dock-scale"
    )
    Surface(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(16.dp),
        color = if (active) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (active) 0.32f else 0.2f)),
        shadowElevation = if (active) 8.dp else 0.dp
    ) {
        IconButton(onClick = onClick, interactionSource = interactionSource) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsPage(
    authEnabled: Boolean,
    reauthSeconds: Int,
    onBack: () -> Unit,
    onAuthSwitchChange: (Boolean) -> Unit,
    onReauthSecondsChange: (Int) -> Unit,
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
    var pdfDateText by remember { mutableStateOf(formatDate(System.currentTimeMillis())) }
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("设置", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onBack) {
                            Text("返回")
                        }
                    }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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

                OutlinedButton(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) {
                    Text("导出数据备份 (ZIP)")
                }
                OutlinedButton(onClick = onImportBackup, modifier = Modifier.fillMaxWidth()) {
                    Text("导入数据备份 (ZIP)")
                }

                OutlinedTextField(
                    value = pdfDateText,
                    onValueChange = { pdfDateText = it },
                    label = { Text("PDF日期 (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        parseDate(pdfDateText)?.let(onExportPdf)
                    },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarHome(
    logs: List<LogWithImages>,
    onEdit: (LogWithImages) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var monthExpanded by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableStateOf(currentMonth.year) }
    val listState = rememberLazyListState()
    var pullDelta by remember { mutableStateOf(0f) }
    val pullExpandConnection = remember(monthExpanded) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y > 0 && !monthExpanded) {
                    pullDelta += available.y
                    if (pullDelta > 72f) {
                        monthExpanded = true
                        pullDelta = 0f
                    }
                } else if (available.y < 0 && monthExpanded) {
                    pullDelta += available.y
                    if (pullDelta < -72f) {
                        monthExpanded = false
                        pullDelta = 0f
                    }
                } else {
                    pullDelta = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pullDelta = 0f
                return Velocity.Zero
            }
        }
    }

    val dateStats = remember(logs) {
        logs.groupBy {
            Instant.ofEpochMilli(it.log.date).atZone(ZoneId.systemDefault()).toLocalDate()
        }.mapValues { (_, items) ->
            DayStat(logCount = items.size, imageCount = items.sumOf { it.images.size })
        }
    }

    val selectedLogs = remember(logs, selectedDate) {
        logs.filter {
            Instant.ofEpochMilli(it.log.date).atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
        }
    }
    val monthLogs = remember(logs, currentMonth) {
        logs.filter {
            YearMonth.from(Instant.ofEpochMilli(it.log.date).atZone(ZoneId.systemDefault()).toLocalDate()) == currentMonth
        }
    }
    val monthImageCount = remember(monthLogs) { monthLogs.sumOf { it.images.size } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullExpandConnection),
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "目前为离线模式，数据均存在本地，只有获取天气时需要联网",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("智能日历", style = MaterialTheme.typography.titleLarge)
                        if (monthExpanded) {
                            Text(
                                text = "本月 ${monthLogs.size} 条 · ${monthImageCount} 图",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MonthIconButton(
                            icon = Icons.Rounded.ChevronLeft,
                            onClick = {
                                selectedDate = selectedDate.minusWeeks(1)
                                currentMonth = YearMonth.from(selectedDate)
                            }
                        )
                        Text(
                            text = "${currentMonth.format(monthFormatter)} ▾",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                pickerYear = currentMonth.year
                                showMonthPicker = true
                            }
                        )
                        MonthIconButton(
                            icon = Icons.Rounded.ChevronRight,
                            onClick = {
                                selectedDate = selectedDate.plusWeeks(1)
                                currentMonth = YearMonth.from(selectedDate)
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (monthExpanded) "上滑收起周视图" else "下拉展开月视图",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MonthNavButton(label = "今天", onClick = {
                            val today = LocalDate.now()
                            selectedDate = today
                            currentMonth = YearMonth.from(today)
                        })
                    }

                    CalendarGrid(
                        currentMonth = currentMonth,
                        selectedDate = selectedDate,
                        monthExpanded = monthExpanded,
                        stats = dateStats,
                        onSelectDate = {
                            selectedDate = it
                            currentMonth = YearMonth.from(it)
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = "${selectedDate.format(formatter)} · ${selectedLogs.size} 条日志 · ${selectedLogs.sumOf { it.images.size }} 张图片",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (selectedLogs.isEmpty()) {
            item {
                ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("当日暂无日志", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "可切换日期查看，或点击底部 + 号新建",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            return@LazyColumn
        }

        items(selectedLogs, key = { it.log.id }) { item ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(item) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatDate(item.log.date)}  ${item.log.weather.ifBlank { "天气未知" }}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            MetricChip(label = "${item.images.size} 图")
                        }

                        MetricChip(label = "阶段：${item.log.safety.ifBlank { "施工记录" }}")
                        Text(
                            text = item.log.content,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (item.images.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                maxItemsInEachRow = 3
                            ) {
                                item.images.take(9).forEach { image ->
                                    AsyncImage(
                                        model = Uri.parse(image.imageUri),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(84.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text("选择月份") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MonthIconButton(
                            icon = Icons.Rounded.ChevronLeft,
                            onClick = { pickerYear -= 1 }
                        )
                        Text(
                            text = "${pickerYear}年",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        MonthIconButton(
                            icon = Icons.Rounded.ChevronRight,
                            onClick = { pickerYear += 1 }
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        (1..12).forEach { month ->
                            val targetMonth = YearMonth.of(pickerYear, month)
                            val isSelected = targetMonth == currentMonth
                            AssistChip(
                                onClick = {
                                    currentMonth = targetMonth
                                    val day = min(selectedDate.dayOfMonth, targetMonth.lengthOfMonth())
                                    selectedDate = targetMonth.atDay(day)
                                    showMonthPicker = false
                                },
                                label = { Text("${month}月") },
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text("完成")
                }
            }
        )
    }
}

private data class DayStat(val logCount: Int, val imageCount: Int)

@Composable
private fun MonthNavButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MonthIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProjectManagerDialog(
    projects: List<ProjectEntity>,
    selectedProjectId: Long?,
    onDismiss: () -> Unit,
    onSelectProject: (Long) -> Unit,
    onAddProject: (String) -> Unit,
    onRenameProject: (Long, String) -> Unit,
    onDeleteProject: (Long) -> Unit
) {
    var newProjectName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ProjectEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("项目管理") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("新项目名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            onAddProject(newProjectName.trim())
                            newProjectName = ""
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("新增项目") }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(240.dp)) {
                    items(projects, key = { it.id }) { project ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (project.id == selectedProjectId) "${project.name}（当前）" else project.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onSelectProject(project.id) }) { Text("切换") }
                                TextButton(onClick = {
                                    renameTarget = project
                                    renameInput = project.name
                                }) { Text("重命名") }
                                TextButton(
                                    onClick = { onDeleteProject(project.id) },
                                    enabled = projects.size > 1
                                ) { Text("删除") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名项目") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("项目名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = renameTarget
                    if (target != null && renameInput.isNotBlank()) {
                        onRenameProject(target.id, renameInput.trim())
                        renameTarget = null
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    monthExpanded: Boolean,
    stats: Map<LocalDate, DayStat>,
    onSelectDate: (LocalDate) -> Unit
) {
    val dotTransition = rememberInfiniteTransition(label = "calendar-dot")
    val dotScale by dotTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.72f at 0
                1.18f at 380 using FastOutSlowInEasing
                1f at 700
                0.72f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "calendar-dot-scale"
    )
    val firstDay = currentMonth.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.value - 1
    val daysInMonth = currentMonth.lengthOfMonth()
    val prevMonth = currentMonth.minusMonths(1)
    val nextMonth = currentMonth.plusMonths(1)
    val prevMonthDays = prevMonth.lengthOfMonth()
    val cells = mutableListOf<LocalDate>()
    for (i in leadingBlanks downTo 1) {
        cells += prevMonth.atDay(prevMonthDays - i + 1)
    }
    for (day in 1..daysInMonth) {
        cells += currentMonth.atDay(day)
    }
    var nextDay = 1
    while (cells.size % 7 != 0) {
        cells += nextMonth.atDay(nextDay)
        nextDay += 1
    }
    val selectedWeekIndex = cells.chunked(7).indexOfFirst { week -> week.contains(selectedDate) }.coerceAtLeast(0)
    val visibleWeeks = if (monthExpanded) cells.chunked(7) else listOf(cells.chunked(7)[selectedWeekIndex])

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = 6.dp
        val cellWidth = (maxWidth - gap * 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { index, weekday ->
                    Box(modifier = Modifier.width(cellWidth), contentAlignment = Alignment.Center) {
                        val color = when (index) {
                            5 -> Color(0xFF2563EB)
                            6 -> Color(0xFFDC2626)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(weekday, style = MaterialTheme.typography.labelMedium, color = color)
                    }
                }
            }

            visibleWeeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    week.forEachIndexed { dayIndex, date ->
                        val inCurrentMonth = date.month == currentMonth.month && date.year == currentMonth.year
                        val today = LocalDate.now()
                        Surface(
                            modifier = Modifier
                                .width(cellWidth)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (date == selectedDate) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            border = if (date == today && date != selectedDate) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onSelectDate(date) }
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                val baseColor = when {
                                    dayIndex == 5 -> Color(0xFF2563EB)
                                    dayIndex == 6 -> Color(0xFFDC2626)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                val dayColor = when {
                                    date == selectedDate -> MaterialTheme.colorScheme.onPrimary
                                    inCurrentMonth -> baseColor
                                    else -> baseColor.copy(alpha = 0.35f)
                                }
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = dayColor
                                )
                                stats[date]?.let { stat ->
                                    if (stat.logCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .graphicsLayer {
                                                    scaleX = dotScale
                                                    scaleY = dotScale
                                                }
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0xFF2BA471))
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(6.dp))
                                    }
                                } ?: Spacer(modifier = Modifier.size(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashList(
    items: List<LogWithImages>,
    onRestore: (Long) -> Unit,
    onDeleteForever: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Empty("回收站为空", "删除后的日志会在这里保留 30 天")
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.log.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(formatDate(item.log.date), style = MaterialTheme.typography.titleMedium)
                    Text(item.log.location, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        item.log.content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRestore(item.log.id) }) {
                            Icon(Icons.Rounded.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("恢复")
                        }
                        OutlinedButton(onClick = { onDeleteForever(item.log.id) }) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("彻底删除")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Editor(
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
    onRemarkChange: (String) -> Unit,
    onRemoveImage: (String) -> Unit,
    onSave: () -> Unit,
    onAddFromCamera: () -> Unit,
    onAddFromGallery: () -> Unit,
    onAutoFetchWeather: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var previewImageIndex by remember { mutableStateOf<Int?>(null) }

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
                        OutlinedTextField(
                            value = projects.firstOrNull { it.id == state.projectId }?.name ?: "未选择项目",
                            onValueChange = {},
                            label = { Text("所属项目") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(projects, key = { it.id }) { project ->
                                AssistChip(
                                    onClick = { onProjectChange(project.id) },
                                    label = { Text(project.name) },
                                    border = if (project.id == state.projectId) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    } else null
                                )
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAddFromCamera) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("拍照")
                        }
                        OutlinedButton(onClick = onAddFromGallery) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("相册")
                        }
                    }
                    if (state.imageUris.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            maxItemsInEachRow = 3
                        ) {
                            state.imageUris.forEachIndexed { index, uri ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = Uri.parse(uri),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { previewImageIndex = index }
                                    )
                                    IconButton(onClick = { onRemoveImage(uri) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "删除图片")
                                    }
                                }
                            }
                        }
                    } else {
                        Text("还没有添加图片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    previewImageIndex?.let { index ->
        ZoomImageDialog(
            imageUris = state.imageUris,
            initialIndex = index,
            onDismiss = { previewImageIndex = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomImageDialog(imageUris: List<String>, initialIndex: Int, onDismiss: () -> Unit) {
    if (imageUris.isEmpty()) return
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "image_scale"
    )
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, imageUris.lastIndex),
        pageCount = { imageUris.size }
    )
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1115))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() },
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val currentUri = imageUris[pagerState.currentPage]
            AsyncImage(
                model = Uri.parse(currentUri),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp)
                    .graphicsLayer(alpha = 0.55f),
                contentScale = ContentScale.Crop
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = 56.dp, start = 8.dp, end = 8.dp)
            ) { page ->
                val uri = imageUris[page]
                AsyncImage(
                    model = Uri.parse(uri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = if (page == pagerState.currentPage) animatedScale else 1f,
                            scaleY = if (page == pagerState.currentPage) animatedScale else 1f,
                            translationX = if (page == pagerState.currentPage) offsetX else 0f,
                            translationY = if (page == pagerState.currentPage) offsetY else 0f
                        ),
                    contentScale = ContentScale.Fit
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.46f),
                                Color.Black.copy(alpha = 0f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0f),
                                Color.Black.copy(alpha = 0.62f)
                            )
                        )
                    )
            )
            Text(
                text = "左右滑动切换 · 双击缩放 · 单击关闭",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 42.dp)
            )
        }
    }
}

@Composable
private fun MetricChip(label: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) }
    )
}

@Composable
private fun Empty(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(time: Long): String = formatter.format(
    Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
)

private fun parseDate(dateString: String): Long? = runCatching {
    formatter.parse(dateString, java.time.LocalDate::from)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()
