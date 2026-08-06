# PQC 表单历史页签

## Task Goal

在 PQC 组长工作台新增“历史表单”页签，仅展示审核通过的 PQC 表单历史；列表内容与“PQC管理”基本一致，并新增“审核通过人”和“审核通过时间”。

## Milestones

1. `completed` 建立 BDD/TDD 任务记录与 RED 静态合同。
2. `completed` 核对 PQC 管理页签现有列池、查询条件和审核字段来源。
3. `completed` 前端新增“历史表单”页签、审核通过固定查询和只读历史列。
4. `completed` 补齐必要的前端类型/API 字段映射。
5. `completed` 运行目标静态合同、相邻合同和类型检查。
6. `in_progress` 更新验证报告与收尾状态。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/pqc-leader-form-history-tab-static.spec.cjs`
- `node IntRuoyiFronted/tests/e2e/team-leader-production-report-history-tab-static.spec.cjs`
- `node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs`
- `node IntRuoyiFronted/tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs`
- `pnpm ts:check`（在 `IntRuoyiFronted` 下）
- `git diff --check`

## Current Status

ready_for_closeout

历史表单功能实现完成，目标合同、生产报工历史相邻合同、后端 mapper 静态合同、`pnpm ts:check` 和 `git diff --check` 均通过。`team-leader-production-report-payload-columns-static.spec.cjs` 仍失败在既存生产报工列池断言，不属于本次 PQC 历史表单改动范围，已在验证报告记录为相邻回归阻塞。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；复用 PQC 管理正式列表和审核状态字段，历史页签通过正向 `APPROVED` 条件隔离。
- `是否存在临时补丁或绕过`：否。

## Experience Gates

- 命中 `docs/frontend-development.md` 多角色共享表格列池隔离门禁：PQC 管理与历史表单共享列表区域时，历史专属字段必须通过列池和 tableKey 隔离，不能只靠 `v-if` 隐藏。
- 命中 `docs/frontend-development.md` 前端列表状态口径完整性门禁：用户要求“审核通过的 PQC表单历史”，必须按 `APPROVED` 正向状态集合建模，不得只排除某个异常状态。
- 命中 `docs/frontend-development.md` 前端角色内容页签拆分口径门禁：PQC 组长页面内部功能模块 Tab 必须有独立 tab key、显示 gate、正式查询触发和相邻合同验证。
