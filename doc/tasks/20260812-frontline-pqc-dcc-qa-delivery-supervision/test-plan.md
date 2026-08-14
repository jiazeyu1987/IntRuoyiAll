# Test Plan

This test plan maps each implementation task to concrete verification evidence. Every task must first record BDD, RED, GREEN, and regression evidence in execution-log.md; independent testers and the supervisor record actual verification outcomes in test-report.md.

## Test Cases

### TC-C00-SCHEMA

- test_case_id: TC-C00-SCHEMA
- mapped_task_ids: [C00]
- mapped_acceptance_ids: [AC-01, AC-04, AC-05, AC-07, AC-09, AC-10, AC-12]
- environment_or_setup: Latest int_main task worktree, backend rules read, database rules read, C00 SQL fixtures under IntRuoyiBackend/sql/mysql/20260812*_mes_* and approval/blocker lists present.
- steps:
  - Run the C00 RED command before implementation and confirm schema contract failure.
  - Run the C00 GREEN and regression Maven commands after implementation.
  - Execute preflight, backfill, postflight, and rollback dry-run against task-owned fixtures.
  - Inspect IntRuoyiBackend/sql/mysql/20260812*_mes_* SQL diff for forbidden DCC-QA binding, PQC context, and item-type tables.
- expected_result: Schema and migration contract pass with explicit blocker evidence for ambiguous historical rows, no guessed current-version backfill, and no forbidden tables.
- evidence: Maven output, SQL fixture reports, row/hash reports, migration diff review, execution-log RED/GREEN markers.

### TC-DF01-ACTIVE-ORDERS

- test_case_id: TC-DF01-ACTIVE-ORDERS
- mapped_task_ids: [DF01]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-11, AC-12]
- environment_or_setup: DF01 worktree after C00 merge; three pressure-pump active-order fixtures and duplicate workOrder/route fixture.
- steps:
  - Run DF01 RED before implementation.
  - Run DF01 GREEN after implementation.
  - Review response contract for activeOrderId, no PQC task filter, no workOrder/route de-duplication, and stable sorting.
- expected_result: All effective active orders are returned as separate rows; display association gaps do not remove active orders; empty pool returns []; database failures surface.
- evidence: Maven output, controller/service test assertions, diff review, execution-log RED/GREEN markers.

### TC-DF02-SNAPSHOT

- test_case_id: TC-DF02-SNAPSHOT
- mapped_task_ids: [DF02]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-09, AC-12]
- environment_or_setup: DF02 worktree after C00 merge; valid, removed, missing-route, and cross-tenant active-order fixtures.
- steps:
  - Run DF02 RED/GREEN command.
  - Review resolver for read-only behavior and absence of client route override.
  - Confirm cross-tenant and nonexistent IDs share illegal-reference semantics.
- expected_result: Resolver returns the fixed route and QA snapshot only for an effective activeOrderId and never writes or reselects route/product data.
- evidence: Maven output, resolver test assertions, diff review, execution-log markers.

### TC-DF03-ROUTE-DCC

- test_case_id: TC-DF03-ROUTE-DCC
- mapped_task_ids: [DF03]
- mapped_acceptance_ids: [AC-04, AC-05, AC-07, AC-11, AC-12]
- environment_or_setup: DF03 worktree after C00 merge; backend and frontend rules read; route edit real path available for Playwright.
- steps:
  - Run backend RED/GREEN command.
  - Run node tests/e2e/mes-route-dcc-project-binding-static.spec.cjs.
  - Use Playwright to perform initial bind, concurrent conflict, rebind, unbind, and stale expectedVersion checks.
  - Verify route save and DCC binding save are separate requests and failures do not masquerade as success.
- expected_result: Formal route-DCC binding has monotonic versions, permission-aware bind/unbind, optimistic conflict handling, and no DCC backend QA dependency.
- evidence: Maven output, node static output, Playwright trace or screenshots, API final read-only response, supervisor diff review.

### TC-DF04-DCC-RESOLVER

- test_case_id: TC-DF04-DCC-RESOLVER
- mapped_task_ids: [DF04]
- mapped_acceptance_ids: [AC-04, AC-05, AC-07, AC-09, AC-12]
- environment_or_setup: DF04 worktree after DF02 and DF03 merges; DCC enabled, disabled, missing, duplicate, and cross-tenant fixtures.
- steps:
  - Run DF04 RED/GREEN command.
  - Review resolver dependencies and verify no productCode/materialCode/productMasterId matching.
- expected_result: Resolver returns exactly one enabled same-tenant DCC project or fails fast on every ambiguity.
- evidence: Maven output, resolver tests, diff review, execution-log markers.

### TC-DF05-DCC-QA

- test_case_id: TC-DF05-DCC-QA
- mapped_task_ids: [DF05]
- mapped_acceptance_ids: [AC-04, AC-05, AC-07, AC-11, AC-12]
- environment_or_setup: DF05 worktree after C00 merge; QA management and DCC project list static tests available.
- steps:
  - Run both DF05 node RED commands before implementation.
  - Run both node GREEN commands and backend QA service regression after implementation.
  - Review that QA relation uses dccProjectCodeId and DCC list uses frontend bulk MES status call.
