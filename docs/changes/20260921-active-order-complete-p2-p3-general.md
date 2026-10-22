# Change Request: Active Order Complete Uses General P2 P3 Flow

## Request Summary And Source

Source: user request on 2026-09-21.

Request: 将活跃订单 `完工` 按钮改成通用正式 P2+P3 合集，不依赖测试重置工单，也不依赖 Stage1 模拟标记。

## Current Baseline Reviewed

- `docs/product/production-team-leader-daily-operations.md`
- `docs/product/production-role-system-operations.md`
- `docs/acceptance/production-execution-main-loop/scope-contract.md`
- `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.java`
- `doc/tasks/20260916-stage1-p1-p2-split/`
- `doc/tasks/20260917-stage1-p3-pqc-release/`

## Classification

Requirement change and user-facing workflow correction.

## Impact

Product: `完工` becomes the primary business action for formal active orders and represents P2 completion/backfill plus P3 PQC release push.

Design: existing standalone P1/P2/P3 diagnostic controls may remain, but `完工` must not inherit their Stage1 simulation gate.

Data: no schema change expected. Existing completion receipts, batch executions, release applications, work tasks and source hashes remain the durable facts.

API: existing release apply endpoint should return enough P3 receipt data, including P2 batch execution binding, for the frontend to verify success.

Tests: add/adjust backend service test and frontend static contract. Real E2E is not required unless explicitly requested.

Release: medium risk because this changes a primary production button. Risk is mitigated by strict server-side ownership/source/idempotency gates.

Operations: no service restart or data migration required for code-level validation.

## Decision

Accept.

## Required Approvals

User approval is present in this turn. No database write, release, remote server operation, Git commit, or E2E authorization is included.

## Downstream Skill Reruns

- backend-api-delivery
- frontend-feature-delivery

## Blockers And Next Action

Implementation accepted and completed for code-level validation.

Known verification limitation: full `yudao-server` reactor verification is blocked by package-phase Maven plugin ordering (`maven-dependency-plugin:unpack-report-openpdf-phrase-override`, MDEP-98). The owned `yudao-module-mes` targeted backend tests and frontend static contracts passed.
