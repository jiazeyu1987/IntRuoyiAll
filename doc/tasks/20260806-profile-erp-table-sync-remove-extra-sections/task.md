# ERP 表格自动同步删除额外展示区

## Task Goal

按用户截图要求删除 `ERP表格自动同步` 页面中的汇总描述区、`Job 调度` 表格和 `最近执行记录` 表格，只保留自动同步开关、每日开始时间、ERP 表格选择列表、保存配置和立即执行一次。

## Milestones

- [x] 记录 BDD/TDD 合同，让静态合同先对截图中的额外展示区 RED。
- [x] 修改 Profile ERP 自动同步组件，移除汇总区、Job 调度区和最近执行记录区。
- [ ] 运行目标静态合同、相邻回归和类型检查。
- [ ] 归档验证报告并完成任务收尾。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js`
- `pnpm ts:check` in `IntRuoyiFronted`
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-remove-extra-sections`

## Current Status

blocked

## Current Blocker

- `pnpm ts:check` 当前失败在无关共享改动 `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`，缺少 `finalInspectionRequired`、`finalInspectionNotApplicableReason`、`qaRulesQuery`。
- 本任务目标文件的静态合同、相邻 NAS 合同和 scoped `git diff --check` 已通过；在无关 QA 页面类型错误修复前，不能按项目规则标记完整收尾。

## BDD Scenarios

- BDD: ERP table sync hides summary panel -> Given 用户打开 Profile 配置页签的 ERP 表格自动同步, When 页面渲染, Then 页面不显示配置来源、已选表格、每日 Cron、启用 Job、最近状态、最近开始时间汇总区。
- BDD: ERP table sync hides job schedule details -> Given 用户只需要配置同步表格和时间, When 页面渲染, Then 页面不显示 Job 调度明细表、处理器、Job ID、Job 状态、当前 Cron 等内部调度信息。
- BDD: ERP table sync hides recent run records -> Given 页面保留 ERP 表格列表的最近一次同步时间, When 页面渲染, Then 页面不再显示最近执行记录表和失败原因列。

## TDD Sequence

- RED: 更新 `profile-erp-table-auto-sync-static.spec.js`，禁止截图中的汇总、Job 调度和最近执行记录区。
- GREEN: 修改 `ProfileErpTableAutoSyncSetting.vue`，删除额外展示区及未使用运行记录状态代码。
- REGRESSION: 复跑 Profile ERP 合同、NAS 页签合同和 `pnpm ts:check`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；只删除展示区，不改变正式 Job/API 错误处理。
- `是否从根因和长期维护角度解决`：是；按用户要求收敛页面信息密度，保留必要配置入口。
- `是否存在临时补丁或绕过`：否。
