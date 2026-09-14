# Execution Log

## User Intent

- 用户要求 `ERP表格同步采用类似生产工单这样的表格同步ERP的表单的方式`。
- 目标是修复个人工作台配置页签内 ERP 表格自动同步继续调用旧 `/erp/kingdee-table-auto-sync/**` 导致显示 `[ERP 模块 yudao-module-erp - 已禁用]` 的问题。

## Evidence Reviewed

- `IntRuoyiFronted/src/views/mes/pro/workorder/index.vue`：生产工单通过 `ErpKingdeeSyncApi.runIncrementalSyncJob('kingdeeProductionOrderSyncJob')` 提交同步 Job。
- `IntRuoyiFronted/src/api/erp/sync/index.ts`：`runIncrementalSyncJob` 先 `JobApi.getJobPage({ handlerName })`，再 `JobApi.runJob(job.id)`。
- `IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue`：旧实现调用 `@/api/erp/kingdeeTableAutoSync`。
- `docs/experience-index.md`：命中前端静态契约隔离门禁、业务运行记录用户可读展示门禁、技能证据文件清理前归档门禁。

## BDD

- BDD: Profile ERP table sync loads formal jobs -> Given 用户打开个人工作台配置页签的 ERP 表格自动同步, When 页面加载配置, Then 页面按 7 个正式 handlerName 查询 `infra/job` 并显示 Job 状态、水位和执行记录，且不访问 `/erp/kingdee-table-auto-sync/**`。
- BDD: Profile ERP table sync saves daily schedule -> Given 用户选择每日开始时间和 ERP 表格, When 点击保存配置, Then 系统更新对应正式 Job 的 cron，并启用所选 Job、停用未选 Job。
- BDD: Profile ERP table sync submits selected jobs once -> Given 用户已选择需要同步的 ERP 表格, When 点击立即执行一次, Then 系统通过 `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)` 提交正式增量同步任务。
- BDD: Missing sync job fails fast -> Given 任一 ERP 同步 handlerName 没有正式 Job, When 页面加载或保存, Then 页面显示 `未找到同步任务处理器`，不得用旧接口或默认成功掩盖。

## TDD Evidence

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL, expected reason: 旧组件仍导入 `@/api/erp/kingdeeTableAutoSync`，缺少 `ErpKingdeeSyncApi`、`JobApi` 和 `InfraJobStatusEnum` 正式 Job 同步链路。
- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` in `IntRuoyiFronted` -> PASS。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\erp-manual-incremental-sync-buttons-static.spec.js` in `IntRuoyiFronted` -> PASS。
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-profile-erp-table-sync-use-job-api/frontend-feature-evidence.md` -> PASS，`Frontend feature evidence is valid.`

## Experience Consolidation

- `docs/frontend-development.md` 新增 `ERP 表格同步 Job 链路门禁`。
- `docs/experience-index.md` 新增 `ERP表格自动同步`、`kingdee-table-auto-sync`、`runIncrementalSyncJob`、`JobApi.updateJobStatus` 等关键词路由。
- `rg -n "ERP表格自动同步|erp-表格同步-job-链路门禁|kingdee-table-auto-sync" docs\experience-index.md docs\frontend-development.md` -> PASS。

## Cleanup

- PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-profile-erp-table-sync-use-job-api --mode preview` -> PASS，keep `task.md`、`execution-log.md`、`verification-report.md`，delete `frontend-feature-evidence.md`，blocked `<none>`。
- APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-profile-erp-table-sync-use-job-api --mode apply` -> PASS，deleted `frontend-feature-evidence.md`。
- DIFF: `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-use-job-api docs/frontend-development.md docs/experience-index.md` -> PASS，仅 Git 换行提示，无 whitespace 错误。

## Current Notes

- 当前工作区已有大量无关脏改动和 `int_main` ahead 1；本任务只修改 Profile ERP 自动同步组件、对应静态合同、前端经验门禁和本任务文档。
