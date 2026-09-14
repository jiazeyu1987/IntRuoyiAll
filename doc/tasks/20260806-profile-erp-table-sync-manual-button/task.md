# ERP 表格手动同步按钮

## Task Goal

在 Profile 配置页签的 `ERP表格自动同步` 表格中增加每行 `手动同步` 按钮，让用户可以对单个 ERP 表格立即触发正式增量同步，并刷新最近同步时间、同步成功/失败和失败原因。

## Milestones

- [x] 记录 BDD/TDD 合同，让静态合同先对缺少每行手动同步按钮 RED。
- [x] 修改 Profile ERP 自动同步组件，增加列表级操作列和单表手动同步处理。
- [ ] 运行目标静态合同、相邻回归和可执行类型检查。
- [ ] 归档验证报告并完成任务收尾。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js`
- `pnpm ts:check` in `IntRuoyiFronted`
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-manual-button`

## Current Status

blocked

## Current Blocker

- `pnpm ts:check` 当前失败在无关共享改动 `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`，缺少多个 `resolvePqc...` 模板绑定方法。
- 本任务目标文件的静态合同、相邻 NAS 合同和 scoped `git diff --check` 已通过；在无关 MES 页面类型错误修复前，不能按项目规则标记完整收尾。

## BDD Scenarios

- BDD: ERP table sync supports row manual sync -> Given 用户查看 ERP 表格自动同步列表, When 用户点击某一行的 `手动同步`, Then 系统只提交该行 ERP 表格对应 handler 的正式增量同步任务。
- BDD: ERP table sync refreshes row result after manual sync -> Given 用户触发某个 ERP 表格手动同步, When 提交成功, Then 页面刷新最近同步时间、同步成功/失败和失败原因列。
- BDD: ERP table sync exposes manual sync failures -> Given 手动同步提交失败, When 后端或 Job API 返回错误, Then 页面通过 `ElMessage.error` 暴露错误，不显示默认成功。

## TDD Sequence

- RED: 更新 `profile-erp-table-auto-sync-static.spec.js`，要求表格存在 `操作` 列和 `手动同步` 按钮，并锁定 `handleRunSingle` 使用正式 `runIncrementalSyncJob(row.handlerName)`。
- GREEN: 修改 `ProfileErpTableAutoSyncSetting.vue`，增加每行手动同步按钮、行级 loading 和结果刷新。
- REGRESSION: 复跑 Profile ERP 合同、NAS 页签合同和可执行类型检查。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；手动同步失败直接暴露错误，不伪装成功。
- `是否从根因和长期维护角度解决`：是；复用正式 `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)` 链路。
- `是否存在临时补丁或绕过`：否。
