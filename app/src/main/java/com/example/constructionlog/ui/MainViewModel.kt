package com.example.constructionlog.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructionlog.ConstructionLogApp
import com.example.constructionlog.data.ConstructionLogEntity
import com.example.constructionlog.data.LogRepository
import com.example.constructionlog.data.LogWithImages
import com.example.constructionlog.data.ProjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScreenMode {
    LIST,
    EDITOR,
    TRASH
}

data class EditorState(
    val editingId: Long? = null,
    val projectId: Long = ConstructionLogEntity.DEFAULT_PROJECT_ID,
    val date: Long = System.currentTimeMillis(),
    val weather: String = "",
    val weatherSource: String = "",
    val location: String = "",
    val content: String = "",
    val workers: String = "",
    val workerNames: String = "",
    val safety: String = "",
    val remark: String = "",
    val imageUris: List<String> = emptyList()
) {
    fun isValid(): Boolean = content.isNotBlank() && location.isNotBlank()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LogRepository = (application as ConstructionLogApp).repository

    val projects = repository.observeProjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    val logs = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId == null) flowOf(emptyList()) else repository.observeLogs(projectId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val trash = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId == null) flowOf(emptyList()) else repository.observeTrash(projectId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val selectedProject = combine(projects, _selectedProjectId) { list, selectedId ->
        list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val _screenMode = MutableStateFlow(ScreenMode.LIST)
    val screenMode: StateFlow<ScreenMode> = _screenMode.asStateFlow()

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.cleanupTrash()
        }
        viewModelScope.launch {
            projects.collect { list ->
                if (list.isEmpty()) return@collect
                val selected = _selectedProjectId.value
                if (selected == null || list.none { it.id == selected }) {
                    _selectedProjectId.value = list.first().id
                }
            }
        }
    }

    fun showList() {
        _screenMode.value = ScreenMode.LIST
    }

    fun showTrash() {
        _screenMode.value = ScreenMode.TRASH
    }

    fun startCreate() {
        _editorState.value = EditorState(
            projectId = _selectedProjectId.value ?: ConstructionLogEntity.DEFAULT_PROJECT_ID
        )
        _screenMode.value = ScreenMode.EDITOR
    }

    fun startEdit(item: LogWithImages) {
        _editorState.value = EditorState(
            editingId = item.log.id,
            projectId = item.log.projectId,
            date = item.log.date,
            weather = item.log.weather,
            location = item.log.location,
            content = item.log.content,
            workers = item.log.workers?.toString() ?: "",
            workerNames = item.log.workerNames,
            safety = item.log.safety,
            remark = item.log.remark,
            imageUris = item.images.map { it.imageUri }
        )
        _screenMode.value = ScreenMode.EDITOR
    }

    fun updateDate(timeMillis: Long) {
        _editorState.update { it.copy(date = timeMillis) }
    }

    fun selectProject(projectId: Long) {
        _selectedProjectId.value = projectId
    }

    fun updateProjectId(projectId: Long) {
        _editorState.update { it.copy(projectId = projectId) }
    }

    fun updateWeatherManual(value: String) = _editorState.update { it.copy(weather = value, weatherSource = "") }
    fun updateWeatherAuto(value: String, source: String) = _editorState.update { it.copy(weather = value, weatherSource = source) }
    fun updateLocation(value: String) = _editorState.update { it.copy(location = value) }
    fun updateContent(value: String) = _editorState.update { it.copy(content = value) }
    fun updateWorkers(value: String) = _editorState.update { it.copy(workers = value) }
    fun updateWorkerNames(value: String) = _editorState.update { it.copy(workerNames = value) }
    fun updateSafety(value: String) = _editorState.update { it.copy(safety = value) }
    fun updateRemark(value: String) = _editorState.update { it.copy(remark = value) }

    fun addImages(uris: List<Uri>) {
        _editorState.update { current ->
            current.copy(imageUris = (current.imageUris + uris.map(Uri::toString)).distinct())
        }
    }

    fun removeImage(uri: String) {
        _editorState.update { it.copy(imageUris = it.imageUris.filterNot { item -> item == uri }) }
    }

    fun save(onDone: () -> Unit) {
        val state = _editorState.value
        if (!state.isValid() || state.projectId <= 0L) return

        viewModelScope.launch {
            repository.save(
                existingId = state.editingId,
                projectId = state.projectId,
                date = state.date,
                weather = state.weather,
                location = state.location,
                content = state.content,
                workers = state.workers.toIntOrNull(),
                workerNames = state.workerNames,
                safety = state.safety,
                remark = state.remark,
                imageUris = state.imageUris
            )
            _screenMode.value = ScreenMode.LIST
            onDone()
        }
    }

    fun addProject(name: String, onDone: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching {
                val id = repository.addProject(name)
                _selectedProjectId.value = id
                id
            })
        }
    }

    fun renameProject(id: Long, name: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching {
                repository.renameProject(id, name)
            })
        }
    }

    fun deleteProject(id: Long, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching {
                val currentProjects = projects.value
                require(currentProjects.size > 1) { "至少保留一个项目" }
                repository.deleteProject(id)
                if (_selectedProjectId.value == id) {
                    _selectedProjectId.value = currentProjects.firstOrNull { it.id != id }?.id
                }
            })
        }
    }

    fun moveToTrash(id: Long) {
        viewModelScope.launch {
            repository.moveToTrash(id)
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            repository.restore(id)
        }
    }

    fun deleteForever(id: Long) {
        viewModelScope.launch {
            repository.deleteForever(id)
        }
    }

    suspend fun clearAllData() {
        repository.clearAllData()
    }
}
