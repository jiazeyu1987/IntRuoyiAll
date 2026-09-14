# ERP 表格自动同步最近执行时间口径修正

## Task Goal

修正个人工作台配置页签中 `ERP表格自动同步` 主表的时间口径：用户看到的时间应表示最近一次同步任务执行时间，不再展示内部增量位置，也不出现用户难理解的内部术语。

## Milestones

- [x] M1 核对截图反馈与现有页面取值来源
- [x] M2 BDD/TDD 静态合同 RED
- [x] M3 前端实现最近执行时间与文案修正
- [x] M4 目标静态合同、相邻回归与类型检查
- [x] M5 收尾清理与最终记录

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js`
- `pnpm ts:check`（工作目录：`IntRuoyiFronted`）
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-execution-time-copy`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-profile-erp-table-sync-execution-time-copy/frontend-feature-evidence.md`

## Applicable Gates

- ERP 表格同步 Job 链路门禁：继续使用 `ErpKingdeeSyncApi.getRunPage`、`ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)` 与 `infra/job` 正式链路。
- 业务运行记录用户可读展示门禁：时间列必须来自正式运行记录，使用项目统一时间格式化，不直出内部状态或字段名。
- 前端文案标准：用户可见文案使用清楚自然的简体中文，避免内部技术术语。
- 前端静态契约隔离门禁：通过聚焦静态合同完成 RED/GREEN，不把无关工作区脏改动计入本任务。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，主表改为读取正式运行记录的 `endedAt/startedAt`，文案改为用户可理解的执行时间。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

目标静态合同、相邻 NAS 合同、diff 检查、证据 validator、`pnpm ts:check` 和 cleanup preview/apply 均已通过。
