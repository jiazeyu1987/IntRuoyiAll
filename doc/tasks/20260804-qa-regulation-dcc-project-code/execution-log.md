# Execution Log

## User Intent

- QA 规程配置应是 QA 通用配置页面。
- 每个 DCC 项目代码对应一个产品。
- 当前样例对应“按压式球囊扩充压力泵”，但不能把该产品写死为页面结构。
- 用户要求进行修改。

## Preflight

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md` 和 `docs/powershell-memory.md`。
- 已读取 `frontend-feature-delivery` 技能和 `references/frontend-contract.md`。
- 已核对 `DccProjectCodeDO`：正式字段包括 `id`、`productMasterId`、`projectName`、`projectCode` 和 `status`。
- 已核对 `QaRegulationPage.vue`：当前页面硬编码压力泵规程来源、产品名称和检验项目初始化数据。
- 已核对 `MesQaInspectionRegulationDO`：当前正式 QA 保存模型尚无 `dccProjectCodeId`，且页面提示正式保存/发布接口未接入。

## Baseline Evidence

- `git status --short --branch` -> 工作区存在多个其它任务的 tracked、staged 和 untracked 改动。
- `git commit -m "Baseline: preserve existing worktree changes before QA regulation update"` -> PASS。
- Baseline commit: `516ef63a1`。
- 基线提交后出现的并行改动：
  - `IntRuoyiFronted/tests/e2e/edhr-batch-page-graph-tab-static.spec.js`
  - `doc/tasks/20260804-mes-item-route-selection/execution-log.md`
  - `doc/tasks/20260804-mes-item-route-selection/verification-report.md`
- 上述并行改动不属于本任务，不修改、不暂存。

## BDD And TDD

BDD: QA 按 DCC 项目代码确定产品范围 -> Given DCC 中存在启用的项目代码且每个项目代码对应一个产品 / When QA 在规程配置页选择项目代码 / Then 页面必须只读展示项目代码、项目名称和产品主数据关系，并将所选项目作为规程范围

BDD: 未选择 DCC 项目代码时阻塞发布 -> Given QA 尚未选择正式 DCC 项目代码 / When 执行发布前检查 / Then 页面必须提示项目范围未完成且不能把固定压力泵数据视为有效配置

BDD: DCC 项目代码读取失败时显式报错 -> Given DCC 项目代码接口返回错误 / When 页面加载项目选项 / Then 页面必须显示可见错误且不得切换到压力泵示例或默认项目

## Milestone Evidence

- M1 completed：现有 DCC 项目 API `getProjectCodePage` 可直接提供项目选择列表；页面无需新增 mock 或跨模块临时数据源。
- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL，首个失败为 `Standalone QA page must use a DCC project selector as the formal product scope.`，证明旧页面仍使用固定压力泵来源卡片。
- M2 completed：BDD 和专用静态契约 RED 已记录。
