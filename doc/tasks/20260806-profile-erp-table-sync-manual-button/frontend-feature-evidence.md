# Frontend Feature Evidence

## Feature

- Goal: 在 Profile 配置页签的 `ERP表格自动同步` 表格中增加每行 `手动同步` 按钮。
- Non-goal: 不新增后端接口，不调整 Job 配置，不恢复旧 `/erp/kingdee-table-auto-sync/**` 接口，不修改 MES 并行页面。
- Entry point: `Profile` 页面配置页签下的 `ERP表格自动同步` 组件。
- Owned files: `IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue`、`IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js`。

## Acceptance

- AC1: ERP 表格列表显示 `操作` 列。
- AC2: 每行显示 `手动同步` 按钮。
- AC3: 点击某行 `手动同步` 只调用该行 `handlerName` 对应的 `ErpKingdeeSyncApi.runIncrementalSyncJob(row.handlerName)`。
- AC4: 手动同步成功后刷新最近同步时间、同步成功/失败和失败原因。
- AC5: 手动同步失败通过错误提示暴露，不伪装成功。

## API Contracts

- `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)` 是正式 ERP 增量同步触发入口。
- `ErpKingdeeSyncApi.getWatermarkList()` 用于刷新最近一次同步时间。
- `ErpKingdeeSyncApi.getRunPage({ pageNo: 1, pageSize: 1, syncType })` 用于刷新最近一次运行结果。

## BDD

- BDD: ERP table sync supports row manual sync -> Given 用户查看 ERP 表格自动同步列表, When 用户点击某一行的 `手动同步`, Then 系统只提交该行 ERP 表格对应 handler 的正式增量同步任务。
- BDD: ERP table sync refreshes row result after manual sync -> Given 用户触发某个 ERP 表格手动同步, When 提交成功, Then 页面刷新最近同步时间、同步成功/失败和失败原因列。
- BDD: ERP table sync exposes manual sync failures -> Given 手动同步提交失败, When 后端或 Job API 返回错误, Then 页面通过 `ElMessage.error` 暴露错误，不显示默认成功。

## RED

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL, expected reason: 合同要求 `操作` 列和 `手动同步` 按钮后，组件尚未包含该用户可见操作列。

## GREEN

- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。

## Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-manual-button` -> PASS。
- `pnpm ts:check` in `IntRuoyiFronted` -> BLOCKED, unrelated current workspace errors in `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`.
- Loading state: 单行按钮使用 `manualSyncingType` 显示行级 loading。
- Error state: 手动同步失败通过 `ElMessage.error` 暴露。

## Blockers

- `pnpm ts:check` 被无关 MES 班组长页面类型错误阻塞：多个 `resolvePqc...` 模板绑定方法缺失。
