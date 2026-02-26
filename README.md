# ConstructionLog

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android-green)
![minSdk](https://img.shields.io/badge/minSdk-34-purple)

一款功能完备的施工日志 Android 应用，专为本地离线使用设计，注重数据安全与隐私。

> **建议**：在下方功能介绍中添加一些应用截图或 GIF 动图，可以让项目更直观、更吸引人。

## ✨ 功能 (Features)

- **日志记录**: 支持日期、天气、施工部位、施工内容、人员、安全文明、备注等丰富字段。
- **图片管理**: 支持拍照或从相册多选图片（一次最多 30 张），内置图片浏览器，支持手势缩放和滑动切换。
- **日历总览**: 在日历首页按日期快速查看当日日志和图片统计，一目了然。
- **多项目管理**: 支持项目的创建、切换、重命名和删除，所有日志按项目完全隔离，方便管理多个工地。
- **回收站**: 提供恢复或彻底删除已删除日志的选项，防止误操作。
- **备份与恢复**: 支持将当前项目数据（包括数据库和所有图片）完整导出为 ZIP 文件进行备份，并可随时恢复。
- **PDF 导出**: 可按日期范围将当前项目的日志导出为 PDF 格式，方便归档和分享。
- **安全增强**:
    - 支持启用生物识别（指纹/面容）进行应用解锁。
    - 自动启用防窥屏功能，在系统的最近任务列表中显示为模糊遮罩，保护隐私。

## 🛠️ 技术栈 (Tech Stack)

- **UI**: 100% [Kotlin](https://kotlinlang.org/) & [Jetpack Compose](https://developer.android.com/jetpack/compose) 现代声明式 UI
- **架构**: MVVM (ViewModel + Repository)
- **数据库**: [Room](https://developer.android.com/training/data-storage/room) + [SQLCipher](https://www.zetetic.net/sqlcipher/) 实现本地数据库加密
- **异步处理**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & Flow

## 🚀 构建与运行 (Build and Run)

### 环境要求

- Android Studio Iguana | 2023.2.1 或更高版本
- JDK 17+

### 本地构建

克隆项目后，可直接在 Android Studio 中打开并运行。或使用 Gradle 命令行构建：

```bash
./gradlew :app:assembleDebug
```

如果你的电脑上安装了多个版本的 JDK，建议明确指定使用 JDK 17 或更高版本：
```bash
# 示例：使用 JDK 17
JAVA_HOME=`/usr/libexec/java_home -v 17` ./gradlew :app:assembleDebug
```

## ⚙️ 个性化配置 (Configuration)

### 天气接口

应用内的“设置”页面支持配置第三方天气 API Key，用于在补录日志时自动获取历史天气。

- **支持服务**:
    - 高德天气 Key (加密存储)
    - 和风天气 Key (加密存储)
- **获取策略**:
    1.  优先使用高德天气 API。
    2.  当查询的日期是10天内的历史日期时，使用和风天气时光机 API。
    3.  若日期超过10天，则需要用户手动填写天气信息。

### Fork & 发布

如果你希望 Fork 本项目并发布自己的版本，请务必：
1.  在 `app/build.gradle.kts` 中修改 `applicationId`。
2.  替换为你自己的应用名称和图标。

## 🤝 贡献 (Contributing)

欢迎对此项目进行贡献！你可以通过以下方式参与：

- 报告 Bug 或提出功能建议 (Issues)
- 提交代码改进 (Pull Requests)

## 📄 许可证 (License)

本项目采用 [Apache License, Version 2.0](LICENSE) 开源许可证。
