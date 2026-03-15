# 架构说明

## 项目概述

装修日记是一款基于 Jetpack Compose 的 Android 应用，采用 MVVM 架构，专注于本地离线数据管理，注重数据安全与隐私。

## 技术栈

### UI 层
- **Kotlin**: 100% Kotlin 开发
- **Jetpack Compose**: 声明式 UI 框架
- **Material3**: Google 设计系统
- **Kotlin Coroutines + Flow**: 异步处理和状态管理

### 架构层
- **MVVM**: Model-View-ViewModel 架构模式
- **Repository Pattern**: 数据仓库模式
- **Dependency Injection**: 手动依赖注入（未来考虑 Hilt）

### 数据层
- **Room**: 本地数据库
- **SQLCipher**: 数据库加密
- **Coil**: 图片加载和缓存

### 工具层
- **WorkManager**: 后台任务调度
- **Biometric Prompt**: 生物识别
- **KtLint**: 代码格式化

## 目录结构

```
app/src/main/java/com/example/constructionlog/
├── ConstructionLogApp.kt          # 应用入口
├── MainActivity.kt                 # 主 Activity
│
├── ui/                             # UI 层
│   ├── AppScreen.kt               # 应用主屏幕
│   ├── MainViewModel.kt           # 主 ViewModel
│   ├── Navigation.kt              # 导航配置
│   ├── SplashScreen.kt            # 启动页
│   │
│   ├── screens/                   # 页面组件
│   │   ├── HomeScreen.kt          # 首页（日志列表）
│   │   ├── EditorScreen.kt        # 编辑日志页面
│   │   ├── CalendarScreen.kt      # 日历视图
│   │   ├── SettingsScreen.kt      # 设置页面
│   │   ├── TrashScreen.kt         # 回收站
│   │   ├── OnboardingScreen.kt    # 引导页
│   │   └── AcceptanceHomeScreen.kt # 签收首页
│   │   └── AcceptanceEditorScreen.kt # 签收编辑页
│   │
│   ├── components/                # 可复用组件
│   │   ├── LogCard.kt             # 日志卡片
│   │   ├── ImagePicker.kt         # 图片选择器
│   │   ├── EmptyState.kt          # 空状态组件
│   │   ├── EditorForm.kt          # 编辑表单
│   │   ├── CalendarComponents.kt  # 日历组件
│   │   ├── ProjectManagerDialog.kt # 项目管理对话框
│   │   ├── TrashComponents.kt     # 回收站组件
│   │   ├── DockBar.kt             # 底部导航栏
│   │   └── SettingsContent.kt     # 设置内容
│   │
│   └── theme/                     # 主题配置
│       └── AppTheme.kt            # 应用主题
│
├── data/                          # 数据层
│   ├── Entities.kt                # 数据实体
│   ├── LogDao.kt                  # 数据库访问对象
│   ├── AppDatabase.kt             # 数据库
│   ├── LogRepository.kt           # 日志仓库
│   ├── AcceptanceModels.kt        # 签收模型
│   ├── AppSettings.kt             # 应用设置
│   ├── QWeatherKeyStore.kt        # 天气 Key 存储
│   ├── AmapKeyStore.kt            # 高德 Key 存储
│   ├── BackupService.kt           # 备份服务
│   ├── PdfExportService.kt        # PDF 导出服务
│   └── backup/                    # 备份相关
│       ├── AutoBackupScheduler.kt # 自动备份调度器
│       └── AutoBackupWorker.kt    # 自动备份 Worker
│
└── security/                      # 安全相关
    └── BiometricAuthenticator.kt  # 生物识别认证器
```

## MVVM 架构详解

### ViewModel
**职责**：
- 负责业务逻辑和状态管理
- 使用 `StateFlow` 管理UI状态
- 不包含 UI 相关代码
- 处理异步操作（Coroutines + Flow）

**特点**：
- 可观察：通过 `StateFlow` 向 UI 提供只读状态
- 持久化：配置变更时状态不丢失（配合 `savedStateHandle`）
- 清晰：职责单一，只关注业务逻辑

