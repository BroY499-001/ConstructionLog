package com.constructionlog.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.constructionlog.app.ConstructionLogApp
import com.constructionlog.app.data.ConstructionLogEntity
import com.constructionlog.app.data.LogRepository
import com.constructionlog.app.data.LogWithImages
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
    val stage: String = "",
    val remark: String = "",
    val imageUris: List<String> = emptyList()
) {
    fun isValid(): Boolean = content.isNotBlank() && location.isNotBlank()
}

data class AcceptanceItemDraft(
    val id: Long? = null,
    val category: String = "",
    val subItem: String = "",
    val standard: String = "",
    val basis: String = "",
    val status: String = com.constructionlog.app.data.AcceptanceStatus.PENDING,
    val note: String = "",
    val imageUris: List<String> = emptyList(),
    // 材料信息（融合到子项中）
    val materialName: String = "",
    val materialBrand: String = "",
    val materialSpec: String = ""
)

data class AcceptanceMaterialDraft(
    val id: Long? = null,
    val name: String = "",
    val brand: String = "",
    val spec: String = "",
    val status: String = com.constructionlog.app.data.AcceptanceStatus.PENDING,
    val note: String = ""
)

data class AcceptanceEditorState(
    val editingId: Long? = null,
    val projectId: Long = com.constructionlog.app.data.ConstructionLogEntity.DEFAULT_PROJECT_ID,
    val type: String = "阶段验收",
    val stage: String = "水电验收",
    val date: Long = System.currentTimeMillis(),
    val weather: String = "",
    val weatherSource: String = "",
    val location: String = "",
    val inspector: String = "",
    val remark: String = "",
    val items: List<AcceptanceItemDraft> = emptyList(),
    val materials: List<AcceptanceMaterialDraft> = emptyList(),
    val imageUris: List<String> = emptyList()
) {
    fun isValid(): Boolean = location.isNotBlank()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LogRepository = (application as ConstructionLogApp).repository

    val projects = repository.observeProjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _projectsLoaded = MutableStateFlow(false)
    val projectsLoaded: StateFlow<Boolean> = _projectsLoaded.asStateFlow()

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

    val planTasks = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId == null) flowOf(emptyList()) else repository.observePlanTasks(projectId)
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

    val acceptanceForms = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId == null) flowOf(emptyList()) else repository.observeAcceptanceForms(projectId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _acceptanceEditorState = MutableStateFlow(AcceptanceEditorState())
    val acceptanceEditorState: StateFlow<AcceptanceEditorState> = _acceptanceEditorState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.cleanupTrash()
        }
        viewModelScope.launch {
            repository.observeProjects().collect {
                _projectsLoaded.value = true
            }
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

    fun startCreateAcceptance(stage: String = "水电验收") {
        val projectId = _selectedProjectId.value ?: ConstructionLogEntity.DEFAULT_PROJECT_ID
        val template = AcceptanceTemplates.templateByName(stage)
        _acceptanceEditorState.value = AcceptanceEditorState(
            projectId = projectId,
            stage = stage,
            items = template?.items ?: AcceptanceTemplates.waterElectricItems()
        )
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
            stage = item.log.stage,
            remark = item.log.remark,
            imageUris = item.images.map { it.imageUri }
        )
        _screenMode.value = ScreenMode.EDITOR
    }

    fun startEditAcceptance(item: com.constructionlog.app.data.AcceptanceFormWithDetails) {
        val materialsByIndex = item.materials.sortedBy { it.orderIndex }
        val itemDrafts = item.items.sortedBy { it.orderIndex }.mapIndexed { idx, it ->
            val mat = materialsByIndex.getOrNull(idx)
            AcceptanceItemDraft(
                id = it.id,
                category = it.category,
                subItem = it.subItem,
                standard = it.standard,
                basis = it.basis,
                status = it.status,
                note = it.note,
                imageUris = decodeImageUris(it.imageUris),
                materialName = mat?.name ?: "",
                materialBrand = mat?.brand ?: "",
                materialSpec = mat?.spec ?: ""
            )
        }
        _acceptanceEditorState.value = AcceptanceEditorState(
            editingId = item.form.id,
            projectId = item.form.projectId,
            type = item.form.type,
            stage = item.form.stage,
            date = item.form.date,
            weather = item.form.weather,
            location = item.form.location,
            inspector = item.form.inspector,
            remark = item.form.remark,
            items = itemDrafts,
            imageUris = item.images.map { it.imageUri }
        )
    }

    fun startEditAcceptanceById(id: Long, onReady: (Boolean) -> Unit) {
        viewModelScope.launch {
            val item = repository.getAcceptanceFormById(id)
            if (item == null) {
                onReady(false)
                return@launch
            }
            startEditAcceptance(item)
            onReady(true)
        }
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
    fun updateStage(value: String) = _editorState.update { it.copy(stage = value) }
    fun updateRemark(value: String) = _editorState.update { it.copy(remark = value) }

    fun updateAcceptanceProjectId(projectId: Long) {
        _acceptanceEditorState.update { it.copy(projectId = projectId) }
    }

    fun updateAcceptanceDate(value: Long) {
        _acceptanceEditorState.update { it.copy(date = value) }
    }

    fun updateAcceptanceWeather(value: String) {
        _acceptanceEditorState.update { it.copy(weather = value, weatherSource = "") }
    }

    fun updateAcceptanceWeatherAuto(value: String, source: String) {
        _acceptanceEditorState.update { it.copy(weather = value, weatherSource = source) }
    }

    fun updateAcceptanceLocation(value: String) {
        _acceptanceEditorState.update { it.copy(location = value) }
    }

    fun updateAcceptanceInspector(value: String) {
        _acceptanceEditorState.update { it.copy(inspector = value) }
    }

    fun updateAcceptanceRemark(value: String) {
        _acceptanceEditorState.update { it.copy(remark = value) }
    }

    fun updateAcceptanceType(value: String) {
        _acceptanceEditorState.update { it.copy(type = value) }
    }

    fun updateAcceptanceStage(value: String) {
        val template = AcceptanceTemplates.templateByName(value)
        _acceptanceEditorState.update { state ->
            if (template == null) {
                state.copy(stage = value)
            } else {
                state.copy(
                    stage = value,
                    items = template.items
                )
            }
        }
    }

    fun updateAcceptanceItem(index: Int, update: AcceptanceItemDraft) {
        _acceptanceEditorState.update { state ->
            val items = state.items.toMutableList()
            if (index in items.indices) {
                items[index] = update
            }
            state.copy(items = items)
        }
    }

    fun addAcceptanceItem() {
        _acceptanceEditorState.update { state ->
            state.copy(items = state.items + AcceptanceItemDraft())
        }
    }

    fun removeAcceptanceItem(index: Int) {
        _acceptanceEditorState.update { state ->
            if (index !in state.items.indices) return@update state
            state.copy(items = state.items.filterIndexed { i, _ -> i != index })
        }
    }

    fun addAcceptanceItemImages(index: Int, uris: List<Uri>) {
        _acceptanceEditorState.update { state ->
            if (index !in state.items.indices) return@update state
            val items = state.items.toMutableList()
            val current = items[index]
            items[index] = current.copy(
                imageUris = (current.imageUris + uris.map(Uri::toString)).distinct()
            )
            state.copy(items = items)
        }
    }

    fun updateAcceptanceMaterial(index: Int, update: AcceptanceMaterialDraft) {
        _acceptanceEditorState.update { state ->
            val items = state.materials.toMutableList()
            if (index in items.indices) {
                items[index] = update
            }
            state.copy(materials = items)
        }
    }

    fun addAcceptanceMaterial() {
        _acceptanceEditorState.update { state ->
            state.copy(materials = state.materials + AcceptanceMaterialDraft())
        }
    }

    fun removeAcceptanceMaterial(index: Int) {
        _acceptanceEditorState.update { state ->
            if (index !in state.materials.indices) return@update state
            state.copy(materials = state.materials.filterIndexed { i, _ -> i != index })
        }
    }

    fun addImages(uris: List<Uri>) {
        _editorState.update { current ->
            current.copy(imageUris = (current.imageUris + uris.map(Uri::toString)).distinct())
        }
    }

    fun removeImage(uri: String) {
        _editorState.update { it.copy(imageUris = it.imageUris.filterNot { item -> item == uri }) }
    }

    fun addAcceptanceImages(uris: List<Uri>) {
        _acceptanceEditorState.update { current ->
            current.copy(imageUris = (current.imageUris + uris.map(Uri::toString)).distinct())
        }
    }

    fun removeAcceptanceImage(uri: String) {
        _acceptanceEditorState.update { it.copy(imageUris = it.imageUris.filterNot { item -> item == uri }) }
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
                stage = state.stage,
                remark = state.remark,
                imageUris = state.imageUris
            )
            _screenMode.value = ScreenMode.LIST
            onDone()
        }
    }

    fun saveAcceptance(onDone: () -> Unit) {
        val state = _acceptanceEditorState.value
        if (!state.isValid() || state.projectId <= 0L) return

        viewModelScope.launch {
            val formId = repository.saveAcceptanceForm(
                existingId = state.editingId,
                projectId = state.projectId,
                type = state.type,
                stage = state.stage,
                date = state.date,
                weather = state.weather,
                location = state.location,
                inspector = state.inspector,
                remark = state.remark,
                items = state.items.mapIndexed { index, item ->
                    com.constructionlog.app.data.AcceptanceItemEntity(
                        id = item.id ?: 0,
                        formId = state.editingId ?: 0,
                        orderIndex = index,
                        category = item.category,
                        subItem = item.subItem,
                        standard = item.standard,
                        basis = item.basis,
                        status = item.status,
                        note = item.note,
                        imageUris = encodeImageUris(item.imageUris)
                    )
                },
                materials = state.items
                    .filter { it.materialName.isNotBlank() }
                    .mapIndexed { index, item ->
                        com.constructionlog.app.data.AcceptanceMaterialEntity(
                            id = 0,
                            formId = state.editingId ?: 0,
                            orderIndex = index,
                            name = item.materialName,
                            brand = item.materialBrand,
                            spec = item.materialSpec,
                            status = item.status,
                            note = ""
                        )
                    },
                imageUris = state.imageUris
            )
            val now = System.currentTimeMillis()
            val isFuture = state.date > now
            val linkedTask = repository.getPlanTaskByAcceptanceFormId(formId)
            if (isFuture) {
                val title = "验收：${state.stage}"
                val detail = buildString {
                    if (state.location.isNotBlank()) {
                        append("地点：").append(state.location)
                    }
                }
                if (linkedTask == null) {
                    repository.addPlanTask(
                        projectId = state.projectId,
                        title = title,
                        detail = detail,
                        dueAt = state.date,
                        priority = 2,
                        acceptanceFormId = formId
                    )
                } else {
                    repository.updatePlanTask(
                        id = linkedTask.id,
                        title = title,
                        detail = detail,
                        dueAt = state.date,
                        priority = linkedTask.priority
                    )
                }
            } else if (linkedTask != null) {
                repository.deletePlanTask(linkedTask.id)
            }
            onDone()
        }
    }

    fun deleteAcceptanceForm(id: Long) {
        viewModelScope.launch {
            repository.deleteAcceptanceForm(id)
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

    fun addPlanTask(title: String, detail: String, dueAt: Long?, priority: Int, onDone: (Result<Unit>) -> Unit) {
        val projectId = _selectedProjectId.value ?: run {
            onDone(Result.failure(IllegalStateException("请先创建项目")))
            return
        }
        viewModelScope.launch {
            onDone(runCatching {
                repository.addPlanTask(projectId, title, detail, dueAt, priority)
            })
        }
    }

    fun updatePlanTask(id: Long, title: String, detail: String, dueAt: Long?, priority: Int, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching {
                repository.updatePlanTask(id, title, detail, dueAt, priority)
            })
        }
    }

    fun togglePlanTask(id: Long, done: Boolean) {
        viewModelScope.launch {
            repository.setPlanTaskDone(id, done)
        }
    }

    fun deletePlanTask(id: Long) {
        viewModelScope.launch {
            repository.deletePlanTask(id)
        }
    }
}


