# ERP 表格同步状态列

## Task Goal

按用户截图要求，在 Profile 配置页签的 `ERP表格自动同步` 列表中增加 `同步成功/失败` 和 `失败原因` 两列，让每个 ERP 表格行直接看到最近一次同步运行结果和失败信息。

## Milestones

- [x] 记录 BDD/TDD 合同，让静态合同先对缺少两列 RED。
- [x] 修改 Profile ERP 自动同步组件，把最近运行记录按 `syncType` 聚合进列表行。
- [ ] 运行目标静态合同、相邻回归和可执行类型检查。
- [ ] 归档验证报告并完成任务收尾。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js`
- `pnpm ts:check` in `IntRuoyiFronted`
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-status-columns`

## Current Status

blocked

## Current Blocker

- `pnpm ts:check` 当前失败在无关共享改动 `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`，缺少多个 `resolvePqc...` 模板绑定方法。
- 本任务目标文件的静态合同、相邻 NAS 合同和 scoped `git diff --check` 已通过；在无关 MES 页面类型错误修复前，不能按项目规则标记完整收尾。

## BDD Scenarios

- BDD: ERP table sync shows latest status -> Given 用户打开 Profile 配置页签的 ERP 表格自动同步列表, When 最近运行记录已加载, Then 每个 ERP 表格行显示最近一次同步是成功、失败、运行中或未执行。
- BDD: ERP table sync shows failure reason -> Given 某个 ERP 表格最近一次同步失败且返回失败原因, When 用户查看该行, Then `失败原因` 列展示该失败原因，不再需要展开最近执行记录表。
- BDD: ERP table sync preserves concise table -> Given 用户要求只保留 ERP 列表视图, When 页面渲染, Then 页面不恢复 `最近执行记录` 独立表格或 Job 调度表。

## TDD Sequence

- RED: 更新 `profile-erp-table-auto-sync-static.spec.js`，要求列表列出 `同步成功/失败`、`失败原因`，并使用 `getRunPage` 聚合最近运行状态。
- GREEN: 修改 `ProfileErpTableAutoSyncSetting.vue`，加载最近运行记录并按 `syncType` 合并到 `syncTableRows`。
- REGRESSION: 复跑 Profile ERP 合同、NAS 页签合同和可执行类型检查。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；无运行记录时仅明确显示未执行，不把未知状态伪装成成功。
- `是否从根因和长期维护角度解决`：是；复用正式 `/erp/kingdee-sync/run/page` 运行记录数据源，按 `syncType` 归并到当前列表。
- `是否存在临时补丁或绕过`：否。