### Repository
**职责**：
- 封装数据访问逻辑
- 提供统一的数据接口
- 支持 Room 数据库操作
- 处理数据转换和业务逻辑

**特点**：
- 抽象：隐藏数据源细节
- 单一：每个实体对应一个 Repository
- 可测试：易于模拟和测试

### UI 层 (Jetpack Compose)
**职责**：
- 声明式 UI 渲染
- 观察 ViewModel 状态
- 处理用户交互
- 使用 `SideEffect` 处理副作用

**特点**：
- 声明式：描述 UI 状态，系统自动更新
- 组合式：通过组合构建复杂 UI
- 响应式：状态变化自动触发 UI 更新

## 数据流

```
UI (Compose)
  ↓ observe()
ViewModel
  ↓ call()
Repository
  ↓ call()
Dao (Room)
  ↓ return()
Repository
  ↓ emit()
ViewModel
  ↓ observe()
UI (Compose)
```

### 典型流程示例

**添加日志**：
```
1. 用户在 EditorScreen 输入内容
2. EditorViewModel.onSaveClick()
3. LogRepository.addLog(log)
4. Room Dao 插入数据
5. StateFlow 更新，UI 自动刷新
```

**查看日志列表**：
```
1. HomeScreen observe MainViewModel.logs
2. MainViewModel.loadLogs()
3. LogRepository.getAllLogs()
4. Room Dao 查询数据
5. StateFlow 更新，UI 自动刷新
```

## 状态管理

### StateFlow
**用途**：UI 状态管理
- 只读，由 ViewModel 更新
- 多个 UI 组件可以观察同一个 StateFlow
- 状态变化自动触发 UI 刷新

### MutableStateFlow
**用途**：ViewModel 内部状态管理
- 可读写
- 仅在 ViewModel 内部使用
- 用于处理临时状态

### SideEffect
**用途**：副作用处理
- 导航跳转
- 显示提示消息
- 不影响 UI 状态
- 通过 LaunchedEffect 处理

### SavedStateHandle
**用途**：配置变更时保存状态
- Activity/Fragment 销毁重建时保留数据
- 用于 ViewModel 之间的数据传递

## 数据库设计

