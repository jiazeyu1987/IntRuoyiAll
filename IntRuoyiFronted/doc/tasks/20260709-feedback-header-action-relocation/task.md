# Task: 生产报工顶部按钮位置调整

## 任务目标

根据截图要求调整 `生产报工` 页面头部布局：删除蓝框内独立页签头部区域，将黄框内的页面级操作按钮移动到红框所示的筛选行右侧位置。

用户复核后的补充要求：删除绿框内的 `新增` 和 `模拟报工` 按钮，正式报工筛选行只保留 `第三方导入`、`导出`、`重置`。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，命令不使用 `&&`，中文读写显式 UTF-8。
- 项目经验索引：已读取 `docs/experience-index.md`，本任务命中 PowerShell、前端页面 / 表格 / 样式门禁。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次按运维控制台风格做紧凑工具栏调整，不做无关视觉重设计。
- 前端交付合同：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，按 BDD + RED/GREEN 记录证据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。仅调整现有按钮位置与页签展示，不改变接口、权限或错误处理。
- 是否从根因和长期维护角度解决：是。将页面级操作收敛到列表筛选工具栏右侧，移除截图中多余的独立顶部栏。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 顶部独立页签栏被移除 -> Given 用户打开生产报工页面 / When 查看页面顶部 / Then 不再显示截图蓝框中的独立 `ContentWrap` 页签区域。
- BDD: 正式报工操作按钮位于筛选行红框位置 -> Given 用户停留在正式报工页签 / When 查看筛选工具栏 / Then `第三方导入`、`导出` 位于 `查询` 与 `重置` 之间的红框空白区。
- BDD: 删除绿框按钮 -> Given 用户停留在正式报工页签 / When 查看筛选工具栏 / Then 不再显示绿框内的 `新增` 和 `模拟报工` 按钮。
- BDD: 待归属视图保留导入操作 -> Given 页面因导入成功进入待归属视图 / When 查看待归属筛选行 / Then 仍保留第三方导入、模拟报工入口。

## 里程碑

- [x] M1：创建任务记录并读取经验门禁。
- [x] M2：补前端 RED 静态布局契约测试。
- [x] M3：移动操作按钮并移除独立顶部栏。
- [x] M4：运行定向验证并更新证据。
- [x] M5：隔离提交本任务改动。

## 预期验证

- `node tests/e2e/mes-feedback-header-action-relocation-static.spec.js`
- `node --check tests/e2e/mes-feedback-header-action-relocation-static.spec.js`
- `node node_modules/eslint/bin/eslint.js src/views/mes/pro/feedback/index.vue`

## 当前状态

Status: completed

completed：已删除独立顶部页签/按钮栏；正式报工筛选行已删除绿框内的 `新增` 和 `模拟报工`，仅保留 `第三方导入`、`导出`、`重置`；待归属视图保留第三方导入与模拟报工入口；定向静态回归、语法检查和 ESLint 均通过。

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260709-feedback-header-action-relocation/task.md`
- `doc/tasks/20260709-feedback-header-action-relocation/execution-log.md`
- `doc/tasks/20260709-feedback-header-action-relocation/frontend-feature-evidence.md`
- `tests/e2e/mes-feedback-header-action-relocation-static.spec.js`
