# Verification Report

## Summary

- Profile `ERP表格自动同步` 已从旧 `/erp/kingdee-table-auto-sync/**` 改为正式 `infra/job` + `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)` 链路。
- 页面现在可以在同一入口选择每日开始时间、选择需要自动同步的 ERP 表格、启停对应 Job，并对选中表格立即提交增量同步 Job。
- 旧 ERP 模块禁用提示的根因已消除：组件不再导入或调用 `@/api/erp/kingdeeTableAutoSync`。

## Verification Commands

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL，旧组件缺少正式 Job 同步链路。
- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\erp-manual-incremental-sync-buttons-static.spec.js` in `IntRuoyiFronted` -> PASS。
- TYPECHECK: `pnpm ts:check` in `IntRuoyiFronted` -> PASS。
- EXPERIENCE: `rg -n "ERP表格自动同步|erp-表格同步-job-链路门禁|kingdee-table-auto-sync" docs\experience-index.md docs\frontend-development.md` -> PASS。
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-profile-erp-table-sync-use-job-api/frontend-feature-evidence.md` -> PASS。

## Acceptance Result

- PASS: 7 个正式 ERP 同步 handlerName 均在 Profile 组件中配置。
- PASS: 保存配置调用 `JobApi.getJobPage`、`JobApi.updateJob`、`JobApi.updateJobStatus`。
- PASS: 立即执行调用 `ErpKingdeeSyncApi.runIncrementalSyncJob`。
- PASS: 运行记录和水位使用正式 `/erp/kingdee-sync/**` API。
- PASS: 静态合同禁止旧 `kingdee-table-auto-sync` 接口。
- PASS: 运行记录继续显示中文状态、中文触发类型和可读时间。

## Notes

- 本任务没有修改后端同步 Job、数据库 schema 或权限菜单。
- Cleanup preview/apply 已完成，临时 `frontend-feature-evidence.md` 已删除，核心 `task.md`、`execution-log.md`、`verification-report.md` 已保留。
- 当前共享工作区仍有大量无关脏改动；未执行提交、融合或清理 worktree。
