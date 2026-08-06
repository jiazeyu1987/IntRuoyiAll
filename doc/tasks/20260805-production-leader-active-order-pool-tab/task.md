# 20260805 生产组长活跃订单池 Tab

## Task Goal

将生产组长页面的“活跃订单池”作为独立功能 Tab；Tab 内容使用统一标准列表模板展示全部活跃订单，并提供“新增活跃订单”按钮，通过正式接口新增活跃订单。

## Milestones

- [x] 识别生产组长页面、统一列表模板和现有活跃订单接口
- [x] 编写并运行聚焦 RED 静态合同
- [x] 实现活跃订单池 Tab、标准列表和新增对话框
- [x] 运行定向 GREEN、相邻回归、TypeScript 和只读真实页面验证
- [ ] 完成证据归档、清理、提交与推送

## Expected Verification

- `workdir=IntRuoyiFronted; node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`
- `workdir=IntRuoyiFronted; node tests/e2e/production-leader-function-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; node tests/e2e/production-leader-tabs-flat-style-static.spec.js`
- `workdir=IntRuoyiFronted; node tests/e2e/production-leader-remove-header-content-static.spec.js`
- `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `workdir=IntRuoyiFronted; node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js`
- `workdir=IntRuoyiFronted; node --check tests/e2e/team-leader-workbench-real-flow.e2e.js`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- 本机 `芋道源码/admin` 只读 Playwright：登录生产组长页面，打开“活跃订单池”Tab 和新增弹窗后取消，断言目标写请求为 `0`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md`
- `git diff --check`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，将活跃订单维护从班组配置职责中拆出，复用正式列表、加入和移出接口。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#统一列表复合工具栏布局门禁`：活跃订单池必须显式复用 `UnifiedListTemplate`，新增操作放在模板 actions 区域，不使用页面级临时工具栏替代标准模板。
- `docs/frontend-development.md#前端角色内容页签拆分口径门禁`：本需求是生产组长页面内部功能模块 Tab，不新增主导航路由或 PQC 组长入口。
- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用最小静态合同完成 RED/GREEN，并单独记录全量类型检查结果。
- `docs/powershell-memory.md`：提交前按脏工作区基线、选择性暂存和提交后残余改动门禁执行。

## Cleanup Candidates

- doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md
