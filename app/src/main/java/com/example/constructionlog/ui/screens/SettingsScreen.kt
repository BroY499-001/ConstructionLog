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
import com.constructionlog.app.ui.components.SettingsPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authEnabled: Boolean,
    reauthSeconds: Int,
    autoBackupEnabled: Boolean,
    autoBackupSummary: String,
    onAuthEnabledChange: (Boolean) -> Unit,
    onReauthSecondsChange: (Int) -> Unit,
    onAutoBackupEnabledChange: (Boolean) -> Unit,
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
                title = { Text("设置", style = MaterialTheme.typography.headlineSmall) },
                actions = { TextButton(onClick = onBack) { Text("完成") } }
            )
        }
    ) { padding ->
        SettingsPage(
            authEnabled = authEnabled,
            reauthSeconds = reauthSeconds,
            autoBackupEnabled = autoBackupEnabled,
            autoBackupSummary = autoBackupSummary,
            onBack = onBack,
            onAuthSwitchChange = onAuthEnabledChange,
            onReauthSecondsChange = onReauthSecondsChange,
            onAutoBackupSwitchChange = onAutoBackupEnabledChange,
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
    }
}
