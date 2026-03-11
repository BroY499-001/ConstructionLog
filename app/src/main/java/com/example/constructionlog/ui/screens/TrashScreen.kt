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
import com.constructionlog.app.ui.components.TrashList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    items: List<LogWithImages>,
    onRestore: (Long) -> Unit,
    onDeleteForever: (Long) -> Unit,
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
                title = { Text("回收站", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    TextButton(onClick = onBack) { Text("日志") }
                }
            )
        }
    ) { padding ->
        TrashList(
            items = items,
            onRestore = onRestore,
            onDeleteForever = onDeleteForever,
            modifier = Modifier.padding(padding)
        )
    }
}
