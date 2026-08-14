# QA 项目代码下拉选择补强

## Task Goal

在已支持上次选择恢复和复制的基础上，确保 QA 规程配置页头部项目代码控件仍是可下拉选择、可远程搜索的 `el-select`，不是纯文本输入框。

## Milestones

- [x] 记录 BDD 场景和任务约束。
- [x] 更新专用静态合同并完成 RED。
- [x] 补强项目代码控件下拉触发与远程后缀显示。
- [x] 跑通 GREEN、相邻回归和差异检查。
- [ ] 类型检查、提交和推送。

## Expected Verification

- `node tests/e2e/qa-regulation-project-last-copy-static.spec.cjs`
- `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs`
- `node tests/e2e/qa-regulation-publish-tab-hidden-static.spec.cjs`
- `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-project-last-copy-static.spec.cjs doc/tasks/20260806-qa-project-selector-dropdown-copy`

## Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：全量 `pnpm ts:check` 存在无关失败时，必须用当前任务专用静态契约完成 RED/GREEN，并记录剩余 blocker。
- `docs/frontend-development.md#复合输入控件交互保留门禁`：给下拉控件增加复制/回显能力时，必须证明原有下拉选择、远程搜索和候选渲染仍保留。

## Current Status

blocked

实现和目标静态验证已完成；`pnpm ts:check` 与推送被非本任务共享工作区状态阻塞。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；继续使用正式 DCC 项目代码接口和 `el-select`。
- `是否从根因和长期维护角度解决`：是；用静态合同锁定复制能力不能替代下拉选择能力。
- `是否存在临时补丁或绕过`：否。
