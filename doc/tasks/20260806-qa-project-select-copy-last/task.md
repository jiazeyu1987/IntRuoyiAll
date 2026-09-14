# QA DCC 项目选择记忆与复制

## Task Goal

让 QA 规程配置页头部的 DCC 项目选择框在再次打开页面时默认恢复上次选择的正式项目，并让当前显示的项目内容可以复制。

## Milestones

- [x] 记录 BDD 场景和任务约束。
- [x] 新增专用静态合同并完成 RED。
- [x] 实现正式 DCC 项目 ID 持久化恢复与复制交互。
- [x] 跑通 GREEN、相邻回归、类型检查和差异检查。
- [x] 完成收尾记录和清理。

## Expected Verification

- `node tests/e2e/qa-regulation-project-last-copy-static.spec.cjs`
- `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs`
- `node tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs`
- `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-project-last-copy-static.spec.cjs doc/tasks/20260806-qa-project-select-copy-last`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-qa-project-select-copy-last/frontend-feature-evidence.md`

## Applicable Gates

- 前端静态契约隔离门禁：本任务新增最小静态合同覆盖“上次选择恢复”和“复制”行为，避免被无关全量问题掩盖。
- QA 规程配置状态门禁：仍以正式 `productMasterId` 管理产品级草稿；本任务只恢复正式 DCC 项目 ID，不用项目名称、样例或缓存快照替代产品事实。
- Element Plus 表单值断言门禁：DCC 项目选择框是 Element Plus 控件，验证要针对控件绑定和复制按钮，不把输入框 value 误判为普通文本。
- Strict no-fallback：不使用本地缓存快照冒充正式项目；恢复时必须通过正式 API 或已加载正式候选验证项目仍启用。

## Current Status

completed

实现、验证和 cleanup 均已完成；提交和推送仍需处理共享工作区既有 ahead/脏改动边界。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；localStorage/剪贴板失败必须显示错误，不静默降级。
- `是否从根因和长期维护角度解决`：是；用正式 DCC 项目 ID 作为恢复键，避免复制展示文本或过期快照。
- `是否存在临时补丁或绕过`：否。
