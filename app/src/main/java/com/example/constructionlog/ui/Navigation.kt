package com.constructionlog.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.constructionlog.app.data.LogWithImages
import com.constructionlog.app.data.PlanTaskEntity
import com.constructionlog.app.data.ProjectEntity
import com.constructionlog.app.ui.screens.CalendarScreen
import com.constructionlog.app.ui.screens.AcceptanceEditorScreen
import com.constructionlog.app.ui.screens.AcceptanceHomeScreen
import com.constructionlog.app.ui.screens.EditorScreen
import com.constructionlog.app.ui.screens.HomeScreen
import com.constructionlog.app.ui.screens.SettingsScreen
import com.constructionlog.app.ui.screens.TrashScreen
import com.constructionlog.app.ui.screens.OnboardingScreen

object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Editor = "editor"
    const val Trash = "trash"
    const val Calendar = "calendar"
    const val Settings = "settings"
    const val AcceptanceHome = "acceptance_home"
    const val AcceptanceEditor = "acceptance_editor"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    logs: List<LogWithImages>,
    trash: List<LogWithImages>,
    editorState: com.constructionlog.app.ui.EditorState,
    acceptanceForms: List<com.constructionlog.app.data.AcceptanceFormWithDetails>,
    acceptanceEditorState: com.constructionlog.app.ui.AcceptanceEditorState,
    projects: List<ProjectEntity>,
    planTasks: List<PlanTaskEntity>,
    projectsLoaded: Boolean,
    selectedProjectId: Long?,
    onShowList: () -> Unit,
    onShowTrash: () -> Unit,
    onStartCreate: () -> Unit,
    onStartEdit: (LogWithImages) -> Unit,
    onStartCreateAcceptance: () -> Unit,
    onStartEditAcceptance: (com.constructionlog.app.data.AcceptanceFormWithDetails) -> Unit,
    onStartEditAcceptanceById: (Long, (Boolean) -> Unit) -> Unit,
    onSelectProject: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRestore: (Long) -> Unit,
    onDeleteForever: (Long) -> Unit,
    onEditorProjectChange: (Long) -> Unit,
    onAcceptanceProjectChange: (Long) -> Unit,
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
    onAcceptanceTypeChange: (String) -> Unit,
    onAcceptanceStageChange: (String) -> Unit,
    onAcceptanceDateChange: (Long) -> Unit,
    onAcceptanceWeatherChange: (String) -> Unit,
    onAcceptanceLocationChange: (String) -> Unit,
    onAcceptanceInspectorChange: (String) -> Unit,
    onAcceptanceRemarkChange: (String) -> Unit,
    onAcceptanceUpdateItem: (Int, com.constructionlog.app.ui.AcceptanceItemDraft) -> Unit,
    onAcceptanceAddItemImageFromCamera: (Int) -> Unit,
    onAcceptanceAddItemImageFromGallery: (Int) -> Unit,
    onAcceptanceAddItem: () -> Unit,
    onAcceptanceRemoveItem: (Int) -> Unit,
    onAcceptanceRemoveImage: (String) -> Unit,
    onAcceptanceSave: () -> Unit,
    onAcceptanceDelete: (Long) -> Unit,
    onAutoFetchAcceptanceWeather: (Long) -> Unit,
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
    onAutoFetchWeather: (Long) -> Unit,
    onAddAcceptanceFromCamera: () -> Unit,
    onAddAcceptanceFromGallery: () -> Unit,
    onboardingCompleted: Boolean,
    onCompleteOnboarding: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
        enterTransition = { fadeIn(animationSpec = tween(400)) },
        exitTransition = { fadeOut(animationSpec = tween(400)) }
    ) {
        composable(Routes.Splash) {
            SplashScreen(navController, onboardingCompleted)
        }
        composable(
            Routes.Onboarding,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(500)) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(500)) + fadeOut() }
        ) {
            OnboardingScreen(
                onFinished = {
                    onCompleteOnboarding()
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        composable(
            Routes.Home,
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
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
                onOpenAcceptanceFromPlan = { formId ->
                    onStartEditAcceptanceById(formId) { ok ->
                        if (ok) {
                            navController.navigate(Routes.AcceptanceEditor)
                        }
                    }
                },
                onOpenAcceptance = { navController.navigate(Routes.AcceptanceHome) },
                onOpenTrash = {
                    onShowTrash()
                    navController.navigate(Routes.Trash)
                },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onImportBackup = onImportBackup
            )
        }
        composable(
            Routes.Editor,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut() }
        ) {
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
        composable(
            Routes.AcceptanceHome,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
        ) {
            AcceptanceHomeScreen(
                forms = acceptanceForms,
                projects = projects,
                selectedProjectId = selectedProjectId,
                onSelectProject = onSelectProject,
                onCreateWaterElectric = {
                    onStartCreateAcceptance()
                    navController.navigate(Routes.AcceptanceEditor)
                },
                onEdit = { item ->
                    onStartEditAcceptance(item)
                    navController.navigate(Routes.AcceptanceEditor)
                },
                onDelete = { item -> onAcceptanceDelete(item.form.id) },
                onBack = { navController.popBackStack(Routes.Home, false) }
            )
        }
        composable(
            Routes.AcceptanceEditor,
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) }
        ) {
            AcceptanceEditorScreen(
                projects = projects,
                state = acceptanceEditorState,
                onProjectChange = onAcceptanceProjectChange,
                onDateChange = onAcceptanceDateChange,
                onTypeChange = onAcceptanceTypeChange,
                onStageChange = onAcceptanceStageChange,
                onWeatherChange = onAcceptanceWeatherChange,
                onLocationChange = onAcceptanceLocationChange,
                onInspectorChange = onAcceptanceInspectorChange,
                onRemarkChange = onRemarkChange,
                onUpdateItem = onAcceptanceUpdateItem,
                onAddItemImageFromCamera = onAcceptanceAddItemImageFromCamera,
                onAddItemImageFromGallery = onAcceptanceAddItemImageFromGallery,
                onAddItem = onAcceptanceAddItem,
                onRemoveItem = onAcceptanceRemoveItem,
                onRemoveImage = onAcceptanceRemoveImage,
                onSave = {
                    onAcceptanceSave()
                    navController.popBackStack(Routes.AcceptanceHome, false)
                },
                onAddFromCamera = onAddAcceptanceFromCamera,
                onAddFromGallery = onAddAcceptanceFromGallery,
                onAutoFetchWeather = onAutoFetchAcceptanceWeather,
                onBack = { navController.popBackStack(Routes.AcceptanceHome, false) }
            )
        }
        composable(
            Routes.Trash,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
        ) {
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
        composable(
            Routes.Calendar,
            enterTransition = { fadeIn() + expandVertically() },
            exitTransition = { fadeOut() + shrinkVertically() }
        ) {
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
                onOpenAcceptanceFromPlan = { formId ->
                    onStartEditAcceptanceById(formId) { ok ->
                        if (ok) {
                            navController.navigate(Routes.AcceptanceEditor)
                        }
                    }
                },
                onBack = {
                    onShowList()
                    navController.popBackStack(Routes.Home, false)
                }
            )
        }
        composable(
            Routes.Settings,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
        ) {
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
