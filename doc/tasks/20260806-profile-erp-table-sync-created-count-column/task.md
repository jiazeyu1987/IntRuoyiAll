# ERP 表格新增行数列

## Task Goal

在 Profile 配置页签的 `ERP表格自动同步` 表格中增加 `新增行数` 列，展示每个 ERP 表格最近一次同步运行记录的新增数量。

## Milestones

- [x] 记录 BDD/TDD 合同，让静态合同先对缺少 `新增行数` 列 RED。
- [x] 修改 Profile ERP 自动同步组件，增加列表列和新增行数展示函数。
- [x] 运行目标静态合同、相邻回归和可执行类型检查。
- [x] 归档验证报告并完成任务收尾。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js`
- `pnpm ts:check` in `IntRuoyiFronted`
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-created-count-column`

## Current Status

completed

## BDD Scenarios

- BDD: ERP table sync shows created row count -> Given 用户查看 ERP 表格自动同步列表, When 每个表格最近一次运行记录已加载, Then 列表显示该表格最近一次同步的新增行数。
- BDD: ERP table sync handles missing run count explicitly -> Given 某个 ERP 表格没有最近运行记录或运行记录缺少新增数量, When 页面渲染, Then `新增行数` 显示 `-`，不伪装成成功或默认新增。
- BDD: ERP table sync preserves existing status columns -> Given 新增行数列已加入, When 用户查看列表, Then 最近同步时间、同步成功/失败、失败原因和手动同步仍保留。

## TDD Sequence

- RED: 更新 `profile-erp-table-auto-sync-static.spec.js`，要求 `新增行数` 列和 `resolveCreatedCount(row.latestRun)`。
- GREEN: 修改 `ProfileErpTableAutoSyncSetting.vue`，从 `ErpKingdeeSyncRunVO.createdCount` 展示新增行数。
- REGRESSION: 复跑 Profile ERP 合同、NAS 页签合同和可执行类型检查。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；无运行记录或缺少新增数量时明确显示 `-`。
- `是否从根因和长期维护角度解决`：是；直接复用正式运行记录字段 `createdCount`。
- `是否存在临时补丁或绕过`：否。