### Log 表
```kotlin
@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                    // 日期
    val weather: String?,                // 天气
    val location: String?,               // 施工部位
    val content: String,                 // 施工内容
    val person: String?,                 // 施工人员
    val safety: String?,                 // 安全文明
    val notes: String?,                  // 备注
    val images: String?,                 // 图片路径列表
    val isDeleted: Boolean = false,      // 是否删除
    val projectId: Long = 1,             // 项目ID
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### Acceptance 表（签收）
```kotlin
@Entity(tableName = "acceptances")
data class AcceptanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logId: Long,                     // 关联日志ID
    val siteName: String?,               // 现场名称
    val sitePerson: String?,             // 现场负责人
    val sitePhone: String?,              // 联系电话
    val siteTime: String?,               // 签收时间
    val status: String = "pending",      // 签收状态
    val createdAt: Long = System.currentTimeMillis()
)
```

### 关系设计
- Log 和 Acceptance：一对多关系（一个日志可以有多个签收）
- 使用 `@Relation` 注解处理关联查询

## 安全设计

### 数据库加密
- 使用 **SQLCipher** 加密数据库文件
- Key 存储在 **Android Keystore** 中
- 密钥派生自用户设置的密码

```kotlin
// 示例：数据库初始化
val password = passwordStore.getPassword() ?: return
val key = SqlCipherCompat.generateKey(password)
database = Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    "construction_log.db"
).openHelperFactory(EncryptedFile.FileFactory { ... }).build()
```

### 备份加密
- ZIP 文件使用 **AES-256** 加密
- 密钥派生自用户密码
- 加密强度与数据库一致

### 生物识别
- 使用 **Biometric Prompt** API
- 支持指纹和面容识别
- 首次使用需要设置密码

```kotlin
// 示例：生物识别认证
val authenticator = BiometricAuthenticator(context)
authenticator.authenticate(
    onSuccess = { /* 解锁成功 */ },
    onFailure = { /* 解锁失败 */ }
)
```

### 防窥屏
- 使用 **ActivityOptionsCompat.makeBasic()** 设置模糊遮罩
- 在系统最近任务列表中显示模糊效果

## 性能优化

### 列表优化
- 使用 `LazyColumn` 替代 `Column`，支持虚拟化
- 使用 `key()` 函数优化列表项更新
- 使用 `DiffUtil` 优化刷新

```kotlin
LazyColumn {
    items(items = logs, key = { it.id }) { log ->
        LogCard(log)
    }
}
```

### 图片优化
- 使用 **Coil** 懒加载图片
- 自动缩略图缓存
- 压缩存储，减少空间占用

```kotlin
// 示例：图片加载
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(log.images)
        .crossfade(true)
        .build(),
    contentDescription = null
)
```

### 备份优化
- **流式处理**：避免一次性加载大文件到内存
- **分卷备份**：大文件自动分卷
- **内存占用降低 60-80%**

```kotlin
// 示例：流式备份
val zipFile = ZipFile(outputFile)
zipFile.use { zip ->
    logsWithImages.forEach { logWithImages ->
        // 逐条处理，不一次性加载所有数据
        val log = logWithImages.log
        val images = logWithImages.images
        // 流式写入 ZIP
    }
}
```

## 扩展性设计

### 插件化
- 未来可扩展插件系统
- 支持自定义数据源
- 支持自定义功能模块

### 模块化
- UI 层、数据层分离
- 便于独立开发和测试
- 支持增量更新

### 多项目支持
- 项目隔离机制
- 独立数据库实例
- 独立备份文件

```kotlin
// 示例：多项目隔离
@Entity(tableName = "logs")
data class LogEntity(
    // ...
    val projectId: Long = 1,  // 项目 ID
    // ...
)

// 查询时指定项目
@Query("SELECT * FROM logs WHERE projectId = :projectId AND isDeleted = 0")
fun getLogsByProject(projectId: Long): Flow<List<LogWithImages>>
```

## 测试策略

### 单元测试
- 测试 ViewModel 业务逻辑
- 测试 Repository 数据访问
- 测试工具类方法

**示例**：
```kotlin
@Test
fun `test add log`() {
    val repository = LogRepositoryFake()
    val viewModel = MainViewModel(repository)

    viewModel.addLog(testLog)

    assertEquals(1, viewModel.logs.value.size)
}
```

### UI 测试
- 使用 Compose Testing
- 测试 UI 渲染和交互

**示例**：
```kotlin
@Test
fun `test log list renders correctly`() {
    composeTestRule.setContent {
        ConstructionLogTheme {
            HomeScreen()
        }
    }

    composeTestRule.onNodeWithText("测试日志").assertIsDisplayed()
}
```

### 测试覆盖率目标
- 单元测试覆盖率 ≥ 60%
- UI 测试覆盖主要流程

## 依赖注入

### 当前方案：手动 DI
- 通过构造函数注入依赖
- 使用 `object` 单例管理全局依赖

### 未来方案：Hilt
- 考虑迁移到 Hilt
- 提供更好的代码生成和类型安全
- 支持模块化架构

## 构建系统

### Gradle 配置
- **Build Script DSL**: Kotlin DSL (`build.gradle.kts`)
- **Kotlin**: 1.9.20
- **Compose**: 1.5.0
- **Room**: 2.6.0

### 依赖管理
```kotlin
dependencies {
    // UI
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // 数据
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    ksp("androidx.room:room-compiler:2.6.0")

    // 图片
    implementation("io.coil-kt:coil-compose:2.4.0")

    // 异步
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
}
```

## 总结

装修日记项目采用现代化的 Android 开发技术栈和架构模式，注重代码质量、性能优化和数据安全。通过清晰的分层架构和规范的代码组织，确保项目的可维护性和可扩展性。

---

**更新日期**：2026-03-15
