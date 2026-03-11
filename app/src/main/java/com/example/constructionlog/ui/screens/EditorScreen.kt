package com.constructionlog.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.constructionlog.app.data.ProjectEntity
import com.constructionlog.app.ui.EditorState
import com.constructionlog.app.ui.components.EditorForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
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
                title = {
                    Text(
                        text = if (state.editingId == null) "新建施工记录" else "编辑施工记录",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    TextButton(onClick = onBack) { Text("日志") }
                }
            )
        }
    ) { padding ->
        EditorForm(
            projects = projects,
            state = state,
            onProjectChange = onProjectChange,
            onDateChange = onDateChange,
            onWeatherChange = onWeatherChange,
            onLocationChange = onLocationChange,
            onContentChange = onContentChange,
            onWorkersChange = onWorkersChange,
            onWorkerNamesChange = onWorkerNamesChange,
            onSafetyChange = onSafetyChange,
            onStageChange = onStageChange,
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
