# 20260805 生产组长活跃订单池 Tab

## Task Goal

将生产组长页面的“活跃订单池”作为独立功能 Tab；Tab 内容使用统一标准列表模板展示全部活跃订单，并提供“新增活跃订单”按钮。新增时只输入生产工单“订单号”，通过远程候选下拉选择真实生产工单编号，后端按唯一有效排产解析正式路线和路线版本。

## Milestones

- [x] 识别生产组长页面、统一列表模板和现有活跃订单接口
- [x] 编写并运行聚焦 RED 静态合同
- [x] 记录订单号加入需求变更、BDD 和 RED/GREEN 证据
- [x] 实现候选搜索端点、workOrderId-only 新增接口和服务端路线解析
- [x] 实现单字段远程下拉弹窗，并拆除新增动作中的调拨关联输入
- [x] 更新静态合同和真实 E2E 脚本，拆分调拨追溯只读验收
- [x] 修复未选择真实候选时加入活跃订单可能向后端提交空 `workOrderId` 的回归
- [x] 修复只输入完整订单号但未点候选时仍向后端提交空 `workOrderId` 的截图回归
- [ ] 完成写入型真实 E2E、证据归档、清理、提交与推送

## Expected Verification

- `workdir=IntRuoyiFronted; node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`
- `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `workdir=IntRuoyiFronted; node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js`
- `workdir=IntRuoyiFronted; node --check tests/e2e/team-leader-workbench-real-flow.e2e.js`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-real-flow.e2e.js`，必须使用测试生产组长和任务自有已确认工单；当前缺少 `TLW_*` 前置时记录 BLOCKED。
- `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs/changes/20260806-active-order-code-input.md`
- `git diff --check`

## Current Status

blocked - 本轮已修复“加入活跃订单池提示 `请求参数不正确:不能为null`”回归，并补齐“只输入完整订单号未点候选”精确解析路径；活跃订单聚焦静态合同、`pnpm ts:check` 与目标 `git diff --check` 已通过。2026-08-06 14:58 已确认主运行态前端 8081 HTTP 200、后端 48081 health `UP`，并执行真实 E2E 入口；当前仍因缺少测试租户、账号和任务自有工单/工序等 `TLW_*` 写入夹具而阻塞，且相邻 RRM、PQC 静态合同失败在并行 PQC 列表选择器/重置链路缺失，按项目规则未执行 cleanup apply、提交或推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，将活跃订单维护从班组配置职责中拆出，并让新增流程由后端从唯一有效排产解析正式路线，不再信任客户端路线/调拨输入。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#统一列表复合工具栏布局门禁`：活跃订单池必须显式复用 `UnifiedListTemplate`，新增操作放在模板 actions 区域，不使用页面级临时工具栏替代标准模板。
- `docs/frontend-development.md#前端角色内容页签拆分口径门禁`：本需求是生产组长页面内部功能模块 Tab，不新增主导航路由或 PQC 组长入口。
- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用最小静态合同完成 RED/GREEN，并单独记录全量类型检查结果。
- `docs/e2e-rules.md#element-plus-下拉选择门禁`：真实流程脚本必须点击 Element Plus 真实候选，不用自由文本或隐藏字段替代下拉选择。
- `docs/powershell-memory.md`：提交前按脏工作区基线、选择性暂存和提交后残余改动门禁执行。

## Cleanup Candidates

- doc/tasks/20260805-production-leader-active-order-pool-tab/backend-api-evidence.md
- doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md
- IntRuoyiFronted/test-results/team-leader-workbench-real-flow/
