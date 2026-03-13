package com.constructionlog.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import com.constructionlog.app.data.LogWithImages
import com.constructionlog.app.data.PlanTaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.min

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarHome(
    logs: List<LogWithImages>,
    planTasks: List<PlanTaskEntity>,
    onEdit: (LogWithImages) -> Unit,
    onDelete: (Long) -> Unit,
    onAddPlanTask: (title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onUpdatePlanTask: (id: Long, title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onTogglePlanTask: (id: Long, done: Boolean) -> Unit,
    onDeletePlanTask: (Long) -> Unit,
    onOpenAcceptanceFromPlan: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var monthExpanded by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var pendingDeleteLog by remember { mutableStateOf<LogWithImages?>(null) }
    var pickerYear by remember { mutableStateOf(currentMonth.year) }
    val listState = rememberLazyListState()
    var pullDelta by remember { mutableStateOf(0f) }
    val pullExpandConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            val atTop = !listState.canScrollBackward
            if (available.y > 0 && !monthExpanded && atTop) {
                // 列表在顶部，下拉展开月视图
                pullDelta += available.y
                if (pullDelta > 72f) {
                    monthExpanded = true
                    pullDelta = 0f
                }
                return available.copy(x = 0f)
            } else if (available.y < 0 && monthExpanded) {
                // 上划收起月视图
                pullDelta += available.y
                if (pullDelta < -72f) {
                    monthExpanded = false
                    pullDelta = 0f
                }
            } else if ((available.y > 0 && !atTop) || (available.y < 0 && !monthExpanded)) {
                pullDelta = 0f
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            pullDelta = 0f
            return Velocity.Zero
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
    val stageTimeline = remember(logs) { buildStageTimeline(logs) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullExpandConnection),
        state = listState,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 90.dp),
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
            StageTimelineCard(stageTimeline = stageTimeline)
        }

        item {
            PlanTasksCard(
                planTasks = planTasks.take(3),
                onAddPlanTask = onAddPlanTask,
                onUpdatePlanTask = onUpdatePlanTask,
                onTogglePlanTask = onTogglePlanTask,
                onDeletePlanTask = onDeletePlanTask,
                onOpenAcceptanceFromPlan = onOpenAcceptanceFromPlan,
                showHeaderAction = true
            )
        }

        item {
            Text(
                text = "${selectedDate.format(dayFormatter)} · ${selectedLogs.size} 条日志 · ${selectedLogs.sumOf { it.images.size }} 张图片",
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
            LogCard(
                item = item,
                onEdit = onEdit,
                onDelete = { pendingDeleteLog = it }
            )
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

    pendingDeleteLog?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteLog = null },
            title = { Text("删除这条日志？") },
            text = { Text("删除后会先移到回收站，可在 30 天内恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target.log.id)
                        pendingDeleteLog = null
                    }
                ) { Text("确认删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLog = null }) { Text("取消") }
            }
        )
    }
}

private data class StageTimelineItem(
    val stage: String,
    val start: LocalDate,
    val end: LocalDate,
    val count: Int,
    val startRatio: Float,
    val widthRatio: Float
)

@Composable
private fun StageTimelineCard(stageTimeline: List<StageTimelineItem>) {
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("施工阶段时间线（甘特）", style = MaterialTheme.typography.titleMedium)
            if (stageTimeline.isEmpty()) {
                Text(
                    "暂无日志，无法生成时间线",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
            stageTimeline.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.stage, style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${item.start.format(dayFormatter)} ~ ${item.end.format(dayFormatter)} · ${item.count} 条",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val startWeight = item.startRatio.coerceAtLeast(0f)
                        if (startWeight > 0f) {
                            Spacer(modifier = Modifier.weight(startWeight))
                        }
                        Box(
                            modifier = Modifier
                                .weight(item.widthRatio.coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.82f))
                        )
                        val endWeight = (1f - item.startRatio - item.widthRatio).coerceAtLeast(0f)
                        if (endWeight > 0f) {
                            Spacer(modifier = Modifier.weight(endWeight))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanTasksCard(
    planTasks: List<PlanTaskEntity>,
    onAddPlanTask: (title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onUpdatePlanTask: (id: Long, title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onTogglePlanTask: (id: Long, done: Boolean) -> Unit,
    onDeletePlanTask: (Long) -> Unit,
    onOpenAcceptanceFromPlan: (Long) -> Unit,
    showHeaderAction: Boolean
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<PlanTaskEntity?>(null) }
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
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
                Text("提醒与计划", style = MaterialTheme.typography.titleMedium)
                if (showHeaderAction) {
                    TextButton(onClick = { showAddDialog = true }) { Text("新建计划") }
                }
            }
            if (planTasks.isEmpty()) {
                Text(
                    "暂无计划项，建议先加验收、复检、到货提醒。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                planTasks.take(6).forEach { task ->
                    val linkedAcceptanceId = task.acceptanceFormId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .let { base ->
                                    if (linkedAcceptanceId != null) {
                                        base.clickable { onOpenAcceptanceFromPlan(linkedAcceptanceId) }
                                    } else {
                                        base
                                    }
                                },
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (task.done) "✓ ${task.title}" else task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = buildString {
                                    append("优先级${task.priority}")
                                    task.dueAt?.let { append(" · 截止 ${formatDate(it)}") }
                                    if (linkedAcceptanceId != null) append(" · 关联验收")
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            if (linkedAcceptanceId != null) {
                                TextButton(onClick = { onOpenAcceptanceFromPlan(linkedAcceptanceId) }) { Text("查看验收") }
                            }
                            TextButton(onClick = { onTogglePlanTask(task.id, !task.done) }) {
                                Text(if (task.done) "重开" else "完成")
                            }
                            TextButton(onClick = { editingTask = task }) { Text("编辑") }
                            TextButton(onClick = { onDeletePlanTask(task.id) }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ReminderEditDialog(
            titleText = "新建提醒",
            initialTitle = "",
            initialDetail = "",
            initialDueAt = null,
            initialPriority = 2,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, detail, dueAt, priority ->
                onAddPlanTask(title, detail, dueAt, priority)
                showAddDialog = false
            }
        )
    }

    if (editingTask != null) {
        val target = editingTask ?: return
        ReminderEditDialog(
            titleText = "编辑提醒",
            initialTitle = target.title,
            initialDetail = target.detail,
            initialDueAt = target.dueAt,
            initialPriority = target.priority,
            onDismiss = { editingTask = null },
            onConfirm = { title, detail, dueAt, priority ->
                onUpdatePlanTask(target.id, title, detail, dueAt, priority)
                editingTask = null
            }
        )
    }
}

@Composable
fun ReminderListPage(
    tasks: List<PlanTaskEntity>,
    onAddPlanTask: (title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onUpdatePlanTask: (id: Long, title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onTogglePlanTask: (id: Long, done: Boolean) -> Unit,
    onDeletePlanTask: (Long) -> Unit,
    onOpenAcceptanceFromPlan: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PlanTasksCard(
                planTasks = tasks,
                onAddPlanTask = onAddPlanTask,
                onUpdatePlanTask = onUpdatePlanTask,
                onTogglePlanTask = onTogglePlanTask,
                onDeletePlanTask = onDeletePlanTask,
                onOpenAcceptanceFromPlan = onOpenAcceptanceFromPlan,
                showHeaderAction = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditDialog(
    titleText: String,
    initialTitle: String,
    initialDetail: String,
    initialDueAt: Long?,
    initialPriority: Int,
    onDismiss: () -> Unit,
    onConfirm: (title: String, detail: String, dueAt: Long, priority: Int) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var detail by remember { mutableStateOf(initialDetail) }
    var priority by remember { mutableStateOf(initialPriority.coerceIn(1, 3)) }
    var dueAt by remember { mutableStateOf(initialDueAt ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, singleLine = true)
                OutlinedTextField(value = detail, onValueChange = { detail = it }, label = { Text("说明（可选）") })
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("提醒日期：${formatDate(dueAt)}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { value ->
                        AssistChip(
                            onClick = { priority = value },
                            label = { Text("P$value") },
                            border = if (priority == value) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), detail.trim(), dueAt, priority) },
                enabled = title.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueAt = pickerState.selectedDateMillis ?: dueAt
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun buildStageTimeline(logs: List<LogWithImages>): List<StageTimelineItem> {
    if (logs.isEmpty()) return emptyList()

    val stageRanges = mutableMapOf<String, Triple<LocalDate, LocalDate, Int>>()
    logs.forEach { item ->
        val date = Instant.ofEpochMilli(item.log.date).atZone(ZoneId.systemDefault()).toLocalDate()
        val stage = item.log.stage.ifBlank { estimateStageFromContent(item.log.content) }
        val current = stageRanges[stage]
        stageRanges[stage] = if (current == null) {
            Triple(date, date, 1)
        } else {
            Triple(
                minOf(current.first, date),
                maxOf(current.second, date),
                current.third + 1
            )
        }
    }

    val projectStart = stageRanges.values.minOf { it.first }
    val projectEnd = stageRanges.values.maxOf { it.second }
    val totalDays = (java.time.temporal.ChronoUnit.DAYS.between(projectStart, projectEnd) + 1).toFloat().coerceAtLeast(1f)

    return stageRanges.entries
        .sortedBy { stageOrderIndex(it.key) }
        .map { (stage, range) ->
            val startOffset = java.time.temporal.ChronoUnit.DAYS.between(projectStart, range.first).toFloat()
            val widthDays = (java.time.temporal.ChronoUnit.DAYS.between(range.first, range.second) + 1).toFloat()
            StageTimelineItem(
                stage = stage,
                start = range.first,
                end = range.second,
                count = range.third,
                startRatio = startOffset / totalDays,
                widthRatio = widthDays / totalDays
            )
        }
}

private fun stageOrderIndex(stage: String): Int {
    val ordered = listOf("开工准备", "拆改阶段", "水电阶段", "泥瓦阶段", "木工阶段", "油工阶段", "安装阶段", "收尾验收")
    val index = ordered.indexOf(stage)
    return if (index >= 0) index else ordered.size + stage.hashCode().ushr(1)
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
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
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
                                .aspectRatio(1f),
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
                                    .padding(horizontal = 5.dp, vertical = 5.dp),
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
                                        val markerColor = if (date == selectedDate) {
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                                        } else {
                                            Color(0xFFC2410C)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .graphicsLayer {
                                                    scaleX = dotScale
                                                    scaleY = dotScale
                                                }
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(markerColor)
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