object AcceptanceTemplates {
    data class Template(val name: String, val items: List<AcceptanceItemDraft>, val materials: List<AcceptanceMaterialDraft>)

    fun templateNames(): List<String> = templates.map { it.name }

    fun templateByName(name: String): Template? = templates.firstOrNull { it.name == name }

    private val templates: List<Template> = listOf(
        Template(
            name = "水电验收",
            items = waterElectricItems(),
            materials = emptyList()
        )
    )

    fun waterElectricItems(): List<AcceptanceItemDraft> = listOf(
        AcceptanceItemDraft(
            category = "基础规范",
            subItem = "材料进场",
            standard = "所有水电材料品牌、规格、型号应符合设计要求，并有产品合格证书。",
            basis = "GB 50327-2001《住宅装饰装修工程施工规范》"
        ),
        AcceptanceItemDraft(
            category = "给水工程",
            subItem = "管道试压",
            standard = "试验压力为工作压力的1.5倍，且不小于0.6MPa；稳压1小时，压力降不应大于0.05MPa。",
            basis = "GB 50242-2002《建筑给水排水及采暖工程施工质量验收规范》",
            materialName = "PPR水管", materialBrand = "公元", materialSpec = "国标，D20、D25，3米/根"
        ),
        AcceptanceItemDraft(
            category = "给水工程",
            subItem = "管道走向",
            standard = "敷设应横平竖直；冷热水管中心间距一般为150mm（±10mm）；遵循\u201C左热右冷、上热下冷\u201D原则。",
            basis = "GB 50327-2001《住宅装饰装修工程施工规范》"
        ),
        AcceptanceItemDraft(
            category = "给水工程",
            subItem = "管卡固定",
            standard = "管道安装牢固，管卡位置正确；DN15管径管卡间距：明装不大于1.5m，暗装不大于1.0m；转角处需增设管卡。",
            basis = "GB 50327-2001《住宅装饰装修工程施工规范》"
        ),
        AcceptanceItemDraft(
            category = "给水工程",
            subItem = "出水口位置",
            standard = "出水口应突出墙面饰面层，间距正确，且在同一水平线上（误差<2mm），无歪斜。",
            basis = "行业通用施工工艺规范"
        ),
        AcceptanceItemDraft(
            category = "排水工程",
            subItem = "管道坡度",
            standard = "排水管道必须有坡度，标准坡度一般为1%~2.5%（具体视管径而定），严禁倒坡。",
            basis = "GB 50242-2002《建筑给水排水及采暖工程施工质量验收规范》",
            materialName = "PVC下水管", materialBrand = "公元", materialSpec = "国标，D50、D110，4米/根"
        ),
        AcceptanceItemDraft(
            category = "排水工程",
            subItem = "存水弯",
            standard = "卫生器具（地漏、洗手盆等）排水口下方必须设有存水弯，水封深度不得小于50mm，防臭防虫。",
            basis = "GB 50242-2002《建筑给水排水及采暖工程施工质量验收规范》"
        ),
        AcceptanceItemDraft(
            category = "排水工程",
            subItem = "通水试验",
            standard = "排水通畅，无堵塞、无渗漏；排水横管与立管连接处应采用45度斜三通或顺水三通，减少阻力。",
            basis = "GB 50242-2002《建筑给水排水及采暖工程施工质量验收规范》"
        ),
        AcceptanceItemDraft(
            category = "电气工程",
            subItem = "开槽规范",
            standard = "墙体开槽应横平竖直；承重墙、梁、柱严禁开横向槽；开槽深度应确保管线埋入后抹灰层厚度>15mm。",
            basis = "GB 50327-2001《住宅装饰装修工程施工规范》",
            materialName = "PVC穿管", materialBrand = "公元", materialSpec = "国标，D20，3米/根"
        ),
        AcceptanceItemDraft(
            category = "电气工程",
            subItem = "强弱电间距",
            standard = "强弱电线管平行敷设间距应\u2265300mm；强弱电交叉处应成直角，且包裹铝箔纸做屏蔽处理。",
            basis = "GB 50303-2015《建筑电气工程施工质量验收规范》",
            materialName = "超六类网线", materialBrand = "科意姆", materialSpec = "国标，305米/卷"
        ),
        AcceptanceItemDraft(
            category = "电气工程",
            subItem = "线管穿线",
            standard = "导线在管内不得有接头；导线总截面积（含绝缘层）不应超过管内截面积的40%（保证活线，可抽动）。",
            basis = "GB 50303-2015《建筑电气工程施工质量验收规范》",
            materialName = "电线", materialBrand = "特变电工", materialSpec = "国标，100米/卷"
        ),
        AcceptanceItemDraft(
            category = "电气工程",
            subItem = "线径规范",
            standard = "照明回路\u22651.5mm\u00B2；插座回路\u22652.5mm\u00B2；厨卫/空调等大功率回路\u22654mm\u00B2（具体按设计图纸）。",
            basis = "GB 50054-2011《低压配电设计规范》"
        ),
        AcceptanceItemDraft(
            category = "电气工程",
            subItem = "分色布线",
            standard = "相线（火线）红色，零线蓝色或淡蓝色，接地线黄绿双色；便于后期维修识别。",
            basis = "GB 50303-2015《建筑电气工程施工质量验收规范》"
        ),
        AcceptanceItemDraft(
            category = "电气工程",
            subItem = "插座接线",
            standard = "插座接线必须遵循\u201C左零右火上接地\u201D原则；单相三孔插座，接地线接上孔。",
            basis = "GB 50303-2015《建筑电气工程施工质量验收规范》"
        ),
        AcceptanceItemDraft(
            category = "电气工程",
            subItem = "底盒安装",
            standard = "底盒安装平整、端正；线管与底盒连接处应使用锁扣（杯梳）固定，保护电线绝缘层。",
            basis = "行业通用施工工艺规范"
        ),
        AcceptanceItemDraft(
            category = "电气工程",
            subItem = "绝缘电阻",
            standard = "对导线进行绝缘电阻测试，线间及对地绝缘电阻值应大于0.5M\u03A9。",
            basis = "GB 50303-2015《建筑电气工程施工质量验收规范》"
        ),
        AcceptanceItemDraft(
            category = "防水工程",
            subItem = "防水涂料",
            standard = "防水涂料品牌、规格应符合设计要求，涂刷均匀、无漏涂。",
            basis = "GB 50327-2001《住宅装饰装修工程施工规范》",
            materialName = "防水涂料", materialBrand = "东方雨虹", materialSpec = "国标"
        )
    )

    fun waterElectricMaterials(): List<AcceptanceMaterialDraft> = emptyList()
}

private fun encodeImageUris(uris: List<String>): String = uris.joinToString(separator = "\n")

private fun decodeImageUris(raw: String): List<String> =
    raw.split('\n').map { it.trim() }.filter { it.isNotBlank() }
