# Verification Report

## Scope

- 删除范围：`ERP表格自动同步` 页面截图中的顶部汇总区、`Job 调度` 明细区、`最近执行记录` 明细区。
- 保留范围：`启用自动同步`、`每日开始时间`、ERP 表格选择列表、`保存配置`、`立即执行一次`。

## Result

- 页面结构已收敛为配置表单和 ERP 表格列表。
- ERP 表格列表仍显示 `ERP表格名称`、`本地页签名称`、`最近一次同步时间`。
- 正式同步链路仍使用 `infra/job` 和 `ErpKingdeeSyncApi.runIncrementalSyncJob`，未恢复旧 `/erp/kingdee-table-auto-sync/**` 接口。

## Verification Evidence

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- `pnpm ts:check` in `IntRuoyiFronted` -> FAIL/BLOCKED；失败点在无关共享改动 `src/views/mes/pro/processpool/QaRegulationPage.vue`。

## Blockers

- 全量类型检查当前被无关 QA 规程页面类型错误阻塞：
- `finalInspectionRequired` does not exist on component instance.
- `finalInspectionNotApplicableReason` does not exist on component instance.
- `qaRulesQuery` cannot be found.
- 本任务不修改该 QA 页面，以避免混入并行任务范围。
