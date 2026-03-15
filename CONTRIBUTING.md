# 贡献指南

感谢你对装修日记项目的关注！我们欢迎所有形式的贡献。

## 如何贡献

### 1. Fork 本项目
点击右上角的 [Fork](https://github.com/YOUR_USERNAME/construction-log/fork) 按钮

### 2. Clone 你的 Fork
```bash
git clone https://github.com/YOUR_USERNAME/construction-log.git
cd construction-log
```

### 3. 创建分支
```bash
git checkout -b feature/your-feature-name
# 或修复 bug
git checkout -b fix/your-bug-fix
```

### 4. 进行修改
- 遵循现有代码风格
- 添加必要的注释
- 更新相关文档
- 确保代码通过编译

### 5. 提交更改
```bash
git add .
git commit -m "feat: 添加XX功能"
# 或
git commit -m "fix: 修复XX问题"
# 或
git commit -m "docs: 更新XX文档"
```

### 6. 推送到你的 Fork
```bash
git push origin feature/your-feature-name
```

### 7. 创建 Pull Request
在 GitHub 上创建 Pull Request，我们会审核代码

## 代码规范

### Kotlin 代码风格
- 使用 [KtLint](https://pinterest.github.io/ktlint/) 格式化代码
- 遵循 [Kotlin 官方规范](https://kotlinlang.org/docs/coding-conventions.html)
- 函数命名：`camelCase`
- 类命名：`PascalCase`
- 常量命名：`UPPER_SNAKE_CASE`
- 包命名：全小写，使用点分隔

### 文档规范
- 所有公开 API 添加 KDoc 注释
- 复杂逻辑添加行内注释
- 更新 CHANGELOG.md

### 测试规范
- 单元测试覆盖率 ≥ 60%
- UI 测试使用 Compose Testing
- 使用 Android Studio 的 Test Runner

### 命名规范
- Activity/Fragment：以 `Screen` 结尾
- ViewModel：以 `ViewModel` 结尾
- Repository：以 `Repository` 结尾
- Dao：以 `Dao` 结尾
- Entity：以 `Entity` 结尾

## 开发环境

- **Android Studio**: Iguana 2023.2.1 或更高版本
- **JDK**: 17 或更高版本
- **Gradle**: 8.5 或更高版本
- **Kotlin**: 1.9.20 或更高版本

### 本地构建

```bash
# 清理构建
./gradlew clean

# 构建 Debug 版本
./gradlew :app:assembleDebug

# 构建 Release 版本
./gradlew :app:assembleRelease
```

## 提交流程

### 提交信息规范

使用语义化提交信息：

- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式（不影响代码运行）
- `refactor`: 重构（既不是新增功能，也不是修复 bug）
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具链相关

#### 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### 示例

```bash
# 新功能
git commit -m "feat: 添加图片懒加载功能"

# 修复 bug
git commit -m "fix: 修复备份恢复失败问题"

# 文档更新
git commit -m "docs: 更新 README.md"

# 重构
git commit -m "refactor: 重构数据层，使用 Flow 替代 LiveData"

# 性能优化
git commit -m "perf: 优化列表滚动性能，减少卡顿"

# 测试
git commit -m "test: 添加 ViewModel 单元测试"

# 构建工具
git commit -m "chore: 升级 Gradle 到 8.5"
```

### 提交前检查清单

- [ ] 代码通过编译
- [ ] 代码格式化（使用 KtLint）
- [ ] 添加必要的注释和 KDoc
- [ ] 更新相关文档
- [ ] 更新 CHANGELOG.md
- [ ] 添加单元测试（如果有改动逻辑）
- [ ] 在本地运行测试通过

## 行为准则

- 尊重所有贡献者
- 保持友好和建设性
- 专注于代码本身
- 不进行人身攻击
- 接受建设性反馈

## 问题反馈

- 报告 Bug：[GitHub Issues](https://github.com/YOUR_USERNAME/construction-log/issues)
- 功能建议：[GitHub Discussions](https://github.com/YOUR_USERNAME/construction-log/discussions)

## 许可证

贡献的代码将采用 Apache License 2.0 许可证。

## 贡献者

感谢所有贡献者！

## 联系方式

如有任何问题，欢迎通过以下方式联系：

- 提交 Issue
- 发送 Pull Request
- 在 GitHub Discussions 中讨论

---

**再次感谢你的贡献！** 🎉
