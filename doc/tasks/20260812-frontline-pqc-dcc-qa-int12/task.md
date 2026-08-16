# INT12 Final Frontline PQC Integration

## Task Goal

Connect the already verified DF01-DF11 capabilities into the formal frontline PQC controller, page, employee-switch, equipment, submission, signature, correction, release-writer, idempotency, and production-event flow. DF10/DF11 contracts are authoritative.

## Milestones

- [x] M1: Inspect the integrated baseline and record executable BDD scenarios.
- [x] M2: Run the specified backend/frontend tests and capture a real RED for missing integration behavior.
- [x] M3: Implement the smallest formal integration without fallback or compatibility branches.
- [x] M4: Run GREEN, adjacent regression, TypeScript, static-contract, and validator gates.
- [x] M5: Real Playwright write-path verification was explicitly waived by the user on 2026-08-15; no replacement test was run.
- [x] M6: Main-agent scope review, direct `int_main` integration, cleanup apply, and closeout completed; no new test was run after the user waiver.

## Expected Verification

- Backend focused Maven command from the supervisor dev plan.
- `node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs`.
- `node tests/e2e/frontline-pqc-formal-submit-static.spec.js`.
- `pnpm ts:check`.
- Real Playwright flow: active-order selection, QA process/items, employee switch, equipment/result entry, signed submission, separate PATROL_AM/PATROL_PM state.
- Backend/frontend evidence validators and `git diff --check`.

## Current Status

completed

The formal MES compile prerequisite was integrated into `int_main` as commit `254bb6181`; two newer scheduler-workbench differences were backed up and restored byte-for-byte without entering that commit. The response-contract remediation completed strict TDD. The expanded 44-test backend regression, required frontend static sequence, all three evidence validators and self-tests, `git diff --check`, and the precise formal-contract forbidden scan all pass.

The user explicitly instructed `不用测试，继续推进` on 2026-08-15. Real write-path Playwright was therefore not run and is recorded as a user-approved verification exception, not as PASS. Implementation and all previously completed local gates are integrated into `int_main`; cleanup apply completed with zero deleted paths and all declared evidence retained. The unused task-owned response-contract worktree and its patch-equivalent temporary branch were removed after exact scope/hash checks.

## Authority And Ownership

- Authority: `E:\IntRuoyi\doc\tasks\20260811-frontline-pqc-dcc-qa-agent-design\common-background.md`, `interface-contracts.md`, `agent-tasks\INT12-final-integration.md`, plus the supervisor PRD/dev-plan/test-plan in this worktree.
- Baseline: `int_main` commit `333029852`; INT12 branch contains DF10/DF11 round-4 PASS contracts and merge commit `cf23d6111`.
- Ownership: only controller/personnel-switch/submission/correction/release consumer/page/final integration tests and this task directory.
- Forbidden: upstream schema, route-DCC mapping, QA locked-version assembly, DF11 API contracts, product/route/MES-process QA inference, fallback/compat/default-success branches.

## Experience Gate

- `docs/experience-index.md` exists.
- Applicable gates: frontend picker immediate response and stale-request isolation; submit-only validation separated from draft calculation; PQC published-item/equipment snapshot authority; actual employee and signature authority; worktree compile-baseline blocker must be evidenced instead of bypassed.

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；只连接正式 DF01-DF11 服务合同并删除旧身份链路，不复制上游解析。
- 是否存在临时补丁或绕过：否；合并后发现的旧 `workOrderId + routeId` 产品推算入口已删除，没有通过补测试桩保留旧链路。

## Cleanup Keep

- `doc/tasks/20260812-frontline-pqc-dcc-qa-int12/backend-api-evidence.md`
- `doc/tasks/20260812-frontline-pqc-dcc-qa-int12/bug-regression-evidence.md`
- `doc/tasks/20260812-frontline-pqc-dcc-qa-int12/frontend-feature-evidence.md`
- `doc/tasks/20260812-frontline-pqc-dcc-qa-int12/prerequisite-main-overlap-backup/`
