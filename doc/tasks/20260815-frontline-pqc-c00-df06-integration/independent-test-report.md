# Independent Test Report

## Result

PASS

## Scope

- Task: C00/DF06/INT12/VAL13 final integration.
- Worktree: `D:\IntRuoyiWorktree\20260815-frontline-pqc-c00-df06-integration`.
- Mode: independent validation only. No production code, migration, test source, task-state, Git, runtime registry, or business data was intentionally modified.
- Exception: real write Playwright path remains a user-approved 2026-08-15 exception. It is recorded as an exception only, not PASS and not FAIL.

## Requirement Gate

1. C00 approved manifest gate: PASS.
   - `20260812_mes_pqc_dcc_qa_c00_backfill.sql` creates and consumes `c00_backfill_approved_active_order_snapshot`.
   - Active-order snapshot candidates are inserted with `evidence_source = 'APPROVED_MANIFEST'`.
   - Missing approved active-order manifest is blocked by `active_order_manifest_missing`.
   - Task version history is only cross-check evidence via `active_order_manifest_task_version_ambiguous`.
   - Search of C00 SQL returned no `UNIQUE_TASK_VERSION` or `tmp_c00_active_order_unique_task_version`.
   - `MesQaPqcSchemaTest` explicitly asserts the same forbidden strings are absent.

2. DF06 active-order creation/reactivation: PASS.
   - `addActiveOrder` resolves the formal route DCC binding, enabled DCC project, unique QA regulation, current PUBLISHED QA version, and valid QA processes before insert.
   - New active orders write `dccProjectCodeId`, `qaRegulationId`, and `qaRegulationVersionId`, then insert route snapshots and PQC tasks in the same transactional method.
   - PQC task generation creates the canonical rule keys `FIRST`, `PATROL_AM`, `PATROL_PM`, and `FINAL`.
   - Missing DCC, disabled DCC, and missing QA are covered by fail-fast tests.
   - Removed-order reactivation validates and preserves the removed row's historical QA snapshot and task identities; it does not re-read current route DCC/QA to relock.

3. QA-to-DCC boundary: PASS.
   - QA save/request contract uses `dccProjectCodeId` and QA-owned `processes`.
   - Frontend static contract forbids `productId`, `productMasterId`, `routeId`, `routeProcessId`, `processId`, and `workOrderId` in QA save payload.
   - Locked QA version read validates DCC project code ownership and version state, with tests proving RETIRED locked versions can still be read without enabled-DCC current checks.
   - No reviewed evidence shows QA regulation being validated against product, material, or MES route process existence.

4. INT12/VAL13 backend frozen suite: PASS.
   - Re-ran the VAL13 aggregate Maven command.
   - Surefire evidence shows 17 classes, 127 tests, 0 failures, 0 errors, 0 skipped.

5. Frontend static contracts and TypeScript: PASS.
   - Re-ran all 6 required frontend static contracts.
   - Re-ran `pnpm ts:check`; exit code 0.

6. Branch runtime port guard: PASS.
   - Re-ran `scripts\preflight\branch-runtime-port-guard.ps1`.
   - Output: `Branch runtime port guard passed for task/20260815-frontline-pqc-c00-df06-integration/int_main: frontend 8155, backend 48155.`
   - This matches v4 slot range support and the current slot 21 mapping.

7. Real write Playwright path: EXCEPTION.
   - User explicitly waived the real write Playwright path on 2026-08-15.
   - This report does not count that path as PASS and does not fail the task on that waived path.

## Commands Re-run

- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS; frontend 8155, backend 48155.
- `mvn.cmd -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest,MesQaInspectionRegulationServiceTest,MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest,MesFrontlineActiveOrderSnapshotResolverTest,MesRouteDccProjectBindingServiceTest,MesRouteDccProjectBindingControllerTest,MesFrontlineDccProjectResolverTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest,MesFrontlinePqcTaskOverlayTest,MesFrontlineProductionSubmitCandidateTest,MesFrontlinePqcEmployeeSwitchServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcSubmissionConcurrencyTest,MesProcessPoolPqcInspectionCorrectionServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS; 127 tests.
- Surefire XML recount -> PASS; 17 classes, 127 tests, 0 failures, 0 errors, 0 skipped.
- `node tests/e2e/mes-route-dcc-project-binding-static.spec.cjs` -> PASS.
- `node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs` -> PASS.
- `node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs` -> PASS.
- `node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs` -> PASS.
- `node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs` -> PASS.
- `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260815-frontline-pqc-c00-df06-integration\database-schema-evidence.md` -> PASS.
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --self-test` -> PASS.
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --self-test` -> PASS.
- `git diff --check` -> PASS; line-ending warnings only.
- `rg -n "^(<<<<<<<|>>>>>>>)" --glob '!**/target/**' --glob '!**/node_modules/**'` -> PASS; no matches.
- `rg -n "UNIQUE_TASK_VERSION|tmp_c00_active_order_unique_task_version" ...C00 SQL files...` -> PASS; no matches.

## Decision

PASS. No blocking defect was found under the approved validation scope. The only unrun user-path gate is the 2026-08-15 user-approved real write Playwright exception, recorded as neither PASS nor FAIL.
