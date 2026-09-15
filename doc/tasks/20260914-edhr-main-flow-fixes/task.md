# 20260914-edhr-main-flow-fixes

## Task Goal

Persistent objective: 修复静态发现的主流程逻辑问题，按普通ERP订单的前后端路径检查配置发布、加入订单、生产/PQC及复核、完工回填、生产放行、四类报告、最终放行、归档和历史追溯。优先普通主流程，不扩大到小概率异常。

Directly fix the remaining eDHR main-flow blockers in the current `int_main` worktree:

- EDHR-STATIC-008: process-inspection verdict consistency for scrap quantities.
- EDHR-STATIC-020: formal loss must come from production replenishment orders, with no-replenishment confirmation.
- EDHR-STATIC-023: cross-workorder production allocation must use source events and target allocations correctly.
- EDHR-STATIC-026: conditional loss forms must be treated as not applicable across task creation, filling, progression, and progress.
- Main-flow gap: completing an order without matched replenishment orders must require an explicit "no replenishment information" confirmation.
- EDHR-STATIC-027—032: dynamic loss receipt chain, completion-timed input batches/detail, trusted automatic effects, actual submitter, independent report sequencing, and consistent route-material freezing.

## 设计约束检查 / Design Constraints Check

- Work directly in the current `int_main` worktree as requested.
- Do not use fallback, silent downgrade, mock success, or guessed source data.
- Keep process-start, per-process batch-record binding, and formBinding slot chains separate.
- Do not use API-only or database writes as user-path E2E verification.
- Do not commit or push unless the user explicitly authorizes Git writes in this turn.
- Formal loss source follows the current business rule: production replenishment orders are authoritative; frontline process loss and PQC scrap are process evidence only.
- Pick lists may arrive after frontline production/PQC; completion backfills input material source data.

## Milestones

1. Completed: BDD and observed RED for 008, 020, 023, 026 and no-replenishment confirmation/replay.
2. Completed: inspected API, completion, replenishment, writer, batch-task and readiness boundaries.
3. Completed: added regression tests for formal sources, cancellation, source IDs, scrap evidence and conditional task gates.
4. Completed: backend implementation for 008, 020, 023 and 026; formal loss source IDs remain separate from materialized record IDs.
5. Completed: frontend no-replenishment popup, confirmation retry and cancellation path.
6. Completed: final targeted regression 210/210 passed; frontend type checking and related contracts passed. Broad regression found nine unchanged baseline failures, reproduced with the original batch service in an isolated class directory.
7. Completed: bug status, evidence consolidation, cleanup preview/apply. Git integration remains unauthorized by the current turn.

## Expected Verification

- Backend focused unit/static tests for each corrected service boundary.
- Frontend focused static/component contract for the no-replenishment confirmation request payload and visible confirmation behavior.
- Relevant Maven targeted tests under `yudao-module-mes`.
- Relevant frontend targeted tests or static contract under `IntRuoyiFronted`.
- `git diff --check`.
- Skill evidence validators for backend, frontend, and bug-regression evidence.

## Current Status

ready_for_closeout — user authorized task-owned commit/push; implementation verified and previous cleanup passed, Git integration now in progress.

Previous targeted tests and cleanup remain historical evidence, not proof of the newly found paths. Current scope: propagate allocated source events through target-order completion/backfill/release; carry typed dynamic loss-form evidence through PQC release and downstream receipts. Preserve all existing uncommitted work. Git commit/push and E2E remain unauthorized.

## Follow-up Milestones

1. Completed: BDD and observed RED for completion, list progress, reallocation and dynamic-loss dossier acceptance.
2. Completed: allocated-event discovery across list/completion/loss/batch release; typed dynamic loss and audit evidence through final PQC receipt/replay/API.
3. Completed: 133 targeted tests and frontend type check; existing active-order service baseline failures separately reproduced. Documentation updated; cleanup preview/apply passed. No implementation or cleanup work remains for the two follow-up findings; Git commit/push remain unauthorized.

## Main-flow Goal Milestones

1. Fix normal pre-completion production loading without ERP pick-list batches; completion remains the authoritative backfill/check boundary.
2. Give verified automatic process-inspection/loss backfill a trusted submission path independent of manual fill-task timing and candidate identity.
3. Verify manual form responsibility and sequencing remain enforced, while automatic evidence cannot be forged through ordinary frontend requests.
4. Recheck frontend/backend contracts across activation, production/PQC submit and review, completion, PQC release, manual batch records, four reports, manager release and archive/history.
5. Resolve every ordinary-path gap found, run meaningful connected regressions and retain a requirement-to-evidence matrix. No E2E, deployment, main service restart or Git write is authorized by this code-analysis goal.

## Verification Boundaries

- Full affected batch-service regression is retained as evidence: 185 tests, 176 passing and 9 failing, with the same 9 failures reproduced using the original service at HEAD 70b53cf270fb989b48c3850e315e691011d69fe4.
- These pre-existing failures are not described as PASS or as new business defects. Other targeted regression remains a required gate.
- No browser E2E, database writes, deployment, main backend restart, Git commit or push is authorized or executed.

## 2026-09-15 Current Milestone Evidence

- Completed: pre-completion production no longer requires ERP batches; order detail projects the immutable completion pick-list snapshot. 103-test and 28-test selections pass.
- Completed: trusted automatic dynamic inspection/loss submission, actual manual submitter, companion todo closure and subsequent manual advancement. BPM/MES 33-test selection and work-task 24-test selection pass.
- Completed: independent release-report uploads no longer gate ordinary route-form dispatch. 87 release-chain tests pass; final readiness still checks both report and ordinary-form evidence.
- Completed verification: frontend TypeScript and three relevant static frontend contracts pass. Archive/PDF/history three-test endpoint selection passes.
- Completed: activation now freezes materials from the normal route UI/frontline source; 30 tests plus inserted snapshot assertion PASS. Production/PQC/review/completion 127-test expansion PASS after correcting an obsolete audit stub.
- Completed: final 90-step source/evidence matrix, 70 publication/finalization tests, 42 source/QA-publication tests, and managed-active-order finalization test PASS.
- Remaining administrative gate: cleanup preview/apply, then explicit current-turn Git commit/push authorization required by AGENTS.md before the task-closeout-rules.md completion gate can be fulfilled. No remaining confirmed ordinary main-flow code issue; no E2E claim.

## Final Closeout Gate

Cleanup preview and apply PASS (ready/applied, int_main, linked=False, no blocked paths or warnings). Only task-local temporary logs were deleted; task.md, execution-log.md, verification-report.md and formal code/tests remain. Unrelated dirty files were untouched. Backend/frontend/bug evidence validators and diff whitespace check PASS. No remaining implementation or verification work in the authorized code-analysis scope. Awaiting user authorization to commit/push task-owned changes only; goal blocked threshold has not been met, so no goal status update is made this turn.
