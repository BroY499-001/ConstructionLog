package com.constructionlog.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.constructionlog.app.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockBar(
    visible: Boolean,
    showProjectManager: Boolean,
    showReminderPage: Boolean,
    onShowProjectManager: () -> Unit,
    onShowReminderPage: () -> Unit,
    onStartCreate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showQuickCreateMenu by remember { mutableStateOf(false) }
    val dockActiveIndex = when {
        showProjectManager -> 0
        showReminderPage -> 1
        else -> -1
    }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxWidth(),
        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 2 },
            animationSpec = tween(220, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(140)) + slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(220, easing = FastOutSlowInEasing)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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
                                painter = painterResource(R.drawable.ic_project),
                                contentDescription = "项目管理",
                                active = dockActiveIndex == 0,
                                onClick = onShowProjectManager
                            )
                        }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            DockIconButton(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = "提醒",
                                active = dockActiveIndex == 1,
                                onClick = onShowReminderPage
                            )
                        }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Box {
                                DockIconButton(
                                    painter = painterResource(R.drawable.ic_logbook),
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
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = "设置",
                                active = dockActiveIndex == 3,
                                onClick = onOpenSettings
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockIconButton(
    painter: Painter,
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
                painter = painter,
                contentDescription = contentDescription,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
