package com.example.constructionlog

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Process
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.constructionlog.data.AmapKeyStore
import com.example.constructionlog.data.AppSettings
import com.example.constructionlog.data.BackupService
import com.example.constructionlog.data.PdfExportService
import com.example.constructionlog.data.QWeatherKeyStore
import com.example.constructionlog.security.BiometricAuthenticator
import com.example.constructionlog.ui.AppScreen
import com.example.constructionlog.ui.MainViewModel
import com.example.constructionlog.ui.ScreenMode
import com.example.constructionlog.ui.theme.ConstructionLogTheme
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val appSettings: AppSettings by lazy { AppSettings(this) }
    private val amapKeyStore: AmapKeyStore by lazy { AmapKeyStore(this) }
    private val qWeatherKeyStore: QWeatherKeyStore by lazy { QWeatherKeyStore(this) }

    private var authenticated = false
    private var backgroundAt = 0L
    private var authGateVisible by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authGateVisible = appSettings.isAppAuthEnabled()

        setContent {
            val mode by viewModel.screenMode.collectAsStateWithLifecycle()
            val logs by viewModel.logs.collectAsStateWithLifecycle()
            val trash by viewModel.trash.collectAsStateWithLifecycle()
            val editor by viewModel.editorState.collectAsStateWithLifecycle()
            val projects by viewModel.projects.collectAsStateWithLifecycle()
            val selectedProject by viewModel.selectedProject.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val app = application as ConstructionLogApp
            val backupService = remember { BackupService(context) }
            val pdfExportService = remember { PdfExportService(context, app.repository) }

            var authEnabled by remember { mutableStateOf(appSettings.isAppAuthEnabled()) }
            var reauthSeconds by remember { mutableStateOf(appSettings.getReauthSeconds()) }
            var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
            var pendingPdfDateMillis by remember { mutableStateOf<Long?>(null) }
            var pendingWeatherDateMillis by remember { mutableStateOf<Long?>(null) }
            var attemptedAutoWeather by remember(mode, editor.editingId) { mutableStateOf(false) }
            var amapKeyConfigured by remember { mutableStateOf(amapKeyStore.hasApiKey()) }
            var qWeatherKeyConfigured by remember { mutableStateOf(qWeatherKeyStore.hasApiKey()) }
            var qWeatherHostConfigured by remember { mutableStateOf(qWeatherKeyStore.hasApiHost()) }

            val pickImagesLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)
            ) { uris ->
                if (uris.isNotEmpty()) {
                    uris.forEach { uri ->
                        runCatching {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                    }
                    runOnIo(
                        action = {
                            val localUris = persistImagesLocally(uris)
                            withContext(Dispatchers.Main) { viewModel.addImages(localUris) }
                        },
                        onSuccess = {},
                        onError = { toast("导入图片失败: ${it.message}") }
                    )
                }
            }
            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { success ->
                if (success) {
                    pendingCameraUri?.let { cameraUri ->
                        runOnIo(
                            action = {
                                val local = persistImageToLocal(cameraUri)
                                withContext(Dispatchers.Main) { viewModel.addImages(listOf(local)) }
                            },
                            onSuccess = {},
                            onError = { toast("保存照片失败: ${it.message}") }
                        )
                    }
                }
            }
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    val targetDate = pendingWeatherDateMillis ?: editor.date
                    fetchWeatherForDate(targetDate) { weather, source -> viewModel.updateWeatherAuto(weather, source) }
                } else {
                    toast("未授予定位权限，无法自动获取天气")
                }
            }

            val exportBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/zip")
            ) { uri ->
                if (uri != null) {
                    runOnIo(
                        action = { backupService.exportBackup(uri).getOrThrow() },
                        onSuccess = { toast("备份导出成功") },
                        onError = { toast("备份导出失败: ${it.message}") }
                    )
                }
            }

            val importBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    runOnIo(
                        action = {
                            app.database.close()
                            backupService.importBackup(uri).getOrThrow()
                        },
                        onSuccess = {
                            toast("导入完成，应用即将重启")
                            restartApp()
                        },
                        onError = { toast("备份导入失败: ${it.message}") }
                    )
                }
            }

            val exportPdfLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/pdf")
            ) { uri ->
                val day = pendingPdfDateMillis
                if (uri != null && day != null) {
                    val projectId = selectedProject?.id
                    if (projectId == null) {
                        toast("请先创建项目")
                        pendingPdfDateMillis = null
                        return@rememberLauncherForActivityResult
                    }
                    runOnIo(
                        action = { pdfExportService.exportDayPdf(uri, day, projectId).getOrThrow() },
                        onSuccess = { toast("PDF 导出成功") },
                        onError = { toast("PDF 导出失败: ${it.message}") }
                    )
                }
                pendingPdfDateMillis = null
            }

            fun tryAutoFetchWeather(targetDateMillis: Long) {
                pendingWeatherDateMillis = targetDateMillis
                val fineGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarseGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (fineGranted || coarseGranted) {
                    fetchWeatherForDate(targetDateMillis) { weather, source -> viewModel.updateWeatherAuto(weather, source) }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            LaunchedEffect(mode, editor.editingId, editor.weather) {
                if (mode == ScreenMode.EDITOR && editor.editingId == null && editor.weather.isBlank() && !attemptedAutoWeather) {
                    attemptedAutoWeather = true
                    tryAutoFetchWeather(editor.date)
                }
            }

            ConstructionLogTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (authGateVisible) Modifier.blur(16.dp) else Modifier)
                    ) {
                        AppScreen(
                            mode = mode,
                            logs = logs,
                            trash = trash,
                            editorState = editor,
                            projects = projects,
                            selectedProjectId = selectedProject?.id,
                            onShowList = viewModel::showList,
                            onShowTrash = viewModel::showTrash,
                            onStartCreate = viewModel::startCreate,
                            onStartEdit = viewModel::startEdit,
                            onSelectProject = viewModel::selectProject,
                            onDelete = viewModel::moveToTrash,
                            onRestore = viewModel::restore,
                            onDeleteForever = viewModel::deleteForever,
                            onEditorProjectChange = viewModel::updateProjectId,
                            onAddProject = { name ->
                                viewModel.addProject(name) { result ->
                                    result.onSuccess { toast("项目已创建") }
                                        .onFailure { toast("创建项目失败: ${it.message}") }
                                }
                            },
                            onRenameProject = { id, name ->
                                viewModel.renameProject(id, name) { result ->
                                    result.onSuccess { toast("项目已重命名") }
                                        .onFailure { toast("重命名失败: ${it.message}") }
                                }
                            },
                            onDeleteProject = { id ->
                                viewModel.deleteProject(id) { result ->
                                    result.onSuccess { toast("项目已删除") }
                                        .onFailure { toast("删除失败: ${it.message}") }
                                }
                            },
                            onDateChange = viewModel::updateDate,
                            onWeatherChange = viewModel::updateWeatherManual,
                            onLocationChange = viewModel::updateLocation,
                            onContentChange = viewModel::updateContent,
                            onWorkersChange = viewModel::updateWorkers,
                            onWorkerNamesChange = viewModel::updateWorkerNames,
                            onSafetyChange = viewModel::updateSafety,
                            onRemarkChange = viewModel::updateRemark,
                            onRemoveImage = viewModel::removeImage,
                            onSave = { viewModel.save {} },
                            authEnabled = authEnabled,
                            reauthSeconds = reauthSeconds,
                            onAuthEnabledChange = { enabled ->
                                appSettings.setAppAuthEnabled(enabled)
                                authEnabled = enabled
                                if (!enabled) {
                                    authenticated = true
                                    authGateVisible = false
                                } else {
                                    authenticated = false
                                    authGateVisible = true
                                }
                            },
                            onReauthSecondsChange = { seconds ->
                                appSettings.setReauthSeconds(seconds)
                                reauthSeconds = seconds
                            },
                            onExportBackup = {
                                exportBackupLauncher.launch("construction-log-backup-${System.currentTimeMillis()}.zip")
                            },
                            onImportBackup = {
                                importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            },
                            onExportPdf = { dayMillis ->
                                pendingPdfDateMillis = dayMillis
                                exportPdfLauncher.launch("construction-log-${dayMillis}.pdf")
                            },
                            onClearAllData = {
                                runOnIo(
                                    action = {
                                        viewModel.clearAllData()
                                        clearImageFiles()
                                    },
                                    onSuccess = { toast("数据已清空") },
                                    onError = { toast("清空失败: ${it.message}") }
                                )
                            },
                            amapKeyConfigured = amapKeyConfigured,
                            onSaveAmapKey = { key ->
                                runCatching {
                                    amapKeyStore.saveApiKey(key)
                                    amapKeyConfigured = true
                                    toast("高德天气 Key 已保存")
                                }.onFailure {
                                    toast("保存 Key 失败: ${it.message}")
                                }
                            },
                            qWeatherKeyConfigured = qWeatherKeyConfigured,
                            onSaveQWeatherKey = { key ->
                                runCatching {
                                    qWeatherKeyStore.saveApiKey(key)
                                    qWeatherKeyConfigured = true
                                    toast("和风天气 Key 已保存")
                                }.onFailure {
                                    toast("保存 Key 失败: ${it.message}")
                                }
                            },
                            qWeatherHostConfigured = qWeatherHostConfigured,
                            onSaveQWeatherHost = { host ->
                                runCatching {
                                    qWeatherKeyStore.saveApiHost(host)
                                    qWeatherHostConfigured = true
                                    toast("和风 API Host 已保存")
                                }.onFailure {
                                    toast("保存 Host 失败: ${it.message}")
                                }
                            },
                            onAddFromCamera = {
                                val dir = context.getExternalFilesDir("Pictures") ?: context.filesDir
                                val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                            onAddFromGallery = {
                                pickImagesLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onAutoFetchWeather = { dateMillis ->
                                tryAutoFetchWeather(dateMillis)
                            }
                        )
                    }

                    if (authGateVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 30.dp)
                                .background(Color.Black.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (!appSettings.isAppAuthEnabled()) {
            authenticated = true
            authGateVisible = false
            return
        }

        if (!authenticated || shouldReAuth()) {
            authGateVisible = true
            BiometricAuthenticator(this).authenticate(
                onSuccess = {
                    authenticated = true
                    authGateVisible = false
                },
                onFailure = {
                    finishAffinity()
                }
            )
        } else {
            authGateVisible = false
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundAt = System.currentTimeMillis()
        if (appSettings.isAppAuthEnabled()) {
            authGateVisible = true
        }
    }

    private fun shouldReAuth(): Boolean {
        if (backgroundAt == 0L) return false
        return System.currentTimeMillis() - backgroundAt > appSettings.getReauthSeconds() * 1000L
    }

    private fun runOnIo(action: suspend () -> Unit, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        lifecycleScope.launch {
            val error = withContext(Dispatchers.IO) {
                runCatching { action() }.exceptionOrNull()
            }
            if (error == null) {
                onSuccess()
            } else {
                onError(error)
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun clearImageFiles() {
        getExternalFilesDir("Pictures")?.listFiles()?.forEach { it.delete() }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        finishAffinity()
        Process.killProcess(Process.myPid())
    }

    private suspend fun persistImagesLocally(uris: List<Uri>): List<Uri> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            runCatching { persistImageToLocal(uri) }.getOrNull()
        }
    }

    private fun persistImageToLocal(sourceUri: Uri): Uri {
        val picturesDir = getExternalFilesDir("Pictures") ?: filesDir
        picturesDir.mkdirs()

        val existingPath = sourceUri.path
        if (sourceUri.scheme == ContentResolver.SCHEME_FILE && existingPath != null) {
            val existing = File(existingPath)
            if (existing.exists() && existing.absolutePath.startsWith(picturesDir.absolutePath)) {
                return Uri.fromFile(existing)
            }
        }

        val ext = guessImageExtension(sourceUri)
        val target = File(picturesDir, "IMG_${System.currentTimeMillis()}_${(0..9999).random()}.$ext")
        contentResolver.openInputStream(sourceUri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("无法读取图片")
        return Uri.fromFile(target)
    }

    private fun guessImageExtension(uri: Uri): String {
        val mime = contentResolver.getType(uri)
        val byMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        if (!byMime.isNullOrBlank()) return byMime

        val byName = runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    c.getString(0)?.substringAfterLast('.', "")
                } else {
                    null
                }
            }
        }.getOrNull()
        return if (byName.isNullOrBlank()) "jpg" else byName.lowercase(Locale.getDefault())
    }

    private fun fetchWeatherForDate(targetDateMillis: Long, onSuccess: (String, String) -> Unit) {
        runOnIo(
            action = {
                val amapKey = amapKeyStore.getApiKey()
                    ?: throw IllegalStateException("请先在设置中配置高德天气 Key")
                val location = getPreciseLocation() ?: throw IllegalStateException("未获取到当前位置")
                val qWeatherKey = qWeatherKeyStore.getApiKey()
                val qWeatherApiHost = qWeatherKeyStore.getApiHost()
                val (weather, provider) = queryWeatherWithPriority(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    amapKey = amapKey,
                    qWeatherKey = qWeatherKey,
                    qWeatherApiHost = qWeatherApiHost,
                    targetDateMillis = targetDateMillis
                )
                val source = "日期:${DATE_FORMATTER.format(Instant.ofEpochMilli(targetDateMillis).atZone(ZoneId.systemDefault()).toLocalDate())} 天气源:$provider 定位来源:${location.provider} 精度:${location.accuracy.toInt()}m 坐标:${"%.5f".format(location.latitude)},${"%.5f".format(location.longitude)} 时间:${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date(location.time))}"
                withContext(Dispatchers.Main) { onSuccess(weather, source) }
            },
            onSuccess = {},
            onError = { toast("自动获取天气失败: ${it.message}") }
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(maxAgeMs: Long, maxAccuracyMeters: Float): Location? {
        val locationManager = getSystemService(LocationManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val providers = locationManager.getProviders(true)
        return providers
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { location ->
                val ageMs = now - location.time
                ageMs in 0..maxAgeMs && location.accuracy <= maxAccuracyMeters
            }
            .minByOrNull { it.accuracy }
    }

    private suspend fun getPreciseLocation(): Location? {
        val locationManager = getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { provider -> runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false) }

        // Return a very fresh/accurate cache first to improve response speed.
        getLastKnownLocation(maxAgeMs = 2 * 60 * 1000, maxAccuracyMeters = 80f)?.let { return it }

        val current = coroutineScope {
            providers.map { provider ->
                async {
                    withTimeoutOrNull(3_500) {
                        getCurrentLocationFromProvider(locationManager, provider)
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (current.isNotEmpty()) {
            current.firstOrNull { it.provider == LocationManager.GPS_PROVIDER && it.accuracy <= 120f }
                ?.let { return it }
            return current.minByOrNull { it.accuracy }
        }

        return getLastKnownLocation(maxAgeMs = 30 * 60 * 1000, maxAccuracyMeters = 500f)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationFromProvider(
        locationManager: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        val executor = Executors.newSingleThreadExecutor()
        runCatching {
            locationManager.getCurrentLocation(provider, signal, executor) { location ->
                if (cont.isActive) cont.resume(location)
                executor.shutdown()
            }
        }.onFailure {
            if (cont.isActive) cont.resume(null)
            executor.shutdown()
        }
    }

    private suspend fun queryWeatherWithPriority(
        latitude: Double,
        longitude: Double,
        amapKey: String,
        qWeatherKey: String?,
        qWeatherApiHost: String?,
        targetDateMillis: Long
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val adcode = queryAmapAdcode(latitude, longitude, amapKey)
        val forecast = queryAmapForecast(adcode, amapKey, targetDateMillis)
        if (!forecast.isNullOrBlank()) return@withContext forecast to "高德"

        val targetDate = Instant.ofEpochMilli(targetDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now(ZoneId.systemDefault())
        if (targetDate.isBefore(today)) {
            val daysAgo = ChronoUnit.DAYS.between(targetDate, today)
            if (daysAgo > 10) {
                throw IllegalStateException("该日期已超过10天，请手动填写天气")
            }
            val key = qWeatherKey ?: throw IllegalStateException("请先在设置中配置和风天气 Key（用于10天内历史天气）")
            return@withContext queryQWeatherHistorical(latitude, longitude, key, targetDate, qWeatherApiHost) to "和风"
        }

        queryAmapLive(adcode, amapKey, targetDateMillis) to "高德"
    }

    private fun queryAmapAdcode(latitude: Double, longitude: Double, key: String): String {
        val url = URL("https://restapi.amap.com/v3/geocode/regeo?location=$longitude,$latitude&key=$key&extensions=base")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
        }
        try {
            val body = connection.readBody()
            val json = JSONObject(body)
            if (json.optString("status") != "1") {
                throw IllegalStateException(json.optString("info", "逆地理编码失败"))
            }
            return json.getJSONObject("regeocode")
                .getJSONObject("addressComponent")
                .getString("adcode")
        } finally {
            connection.disconnect()
        }
    }

    private fun queryAmapForecast(adcode: String, key: String, targetDateMillis: Long): String? {
        val url = URL("https://restapi.amap.com/v3/weather/weatherInfo?city=$adcode&key=$key&extensions=all")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
        }
        try {
            val body = connection.readBody()
            val json = JSONObject(body)
            if (json.optString("status") != "1") return null
            val forecasts = json.optJSONArray("forecasts") ?: return null
            if (forecasts.length() == 0) return null
            val casts = forecasts.getJSONObject(0).optJSONArray("casts") ?: return null
            if (casts.length() == 0) return null

            val targetDate = Instant.ofEpochMilli(targetDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val hit = (0 until casts.length())
                .map { casts.getJSONObject(it) }
                .firstOrNull { item ->
                    val dateText = item.optString("date", "")
                    runCatching { LocalDate.parse(dateText, DATE_FORMATTER) }.getOrNull() == targetDate
                } ?: return null

            val dayWeather = hit.optString("dayweather", "")
            val nightWeather = hit.optString("nightweather", dayWeather)
            val dayTemp = hit.optString("daytemp", "")
            val nightTemp = hit.optString("nighttemp", "")
            val weather = if (dayWeather == nightWeather || nightWeather.isBlank()) {
                dayWeather
            } else {
                "${dayWeather}转${nightWeather}"
            }
            return if (dayTemp.isNotBlank() && nightTemp.isNotBlank()) {
                "$weather ${nightTemp}~${dayTemp}℃"
            } else {
                weather
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun queryAmapLive(adcode: String, key: String, targetDateMillis: Long): String {
        val targetDate = Instant.ofEpochMilli(targetDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now(ZoneId.systemDefault())
        if (targetDate != today) {
            throw IllegalStateException("高德免费接口不支持历史天气，请手动补录该日期天气")
        }

        val url = URL("https://restapi.amap.com/v3/weather/weatherInfo?city=$adcode&key=$key&extensions=base")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
        }
        try {
            val body = connection.readBody()
            val json = JSONObject(body)
            if (json.optString("status") != "1") {
                throw IllegalStateException(json.optString("info", "天气查询失败"))
            }
            val lives = json.optJSONArray("lives") ?: throw IllegalStateException("天气数据为空")
            if (lives.length() == 0) throw IllegalStateException("天气数据为空")
            val live = lives.getJSONObject(0)
            val weather = live.optString("weather", "未知天气")
            val temp = live.optString("temperature", "")
            return if (temp.isNotBlank()) "$weather ${temp}℃" else weather
        } finally {
            connection.disconnect()
        }
    }

    private fun queryQWeatherHistorical(
        latitude: Double,
        longitude: Double,
        key: String,
        targetDate: LocalDate,
        apiHost: String?
    ): String {
        val locationId = queryQWeatherLocationId(latitude, longitude, key, apiHost)
        val dateText = targetDate.format(DateTimeFormatter.BASIC_ISO_DATE)
        var lastError: Throwable? = null
        for (host in buildQWeatherHostCandidates(apiHost)) {
            try {
                return queryQWeatherHistoricalByHost(host, locationId, dateText, key)
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw IllegalStateException(lastError?.message ?: "和风历史天气查询失败")
    }

    private fun queryQWeatherHistoricalByHost(
        host: String,
        locationId: String,
        dateText: String,
        key: String
    ): String {
        val encodedLocation = urlEncode(locationId)
        val url = URL("$host/v7/historical/weather?location=$encodedLocation&date=$dateText&key=$key")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ConstructionLog/1.0 (Android)")
            setRequestProperty("X-QW-Api-Key", key)
        }
        try {
            val json = connection.readJson("和风历史天气")
            val code = json.optString("code", "")
            if (code != "200") {
                val snippet = json.toString().take(180)
                throw IllegalStateException("和风历史天气查询失败(http=${connection.responseCode}, code=${if (code.isBlank()) "-" else code}) $snippet")
            }
            val daily = firstDailyWeather(json) ?: throw IllegalStateException("和风历史天气数据为空")
            val hourly = json.optJSONArray("weatherHourly")
            val inferred = inferWeatherFromHourly(hourly)
            val dayWeather = daily.optString("textDay")
                .ifBlank { inferred.first.ifBlank { daily.optString("text", "未知天气") } }
            val nightWeather = daily.optString("textNight")
                .ifBlank { inferred.second.ifBlank { dayWeather } }
            val maxTemp = daily.optString("tempMax", "")
            val minTemp = daily.optString("tempMin", "")
            val weather = if (dayWeather == nightWeather || nightWeather.isBlank()) {
                dayWeather
            } else {
                "${dayWeather}转${nightWeather}"
            }
            return if (maxTemp.isNotBlank() && minTemp.isNotBlank()) {
                "$weather ${minTemp}~${maxTemp}℃"
            } else {
                weather
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun queryQWeatherLocationId(
        latitude: Double,
        longitude: Double,
        key: String,
        apiHost: String?
    ): String {
        val location = urlEncode("$longitude,$latitude")
        var lastError: Throwable? = null
        for (host in buildQWeatherHostCandidates(apiHost)) {
            val url = URL("$host/geo/v2/city/lookup?location=$location&key=$key")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ConstructionLog/1.0 (Android)")
                setRequestProperty("X-QW-Api-Key", key)
            }
            try {
                val json = connection.readJson("和风地理编码")
                if (json.optString("code") != "200") {
                    throw IllegalStateException("和风地理编码失败(code=${json.optString("code", "-")})")
                }
                val locations = json.optJSONArray("location") ?: throw IllegalStateException("和风地理编码为空")
                if (locations.length() == 0) throw IllegalStateException("和风地理编码为空")
                return locations.getJSONObject(0).optString("id", "").ifBlank {
                    throw IllegalStateException("和风地理编码缺少地点ID")
                }
            } catch (t: Throwable) {
                lastError = t
            } finally {
                connection.disconnect()
            }
        }
        throw IllegalStateException(lastError?.message ?: "和风地理编码失败")
    }

    private fun firstDailyWeather(json: JSONObject): JSONObject? {
        val weatherDaily = json.optJSONArray("weatherDaily")
        if (weatherDaily != null && weatherDaily.length() > 0) return weatherDaily.optJSONObject(0)
        val weatherDailyObj = json.optJSONObject("weatherDaily")
        if (weatherDailyObj != null) return weatherDailyObj
        val daily = json.optJSONArray("daily")
        if (daily != null && daily.length() > 0) return daily.optJSONObject(0)
        return null
    }

    private fun inferWeatherFromHourly(hourly: org.json.JSONArray?): Pair<String, String> {
        if (hourly == null || hourly.length() == 0) return "" to ""
        val dayFreq = linkedMapOf<String, Int>()
        val nightFreq = linkedMapOf<String, Int>()
        for (i in 0 until hourly.length()) {
            val item = hourly.optJSONObject(i) ?: continue
            val text = item.optString("text", "").trim()
            if (text.isBlank()) continue
            val time = item.optString("time", "")
            val hour = extractHour(time) ?: continue
            if (hour in 6..17) {
                dayFreq[text] = (dayFreq[text] ?: 0) + 1
            } else {
                nightFreq[text] = (nightFreq[text] ?: 0) + 1
            }
        }
        val day = dayFreq.maxByOrNull { it.value }?.key.orEmpty()
        val night = nightFreq.maxByOrNull { it.value }?.key.orEmpty()
        return day to night
    }

    private fun extractHour(time: String): Int? {
        val tIndex = time.indexOf('T')
        if (tIndex < 0 || tIndex + 3 > time.length) return null
        return time.substring(tIndex + 1, tIndex + 3).toIntOrNull()
    }

    private fun HttpURLConnection.readBody(): String {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private fun HttpURLConnection.readJson(apiName: String): JSONObject {
        val code = runCatching { responseCode }.getOrDefault(-1)
        val body = readBody()
        if (body.isBlank()) {
            throw IllegalStateException("$apiName 返回空响应(http=$code)")
        }
        return runCatching { JSONObject(body) }.getOrElse {
            val snippet = body.replace('\n', ' ').take(140)
            throw IllegalStateException("$apiName 返回非JSON(http=$code): $snippet")
        }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun buildQWeatherHostCandidates(configuredHost: String?): List<String> {
        if (!configuredHost.isNullOrBlank()) {
            val normalized = configuredHost.trim().removeSuffix("/")
            val withScheme = if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
                normalized
            } else {
                "https://$normalized"
            }
            return listOf(withScheme) + DEFAULT_QWEATHER_API_HOSTS.filterNot { it.equals(withScheme, ignoreCase = true) }
        }
        return DEFAULT_QWEATHER_API_HOSTS
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DEFAULT_QWEATHER_API_HOSTS = listOf(
            "https://devapi.qweather.com",
            "https://api.qweather.com"
        )
    }
}
