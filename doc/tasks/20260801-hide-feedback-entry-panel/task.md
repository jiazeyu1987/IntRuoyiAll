# 报工页隐藏一线填报面板

## Task Goal

让生产报工页面不再显示截图红框中的一线固定填报面板，只保留正式报工列表、筛选与导入归属相关功能。

## Milestones

- [x] 创建任务台账并记录 BDD 场景
- [x] 补充静态 RED 用例复现红框面板仍在报工页展示
- [x] 执行最小前端改动移除报工页面板挂载
- [x] 运行相关静态回归与前端功能证据校验
- [x] 更新验证报告与任务状态

## Expected Verification

- `src/views/mes/pro/feedback/index.vue` 的正式报工页不再导入或渲染 `FrontlineFixedTemplatePanel`。
- 正式报工 `UnifiedListTemplate`、筛选区和操作区仍存在。
- `edhr-batch` 下生产/PQC 独立填报页面继续使用 `FrontlineFixedTemplatePanel`。
- 报工页相关静态测试通过，且不引入 fallback、降级或吞异常。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是从报工页挂载点移除不应展示的面板，而不是用样式或权限临时遮挡。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`，命中截图红框/前端隐藏类门禁；本任务采用静态契约先 RED、再最小改动、最后回归验证。

## Verification Evidence

- RED：`node tests/e2e/mes-feedback-hide-frontline-panel-static.spec.js` 失败，确认报工页仍渲染 `FrontlineFixedTemplatePanel`。
- GREEN：报工页已移除 `FrontlineFixedTemplatePanel` 挂载和导入，正式报工列表仍保留。
- GREEN：新增隐藏面板静态契约、报工表头操作区契约、正式报工统一列表契约、eDHR 一线独立填报页契约均通过。
- GREEN：`validate_frontend_feature.py --evidence doc\tasks\20260801-hide-feedback-entry-panel\frontend-feature-evidence.md` 通过。
- CLEANUP：`task_closeout.py --task-id 20260801-hide-feedback-entry-panel --mode preview` -> ready，delete `<none>`，blocked `<none>`。
- CLEANUP：`task_closeout.py --task-id 20260801-hide-feedback-entry-panel --mode apply` -> applied，deleted_paths `<none>`。
- Project experience consolidation：已按 `project-experience-consolidation` 检索现有长期经验；本次“红框隐藏 + 保留共享组件其它入口”的经验已被 `docs/frontend-development.md` 中前端静态契约隔离和红框隐藏门禁覆盖，未新增长期经验文档。

## Cleanup Keep

- `doc/tasks/20260801-hide-feedback-entry-panel/frontend-feature-evidence.md`