- expected_result: QA规程只对应DCC项目代码, DCC项目代码列表可显示/跳转QA规程, no DCC-side binding table, no product or route inference.
- evidence: Node output, Maven output, frontend/backend diff review, execution-log markers.

### TC-DF06-ORDER-QA-LOCK

- test_case_id: TC-DF06-ORDER-QA-LOCK
- mapped_task_ids: [DF06]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-08, AC-09, AC-12]
- environment_or_setup: DF06 worktree after DF04 and DF05 merges; active-order creation/reactivation fixtures with current PUBLISHED, RETIRED, disabled DCC, and PATROL rules.
- steps:
  - Run DF06 RED/GREEN command.
  - Inspect transaction boundary for active-order snapshots, process snapshots, and task generation.
  - Verify removed reactivation preserves original snapshot and task history.
- expected_result: New orders lock current enabled DCC and PUBLISHED QA version atomically; four rule keys generate correct tasks; old removed orders do not relock current QA.
- evidence: Maven output, mapper/service assertions, transaction diff review, execution-log markers.

### TC-DF07-LOCKED-PROCESSES

- test_case_id: TC-DF07-LOCKED-PROCESSES
- mapped_task_ids: [DF07]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-07, AC-09, AC-12]
- environment_or_setup: DF07 worktree after DF05 and DF06 merges; fixtures for PUBLISHED, RETIRED, disabled DCC, wrong-regulation, and empty-process versions.
- steps:
  - Run DF07 RED/GREEN command.
  - Review locked-version method to ensure it does not call current QA or require enabled DCC.
  - Confirm no routeProcessId/processId existence check appears in runtime QA reading.
- expected_result: Locked QA version returns QA-owned processes sorted by sort/id for PUBLISHED or RETIRED versions and fails on real data-integrity errors.
- evidence: Maven output, service tests, grep/diff review for forbidden route-process checks, execution-log markers.

### TC-DF08-QA-ITEMS

- test_case_id: TC-DF08-QA-ITEMS
- mapped_task_ids: [DF08]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-08, AC-12]
- environment_or_setup: DF08 worktree after DF07 merge; pressure-pump QA version with 8 processes, 18 business items, and 51 type rows.
- steps:
  - Run DF08 RED/GREEN command.
  - Verify aggregation key qaProcessId + itemCode and itemCode global uniqueness checks.
  - Review resultType validation and rule-key truth table.
- expected_result: Full published item fields, equipment options, source fields, and FIRST/PATROL/FINAL applicability are preserved; PATROL_AM/PM are not collapsed.
- evidence: Maven output, item aggregation assertions, diff review, execution-log markers.

### TC-DF09-TASK-OVERLAY

- test_case_id: TC-DF09-TASK-OVERLAY
- mapped_task_ids: [DF09]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-08, AC-12]
- environment_or_setup: DF09 worktree after DF08 merge; task fixtures for no task, FIRST, PATROL_AM, PATROL_PM, FINAL, and production event candidates.
- steps:
  - Run DF09 RED/GREEN command.
  - Review grouping by activeOrderId, regulationVersionId, qaProcessId, and inspectionRuleKey.
  - Verify production event candidate requires active-order process snapshot ownership.
- expected_result: Task overlay never filters QA processes/items, returns NOT_CREATED when appropriate, keeps AM and PM tasks separate, and sorts by businessDate/ruleSort/round/taskId.
- evidence: Maven output, task overlay tests, production candidate tests, diff review, execution-log markers.

### TC-DF10-BACKEND-PROJECTION

- test_case_id: TC-DF10-BACKEND-PROJECTION
- mapped_task_ids: [DF10]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-07, AC-09, AC-12]
- environment_or_setup: DF10 worktree after DF02, DF07, DF08, and DF09 merges; pressure-pump fixtures and disabled-DCC historical-order fixture.
- steps:
  - Run DF10 RED/GREEN command.
  - Run the QA locked-version service test together with the DF10 projection test and verify the full `getLockedVersionForOrder` aggregate is the only runtime QA read boundary.
  - Review service for batched reads and dedicated MesFrontlinePqcProcessRespVO.
  - Compile the controller consumer and verify only the dedicated PQC mapping drops obsolete aliases while the production-route response remains unchanged.
  - Confirm production-route process response is unchanged.
- expected_result: Active-order process GET projection returns all locked QA processes/items plus task state and candidates through the frozen full locked-version service boundary, without N+1, compatibility aliases, current-QA lookup, or route-process QA validation.
- evidence: Maven output, context service tests, diff review, execution-log markers.

### TC-DF11-FRONTEND-PROJECTION

- test_case_id: TC-DF11-FRONTEND-PROJECTION
- mapped_task_ids: [DF11]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-08, AC-12]
- environment_or_setup: DF11 worktree after DF08 and DF09 merges; frontend dependencies installed.
- steps:
  - Run node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs.
  - Run pnpm ts:check; page-local changes remain limited to formal active-order/task identity and strict DTO consumption.
  - Review API types for activeOrderId-only request, full item fields, resultType union, and inspectionRuleKey union.
  - Execute the real `selectFrontlinePqcActiveOrder` consumer with reversed response completion and assert only the latest activeOrderId mutates page state.
  - Verify duplicate rows with the same workOrderId/routeId have distinct picker keys and active states, and no process-level flattened task identity remains.
