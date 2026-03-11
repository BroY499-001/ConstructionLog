package com.constructionlog.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.constructionlog.app.data.LogWithImages
import com.constructionlog.app.data.PlanTaskEntity
import com.constructionlog.app.data.ProjectEntity
import com.constructionlog.app.ui.screens.CalendarScreen
import com.constructionlog.app.ui.screens.EditorScreen
import com.constructionlog.app.ui.screens.HomeScreen
import com.constructionlog.app.ui.screens.SettingsScreen
import com.constructionlog.app.ui.screens.TrashScreen

object Routes {
    const val Splash = "splash"
    const val Home = "home"
    const val Editor = "editor"
    const val Trash = "trash"
    const val Calendar = "calendar"
    const val Settings = "settings"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    logs: List<LogWithImages>,
    trash: List<LogWithImages>,
    editorState: com.constructionlog.app.ui.EditorState,
    projects: List<ProjectEntity>,
    planTasks: List<PlanTaskEntity>,
    projectsLoaded: Boolean,
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
    onStageChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onRemoveImage: (String) -> Unit,
    onSave: () -> Unit,
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
    onAddPlanTask: (title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onUpdatePlanTask: (id: Long, title: String, detail: String, dueAt: Long?, priority: Int) -> Unit,
    onTogglePlanTask: (id: Long, done: Boolean) -> Unit,
    onDeletePlanTask: (Long) -> Unit,
    onAddFromCamera: () -> Unit,
    onAddFromGallery: () -> Unit,
    onAutoFetchWeather: (Long) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.Splash) {
            SplashScreen(navController)
        }
        composable(Routes.Home) {
            HomeScreen(
                logs = logs,
                planTasks = planTasks,
                projects = projects,
                projectsLoaded = projectsLoaded,
                selectedProjectId = selectedProjectId,
                onSelectProject = onSelectProject,
                onAddProject = onAddProject,
                onRenameProject = onRenameProject,
                onDeleteProject = onDeleteProject,
                onStartCreate = {
                    onStartCreate()
                    navController.navigate(Routes.Editor)
                },
                onStartEdit = { item ->
                    onStartEdit(item)
                    navController.navigate(Routes.Editor)
                },
                onDelete = onDelete,
                onAddPlanTask = onAddPlanTask,
                onUpdatePlanTask = onUpdatePlanTask,
                onTogglePlanTask = onTogglePlanTask,
                onDeletePlanTask = onDeletePlanTask,
                onOpenTrash = {
                    onShowTrash()
                    navController.navigate(Routes.Trash)
                },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onImportBackup = onImportBackup
            )
        }
        composable(Routes.Editor) {
            EditorScreen(
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
                onStageChange = onStageChange,
                onRemarkChange = onRemarkChange,
                onRemoveImage = onRemoveImage,
                onSave = {
                    onSave()
                    onShowList()
                    navController.popBackStack(Routes.Home, false)
                },
                onAddFromCamera = onAddFromCamera,
                onAddFromGallery = onAddFromGallery,
                onAutoFetchWeather = onAutoFetchWeather,
                onBack = {
                    onShowList()
                    navController.popBackStack(Routes.Home, false)
                }
            )
        }
        composable(Routes.Trash) {
            TrashScreen(
                items = trash,
                onRestore = onRestore,
                onDeleteForever = onDeleteForever,
                onBack = {
                    onShowList()
                    navController.popBackStack(Routes.Home, false)
                }
            )
        }
        composable(Routes.Calendar) {
            CalendarScreen(
                logs = logs,
                planTasks = planTasks,
                onEdit = { item ->
                    onStartEdit(item)
                    navController.navigate(Routes.Editor)
                },
                onDelete = onDelete,
                onAddPlanTask = onAddPlanTask,
                onUpdatePlanTask = onUpdatePlanTask,
                onTogglePlanTask = onTogglePlanTask,
                onDeletePlanTask = onDeletePlanTask,
                onBack = {
                    onShowList()
                    navController.popBackStack(Routes.Home, false)
                }
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                authEnabled = authEnabled,
                reauthSeconds = reauthSeconds,
                autoBackupEnabled = autoBackupEnabled,
                autoBackupSummary = autoBackupSummary,
                onAuthEnabledChange = onAuthEnabledChange,
                onReauthSecondsChange = onReauthSecondsChange,
                onAutoBackupEnabledChange = onAutoBackupEnabledChange,
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
                onBack = {
                    onShowList()
                    navController.popBackStack(Routes.Home, false)
                }
            )
        }
    }
}
