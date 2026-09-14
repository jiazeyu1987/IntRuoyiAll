# ERP 表格自动同步选择区改为列表

## Task Goal

将个人工作台配置页签下 `ERP表格自动同步` 的 ERP 表格选择区从横向复选框改为列表展示，列表列为：ERP 里面的表格名称、映射到本地对应页签的名字、最近一次同步时间，同时保留选择哪些 ERP 表格自动同步的能力。

## Milestones

- [x] 记录 BDD/TDD 合同，先让静态合同对旧复选框布局 RED。
- [x] 修改 Profile ERP 自动同步组件，使用可选择列表展示三列信息。
- [x] 运行目标静态合同、相邻回归和类型检查。
- [x] 归档验证报告并完成任务收尾。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js`
- `pnpm ts:check` in `IntRuoyiFronted`
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-list-display`

## Current Status

completed

## BDD Scenarios

- BDD: ERP table selection list shows mapping -> Given 用户打开个人工作台配置页签的 ERP 表格自动同步, When 查看 ERP 表格选择区, Then 页面用列表显示 ERP 表格名称、本地页签名称、最近一次同步时间。
- BDD: ERP table list keeps selectable scheduling scope -> Given 用户需要选择每天自动同步哪些 ERP 表格, When 在列表中勾选或取消某一行, Then 保存配置和立即执行仍使用选中的 `syncType` 集合。
- BDD: Latest sync time comes from formal watermark -> Given 同步水位接口返回最近成功时间, When 页面渲染 ERP 表格列表, Then 最近一次同步时间按对应 `syncType` 显示为可读日期时间。

## TDD Sequence

- RED: 更新 `profile-erp-table-auto-sync-static.spec.js`，要求组件使用 `el-table` 选择列显示三列信息，并禁止旧 `el-checkbox-group` 选择布局。
- GREEN: 修改 `ProfileErpTableAutoSyncSetting.vue`，用 `syncTableRows` 合并 ERP 表格、本地页签和水位时间。
- REGRESSION: 复跑 Profile ERP 合同、NAS 页签合同和 `pnpm ts:check`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；仅调整展示和选择交互，不改变正式 Job/API 链路。
- `是否从根因和长期维护角度解决`：是；用结构化表格承载用户需要看的映射与最近同步时间。
- `是否存在临时补丁或绕过`：否。