- expected_result: Frontend contracts expose QA processes, full item details, four task rule keys, status/options, production candidates and stable sorting; selection is keyed only by activeOrderId, task identity comes only from pqcTaskOptions, and real-consumer stale responses cannot overwrite the latest order.
- evidence: Node static output, TypeScript output, diff review, execution-log markers.

### TC-INT12-INTEGRATION

- test_case_id: TC-INT12-INTEGRATION
- mapped_task_ids: [INT12]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-08, AC-09, AC-10, AC-12]
- environment_or_setup: INT12 worktree after DF01 through DF11 are merged; local backend/frontend services started only after slot reservation; real test tenant/account/data confirmed.
- steps:
  - Run INT12 backend RED/GREEN command.
  - Run node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs.
  - Run node tests/e2e/frontline-pqc-formal-submit-static.spec.js.
  - Run pnpm ts:check.
  - Use Playwright real path to select each pressure-pump active order, view 8 QA processes and 18 items, switch actual employee, select equipment, submit numeric/boolean/text results, and verify AM/PM task independence.
  - API only for final read-only confirmation of submitted task/event/hash/signature state.
- expected_result: Full frontline PQC flow is driven by activeOrderId, locked QA process identity, task rule key, actual employee signature, equipment/result validation, production event ownership, and canonical idempotency.
- evidence: Maven output, node outputs, TypeScript output, Playwright trace/screenshots, read-only API response, supervisor review notes.

### TC-VAL13-INDEPENDENT-ACCEPTANCE

- test_case_id: TC-VAL13-INDEPENDENT-ACCEPTANCE
- mapped_task_ids: [VAL13]
- mapped_acceptance_ids: [AC-01, AC-02, AC-03, AC-04, AC-05, AC-06, AC-07, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13, AC-14]
- environment_or_setup: Latest int_main after INT12 fast-forward merge; independent agent did not implement C00 through INT12; production-code read-only.
- steps:
  - Re-run the VAL13 backend aggregate Maven command from VAL13-independent-acceptance.md.
  - Re-run every task-added frontend static test directly with node.
  - Run pnpm ts:check.
  - Verify C00 SQL preflight/postflight/rollback dry-run reports.
  - Use Playwright real paths for route-DCC config, DCC QA link/status, three active orders, QA process list, item list, personnel switch, device selection, formal submit, and AM/PM separation.
  - Inspect final Git/task evidence for branch, worktree, commit hash, validation, merge status, cleanup, and no push/deploy/server/shared-data modification.
- expected_result: VAL13 records pass/fail evidence for every AC; if any defect exists, it returns the exact failing task to the supervisor and makes no production-code change.
- evidence: test-report.md VAL13 section, command outputs, Playwright artifacts, read-only API confirmations, final task-state and supervisor closeout report.

### TC-SYS-WAVE-GATE

- test_case_id: TC-SYS-WAVE-GATE
- mapped_task_ids: [C00, DF01, DF02, DF03, DF04, DF05, DF06, DF07, DF08, DF09, DF10, DF11, INT12, VAL13]
- mapped_acceptance_ids: [AC-02, AC-03, AC-04, AC-11, AC-13]
- environment_or_setup: Supervisor task-state file and live agent list available.
- steps:
  - Before launching each wave, confirm all dependency_ids are completed, reviewed, independently tested, committed, and merged into int_main.
  - Confirm no more than three worker subagents are running.
  - Confirm no later-wave task starts to fill unused worker capacity.
  - Confirm write_scope conflicts are absent among same-wave running tasks.
- expected_result: Execution order exactly matches Wave 0 through Wave 9 and all shared-file ownership is serial.
- evidence: task-state transitions, execution-log supervisor entries, live agent list snapshots, branch/worktree records.

### TC-SYS-MERGE-CLOSEOUT

- test_case_id: TC-SYS-MERGE-CLOSEOUT
- mapped_task_ids: [C00, DF01, DF02, DF03, DF04, DF05, DF06, DF07, DF08, DF09, DF10, DF11, INT12, VAL13]
- mapped_acceptance_ids: [AC-04, AC-11, AC-12, AC-13, AC-14]
- environment_or_setup: Each task branch has passed supervisor review and independent verification; int_main available; worktree rules read.
- steps:
  - For each implementation task, inspect full diff and staged files.
  - Merge latest int_main into the task branch, rerun task validations, then fast-forward merge into int_main.
  - Stop on conflicts, non-fast-forward state, unowned dirty overlap, or half-merge risk.
  - After final validation, clean only task-owned worktrees and record remaining risks/blockers.
- expected_result: All passed code is merged into int_main by fast-forward only, no push/deploy/server/shared-data action occurs, task worktrees are cleaned by rule, and final report covers all 14 tasks.
- evidence: git status/log outputs, commit hashes, validation outputs, cleanup evidence, final supervisor report.
