# Frontend Feature Evidence

## Feature

- Goal: 在 Profile 配置页签的 `ERP表格自动同步` 列表中增加 `同步成功/失败` 和 `失败原因` 两列。
- Non-goal: 不恢复独立 `最近执行记录` 表，不调整后端接口、权限、Job 配置或数据库 schema。
- Entry point: `Profile` 页面配置页签下的 `ERP表格自动同步` 组件。
- Owned files: `IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue`、`IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js`。

## Acceptance

- AC1: ERP 表格列表显示 `同步成功/失败` 列。
- AC2: ERP 表格列表显示 `失败原因` 列。
- AC3: 最近运行状态来自正式 `ErpKingdeeSyncApi.getRunPage`，按每个 `syncType` 查询最新一条运行记录。
- AC4: 状态以中文显示 `成功`、`失败`、`运行中`、`未执行` 或未知状态，不把未知状态伪装为成功。
- AC5: 页面不恢复独立 `最近执行记录` 表或 `Job 调度` 表。

## API Contracts

- `ErpKingdeeSyncApi.getRunPage({ pageNo: 1, pageSize: 1, syncType })` 返回最近运行记录。
- `ErpKingdeeSyncRunVO.status` 用于显示中文同步状态。
- `ErpKingdeeSyncRunVO.failureMessage` 用于显示失败原因。

## BDD

- BDD: ERP table sync shows latest status -> Given 用户打开 Profile 配置页签的 ERP 表格自动同步列表, When 最近运行记录已加载, Then 每个 ERP 表格行显示最近一次同步是成功、失败、运行中或未执行。
- BDD: ERP table sync shows failure reason -> Given 某个 ERP 表格最近一次同步失败且返回失败原因, When 用户查看该行, Then `失败原因` 列展示该失败原因，不再需要展开最近执行记录表。
- BDD: ERP table sync preserves concise table -> Given 用户要求只保留 ERP 列表视图, When 页面渲染, Then 页面不恢复 `最近执行记录` 独立表格或 Job 调度表。

## RED

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL, expected reason: 合同要求 `同步成功/失败` 列后，组件尚未包含该用户可见列。

## GREEN

- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。

## Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-status-columns` -> PASS。
- `pnpm ts:check` in `IntRuoyiFronted` -> BLOCKED, unrelated current workspace errors in `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`.
- Loading state: 表格 loading 覆盖 Job、水位和运行记录加载。
- Empty state: 无运行记录时状态显示 `未执行`，失败原因显示 `-`。
- Error state: 运行记录加载失败通过 `ElMessage.error` 暴露。

## Blockers

- `pnpm ts:check` 被无关 MES 班组长页面类型错误阻塞：多个 `resolvePqc...` 模板绑定方法缺失。
