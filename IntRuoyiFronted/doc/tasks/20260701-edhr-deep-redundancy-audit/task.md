# 任务：eDHR 深度冗余模块审计与安全清理（前端）

- Task ID: `20260701-edhr-deep-redundancy-audit`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_dedup_deep\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

在 Phase 1-6 已完成“批次详情页主流程收口”的基础上，继续深挖 eDHR 模块中是否存在可安全删除、可合并、必须保留或应继续下沉的功能模块；只在证据证明无菜单/无路由/无调用/无业务职责时删除，避免凭页面相似误删真实能力。

## Previous Task Check

- 上一个 eDHR 任务：`20260701-edhr-phase6-module-dedup`
- 状态：`completed`
- 处理说明：本轮在新的 `edhr_dedup_deep` 成对 worktree 中继续，不回滚 Phase 1-6 成果。

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 命令、中文读写和 Python stdin 均显式使用 UTF-8。
- 命中 `docs/worktree-memory.md`：本轮使用独立成对 worktree，路径为 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_dedup_deep`，不在主工作区直接开发。
- 命中 `simplify-codebase`：删除前必须证明调用面和业务职责消失；优先删除死入口和重复导航，而不是重写仍有职责的页面。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。目标是把 eDHR 主流程、后台管理、专业审计/验证能力边界整理清楚。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 冗余模块必须可证明 -> Given 一个 eDHR 页面疑似重复 / When 检查菜单、路由、源码调用、API 测试和真实业务职责 / Then 只有四项证据均表明无生产职责时才允许删除。`
- `BDD: 主流程继续唯一 -> Given 用户从 eDHR 批次列表处理批次 / When 进入详情页 / Then 主流程仍只经批次详情页承载，后台页不得重新成为并行主流程入口。`

## Milestones

1. M1：建立深度审计任务台账。`completed`
2. M2：扫描菜单、路由、页面、API 与测试引用。`completed`
3. M3：形成二次冗余候选和删除安全等级。`completed`
4. M4：执行有证据的最小清理并补测试。`completed`
5. M5：运行前后端验证与真实 E2E。`completed`

## Expected Verification

- 前端静态/类型验证：`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_dedup_deep\yudao-ui-admin-vue3 ts:check`。
- 后端目标测试：受影响 controller/service 测试，若无后端代码删除则记录无变更证据。
- 真实 E2E：测试租户从 `eDHR批次执行` 列表点击 `详情`，确认批次详情主流程仍可见总控、阶段、放行、审计和后台入口。

## Current Blockers

- 暂无。


## Redundancy Decision

- 已证明可安全清理：前端隐藏路由别名 `pro/edhr-work-task`、`pro/feedback/edhr-signature`、`pro/edhr-recordbook`、`pro/feedback/edhr-print-task`、`pro/feedback/edhr-form-template`、`pro/feedback/edhr-form-instance`。
- 保留正式入口：`pro/feedback/edhr-work-task`、`pro/feedback/edhr-signatures`、`pro/feedback/edhr-recordbook`、`pro/feedback/edhr-label`、`pro/feedback/edhr-form`。
- 不删除后端接口：当前清理对象仅为无菜单、无非路由源码调用、无真实入口证据的前端别名路由；后端 API 仍服务正式页面、测试和业务能力。
- 额外修正：`WorkTaskBoardPage.vue` 的任务时间文案补充 `到期时间` / `逾期时间` 明确标签，避免把完成时间误作逾期或到期信息。

## Cleanup Keep

- src/router/modules/remaining.ts
- src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue
- tests/e2e/edhr-redundant-route-alias-static.spec.js
- tests/e2e/edhr-deep-dedup-list-detail-real-flow.e2e.js
- doc/tasks/20260701-edhr-deep-redundancy-audit/task.md
- doc/tasks/20260701-edhr-deep-redundancy-audit/execution-log.md

## Final Verification

- `GREEN: frontend-static-regression -> PASS`，eDHR 既有静态契约与新增冗余路由门禁全部通过。
- `GREEN: frontend-ts-check -> PASS`，`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` 通过。
- `GREEN: real-e2e-system-chrome -> PASS`，测试租户 `aoteman` 从批次执行列表进入批次详情页，详情页 `/get` 与 `/workbench` 均返回 200，6 个冗余别名路由均不可解析。
- `GREEN: backend-delete-decision -> PASS`，本轮未删除后端 API；没有证据证明后端 eDHR 接口已无真实业务职责。
