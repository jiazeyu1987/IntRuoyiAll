# 执行日志

## 用户意图

- 2026-08-07：用户要求删除截图中一线生产工作台的“班组配置”页签。

## BDD

- BDD: 班组配置页签从模块导航中移除 -> Given 用户打开一线生产工作台，When 页面渲染顶部模块导航，Then 不显示“班组配置”页签，且其它模块页签仍保持可用。
- BDD: 班组配置页面分支不可再切换 -> Given 前端模块页签状态完成初始化，When 用户在可见页签间切换，Then 状态类型和渲染分支均不再包含班组配置入口。

## 命令意图与证据

- 已读取 `docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md` 和 `docs/powershell-memory.md`。
- `docs/experience-index.md` 将本任务路由到 `docs/frontend-development.md#前端角色内容页签拆分口径门禁`；已将重复页签组、状态 gate、相邻角色回归要求摘入 `task.md`。
- Git 初始状态：`int_main...origin/int_main [ahead 2]`，存在 1 个既有后端测试改动和 2 个其它未跟踪任务目录；本任务目录创建后将排除于既有脏改动基线提交。
- 基线提交：`2ca0ec3bd chore: baseline concurrent changes before team tab removal`。提交发生时共享工作区有并发暂存变化，最终提交包含既有后端测试、其它任务文档以及本任务初始 3 个文档；未改写历史，后续只选择性暂存本任务实现和记录。
- 基线提交后仍出现其它任务的 staged/untracked 文档，本任务不修改、不清理这些并发文件。
- 页面入口：`src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue` 通过 `leader-type="PRODUCTION"` 和 `show-production-module-tabs="true"` 复用 `TeamLeaderWorkbenchPage.vue`。
- 影响边界：`TeamLeaderWorkbenchPage.vue` 中 7 组重复生产模块页签、`activeProductionModuleTab` 类型、`showProductionConfigModule` gate，以及角色矩阵真实路径中的班组配置页签步骤；无后端 API 变更。

## TDD Evidence

- RED: `node tests/e2e/production-leader-function-tabs-static.spec.js` -> FAIL，预期原因：现有 7 组生产模块导航仍包含 `<el-tab-pane label="班组配置" name="config">`，活动页签类型和内容 gate 仍保留 `config`。

## 里程碑状态

- M1：completed；已确认独立生产组长页面使用组件内部功能模块页签，班组配置内容在非模块组合工作台仍有独立使用场景。
- M2：in_progress；聚焦合同已取得预期 RED。
- M3：pending。
- M4：pending。

## 阻塞项

- 无。
