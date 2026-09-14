# Change Request: Stage 6 放行后追溯展示边界修正

## Request Summary and Source

- Source：用户验收反馈，要求按最新业务边界修改 Stage 6 文档设计。
- Summary：Stage 6 只模拟放行后追溯展示，不负责生产/PQC 完成、完工回填、批次创建、文件上传或最终放行；输入 fixture 必须等价 Stage 5 输出的已放行 `releaseSnapshot`；前端必须使用真实批次执行追溯按钮、追溯页面或追溯抽屉；后端必须调用现有追溯/领域追溯接口；输出改为 `traceVerificationSnapshot`。

## Current Baseline Reviewed

- `doc/tasks/20260821-simulation-stage6-traceability-design/task.md`
- `doc/tasks/20260821-simulation-stage6-traceability-design/simulation-stage6-traceability-design.md`
- `doc/tasks/20260821-simulation-stage6-traceability-design/execution-log.md`
- `doc/tasks/20260821-simulation-stage6-traceability-design/verification-report.md`
- `docs/product/production-team-leader-daily-operations.md`
- `docs/product/production-role-system-operations.md`
- `docs/backend-development.md#活跃订单申请放行资料必须只使用正式来源`
- `docs/backend-development.md#活跃订单模拟完成必须写正式模拟事实`

## Classification

Requirement change and design correction. This is documentation-only and changes Stage 6 scope, input contract, frontend acceptance path, backend action contract, output contract, BDD/TDD/E2E planning, and blocker definitions.

## Impact Analysis

- Product impact：Stage 6 is now a traceability display validation after final release, not a lifecycle simulation step.
- Design impact：Input fixture is limited to Stage 5-compatible released `releaseSnapshot`; traceability source data becomes supporting task-owned source dataset, not the Stage 6 input contract.
- Data impact：Future implementation must prepare task-owned released source data, but Stage 6 action itself must not mutate real business data or lifecycle states.
- API impact：Future implementation must reuse existing batch execution traceability/domain traceability API; JSON-only simulation endpoint is not acceptable.
- Frontend impact：Acceptance must pass through real batch execution traceability button, page, or drawer.
- Test impact：BDD/TDD/E2E must verify real traceability entry, node completeness, broken node display, read-only boundary, and Stage 1-5 structural compatibility.
- Release impact：No release/runtime impact in this documentation-only task.
- Operations impact：No service, database, deployment, or environment operation is required.

## Decision

Accepted.

The change resolves a stale scope mismatch: Stage 6 should consume a released snapshot and validate traceability display, rather than recreating or owning upstream production completion, backfill, batch creation, file upload, or release behavior.

## Required Approvals

- User request in current thread is sufficient for this documentation-only correction.
- Future implementation still requires confirmation of the exact frontend traceability entry and existing backend traceability API contract before code work starts.

## Downstream Skill Reruns

- Current task docs updated directly after accepted decision.
- Future implementation should rerun backend API delivery, frontend feature delivery, BDD/TDD acceptance planning, and independent verification gates as applicable.

## Blockers and Next Action

- Current documentation task has no completion blocker.
- Future implementation blocker：if the real batch execution traceability UI or existing backend traceability/domain API does not exist, Stage 6 implementation must stop and first design/implement the formal traceability capability rather than showing simulation JSON.
- Next action：use the updated Stage 6 task docs as the implementation baseline when code work is explicitly requested.