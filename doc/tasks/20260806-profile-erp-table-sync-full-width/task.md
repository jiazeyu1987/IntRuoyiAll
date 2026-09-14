# ERP 表格自动同步区域全宽

## Task Goal

按用户截图要求，将 `ERP表格自动同步` 区域从黄框的小区域扩展到红框的配置页签可用宽度，并让列表区域跟随父容器变大。

## Milestones

- [x] 记录 BDD/TDD 合同，让静态合同先对旧 `1080px` 宽度限制 RED。
- [x] 修改 Profile ERP 自动同步组件样式，移除卡片最大宽度限制并保持表格全宽。
- [ ] 运行目标静态合同、相邻回归和可执行类型检查。
- [ ] 归档验证报告并完成任务收尾。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js`
- `pnpm ts:check` in `IntRuoyiFronted`
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-full-width`

## Current Status

blocked

## Current Blocker

- `pnpm ts:check` 当前失败在无关共享改动 `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`，缺少 `productionStageStyle` 模板绑定方法。
- 本任务目标文件的静态合同、相邻 NAS 合同和 scoped `git diff --check` 已通过；在无关前线模板页面类型错误修复前，不能按项目规则标记完整收尾。

## BDD Scenarios

- BDD: ERP sync card uses available config width -> Given 用户打开配置页签的 ERP 表格自动同步, When 页面渲染, Then ERP 同步卡片占满配置页签可用宽度，而不是停留在旧黄框宽度。
- BDD: ERP sync table expands with card -> Given ERP 同步卡片变宽, When 列表渲染, Then ERP 表格列表宽度跟随卡片变大。
- BDD: ERP sync layout preserves behavior -> Given 区域宽度调整, When 用户保存配置或点击手动同步, Then 原有同步配置和正式 Job 同步链路不变。

## TDD Sequence

- RED: 更新 `profile-erp-table-auto-sync-static.spec.js`，禁止 `.profile-erp-table-sync` 保留 `max-width: 1080px`，要求 `width: 100%` 和 `max-width: none`。
- GREEN: 修改 `ProfileErpTableAutoSyncSetting.vue` 样式，让卡片和表格使用可用宽度。
- REGRESSION: 复跑 Profile ERP 合同、NAS 页签合同和可执行类型检查。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；只调整布局样式。
- `是否从根因和长期维护角度解决`：是；移除组件自身固定最大宽度限制，让布局跟随配置页签容器。
- `是否存在临时补丁或绕过`：否。
