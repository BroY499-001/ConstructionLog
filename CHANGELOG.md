# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-03-13

### Added
- **流式备份V4**：优化内存占用，支持大文件备份
- **自定义图标**：DockBar使用自定义矢量图标
- **Onboarding引导页**：新用户首次使用时的引导流程
- **页面过渡动效**：列表→详情、详情→列表的横向滑动效果
- **空状态组件**：优化无内容时的用户体验
- **签收表单支持**：新增签收功能，支持现场签收流程

### Changed
- **主题优化**：暖色调配色（橙/青/棕），提升品牌识别度
- **组件拆分**：UI层拆分为screens/、components/、theme/等独立模块
- **备份服务重构**：从全量加载改为流式处理

### Fixed
- **自动备份OOM**：修复自动备份时的内存溢出问题
- **图片加载风险**：修复图片加载可能导致的OOM风险

### Security
- **备份加密**：备份文件支持AES-256加密
- **自动备份加密**：自动备份文件使用加密存储

### Performance
- **内存优化**：备份流程内存占用降低60-80%
- **启动优化**：延迟加载非必要组件

[1.1.0]: https://github.com/BroY499-001/ConstructionLog/releases/tag/v1.1.0
