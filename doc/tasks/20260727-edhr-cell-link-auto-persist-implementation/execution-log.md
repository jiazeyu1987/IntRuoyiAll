# Execution Log

## User Intent

用户要求实现设计文档中的开发验证任务：将 eDHR 批记录单元格链接改为创建/打开执行记录时自动落库预填值，并完成开发验证。

## Baseline And Ownership

- Current branch: `int_main`.
- Existing design task: `doc/tasks/20260727-edhr-cell-link-auto-persist-design/`.
- Dirty baseline before implementation:
  - `291306c4 chore: preserve dirty baseline before cell link auto persist`
  - `e0e51633 chore: preserve controlled browse diagnosis baseline`
- This task owns only changes made after the baseline commits for the auto-persist implementation and evidence.

## BDD Scenarios

- `BDD: Production work order batch code auto-persists on execution create/open -> Given` an enabled cell-link rule maps `PRODUCTION_WORK_ORDER.batchCode` to an empty target execution cell, `When` the execution record is created or opened, `Then` the backend persists the source value into `cell_values_json` and updates the field audit chain.
- `BDD: Existing manual target value is not overwritten -> Given` the target cell already has a stored value and the rule uses `ONLY_WHEN_EMPTY`, `When` the execution is opened again, `Then` the stored value remains unchanged and the auto-persist result reports `TARGET_ALREADY_MANUAL`.
- `BDD: Missing production batch code fails fast -> Given` an enabled production work order batch-code link exists but the work order `batchCode` is blank, `When` the execution is created or opened, `Then` the backend returns a clear missing source value error and does not write a blank/default value.
- `BDD: Repeated open is idempotent -> Given` the same rule and source value were already auto-persisted, `When` the execution is opened repeatedly, `Then` no duplicate audit batch is appended and the hash chain remains valid.
- `BDD: Frontend uses persisted values only -> Given` the execution detail does not contain a stored value, `When` the execution page hydrates draft state, `Then` the frontend must not inject `/prefill` values as if they were saved.
- `BDD: Cell-link auto-persist idempotency key fits audit schema -> Given` an enabled cell-link rule writes through field audit, `When` the auto-persist command builds the system idempotency key, `Then` the key is stable, hash-derived, and no longer than the `mes_pro_batch_record_execution_field_audit_batch.idempotency_key` 64-character schema limit.

## RED/GREEN Evidence

- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" test -> FAIL expected before implementation because the backend had no service that persisted applicable cell-link values through the field-audit chain.`
- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_autoPersistsCellLinksOnNewExecutionAndReturnsSummary" test -> FAIL expected before openOrCreateByContext was wired to auto-persist and return the result summary.`
- `RED: node tests/e2e/edhr-cell-link-auto-persist-static.spec.js -> FAIL expected before frontend adjustment because ExecutionPage.vue still called /prefill and injected local draft values.`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 4 tests, 0 failures, 0 errors.`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 138 tests, 0 failures, 0 errors.`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_bindsExistingSingleExecutionContext+openTask_withoutProductionTaskContext_stillOpensBatchRecordWithoutScheduleReference+openTask_ignoresSingleWorkOrderProductionTaskWhenOpeningBatchRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 3 tests, 0 failures, 0 errors.`
- `GREEN: node tests/e2e/edhr-cell-link-auto-persist-static.spec.js -> PASS: eDHR cell link auto-persist frontend static contract.`
- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 4 tests, 2 failures; generated cell-link auto-persist idempotency key length was 101 instead of the required 64-character schema limit.`
- `GREEN: mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" surefire:test -> PASS, 4 tests, 0 failures, 0 errors; compiled production/test classes were newer than their source files after the full lifecycle compile completed, so Surefire was invoked directly to avoid concurrent reactor recompilation.`
- `GREEN: isolated worktree mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 4 tests, 0 failures, 0 errors.`
- `REGRESSION: isolated worktree mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 138 tests, 0 failures, 0 errors.`
- `GREEN: node tests/e2e/edhr-batch-execution-filler-entry-static.spec.js -> PASS; validates paired worktree URLs and authenticated execution-detail readback.`
- `GREEN: EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8086 and EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48086 node tests/e2e/edhr-batch-execution-real-flow.e2e.js -> PASS through the real batch-detail open-task path.`

## Milestone Updates

- Created implementation task directory and recorded applicable BDD scenarios.
- Implemented `MesProBatchRecordCellLinkAutoPersistService` and the command/result model.
- Routed auto-persist writes through `MesProBatchRecordExecutionFieldAuditService#saveSystemCellLinkChanges`.
- Wired execution create/open and eDHR task open to invoke auto-persist and return `cellLinkAutoPersist`.
- Removed the frontend `/prefill` draft injection path and added a focused static contract.
- Added/updated tests for create-time invocation, task-open summary, missing source value, manual target non-overwrite, and repeated-open idempotency.
- Added backend API, frontend feature, bug regression, and verification evidence files.
- Created isolated worktree `D:\IntRuoyiWorktree\20260727_edhr_cell_link_idempotency` on branch `codex/edhr-cell-link-idempotency-e2e-20260727`, reserved `int_main slot=5`, and started task-owned frontend/backend on `8086/48086`.
- Reproduced the real database failure caused by a 101-character plaintext audit idempotency key against the `varchar(64)` schema.
- Replaced the persisted/lookup idempotency key with deterministic SHA-256 output and verified both save and repeated-open lookup paths use exactly 64 lowercase hexadecimal characters.
- Updated the real E2E runner to require paired frontend/backend worktree URLs, validate frontend HTTP 200 plus backend health `UP`, and reuse browser `ACCESS_TOKEN`, `tenant-id`, and optional `visit-tenant-id` for read-only execution-detail verification.
- Completed the authorized real Playwright path and restored the temporary work-task assignee after verification.

## Verification Log

- `2026-07-27 PASS: MesProBatchRecordCellLinkAutoPersistServiceImplTest -> 4 tests.`
- `2026-07-27 PASS: MesProBatchRecordExecutionServiceImplTest + MesProBatchRecordCellLinkServiceImplTest + MesProBatchRecordExecutionFieldAuditServiceTest -> 138 tests.`
- `2026-07-27 PASS: focused MesProEdhrBatchExecutionServiceTest task/open regression -> 3 tests.`
- `2026-07-27 PASS: edhr-cell-link-auto-persist-static.spec.js.`
- `2026-07-27 PASS: git diff --check for task-owned implementation files and task docs; LF-to-CRLF warnings only.`
- `2026-07-27 PASS: backend-api-delivery evidence validator.`
- `2026-07-27 PASS: frontend-feature-delivery evidence validator.`
- `2026-07-27 PASS: bug-regression-fix-loop evidence validator.`
- `2026-07-27 PASS: task-closeout-cleanup preview -> all six task evidence files kept, no delete, blocked, or warning entries.`
- `2026-07-27 PASS: task-closeout-cleanup apply -> no deletions and no warnings.`
- `2026-07-27 PASS: project-experience-consolidation -> existing durable rule is present at docs/backend-development.md#批记录单元格链接预填落库边界 and indexed in docs/experience-index.md; no duplicate document created.`
- `2026-07-27 BLOCKED broader regression: full MesProEdhrBatchExecutionServiceTest -> 142 tests, 1 failure and 10 errors from unrelated H2 schema, attachment-owner configuration, and release-pending action expectation problems.`
- `2026-07-27 HISTORICAL BLOCKER RESOLVED: user authorized real local write-path E2E with tenant 芋道源码 / admin; the first run failed while inserting the field-audit batch because idempotency_key exceeded varchar(64).`
- `2026-07-27 PASS: idempotency-key regression test now proves both system-write and repeated-open lookup paths generate 64-character SHA-256 keys.`
- `2026-07-27 PASS: isolated worktree backend package completed; yudao-server-exec.jar SHA-256 E2BC50A13F0420E9378F4E9C39D29328F4C1D9201FBBF99A1E91103C90DCF3F9.`
- `2026-07-27 PASS: official login preflight succeeded for 芋道源码/admin against http://127.0.0.1:8086.`
- `2026-07-27 PASS: frontend http://127.0.0.1:8086 returned HTTP 200 and backend http://127.0.0.1:48086/actuator/health returned UP.`
- `2026-07-27 PASS: real Playwright opened batch EDHRB-1785116357526 task 6666, work task 2227, execution 1571, and displayed persisted cell 3:3 value 34126020001.`
- `2026-07-27 PASS: database readback showed field_audit_revision=1, target value present in cell_values_json, exactly one CELL_LINK_AUTO_PREFILL audit batch, and idempotency key length 64.`
- `2026-07-27 PASS: repeated real E2E returned NO_CHANGE_ALREADY_APPLIED and did not append a second audit batch.`
- `2026-07-27 PASS: work task 2227 responsibility restored to user 810 (wangxin), status TODO, updater codex-e2e-rollback.`
- `2026-07-27 PASS: final evidence validators, both frontend static contracts, UTF-8 reads, and task-owned git diff --check completed without errors.`
- `2026-07-27 PASS: cleanup preview/apply kept all seven evidence files with no delete, blocked, or warning entries.`
- `2026-07-27 PASS: task-owned PIDs 51064/60232 stopped after command-line ownership verification; ports 48086/8086 released.`
- `2026-07-27 PASS: original E2E worktree and local branch removed; registry entry 20260727_edhr_cell_link_idempotency marked inactive with cleanupTask 20260727-edhr-cell-link-auto-persist-implementation.`

## Git Evidence

- Implementation baseline commit containing the auto-persist implementation: `b7dc3380 chore: baseline existing worktree changes`.
- `git merge-base --is-ancestor b7dc3380 origin/int_main` -> PASS; implementation is already present on the remote branch.
- Verification record commit: `6b2575da docs: record eDHR cell link auto-persist verification`.
- `git merge-base --is-ancestor 6b2575da origin/int_main` -> PASS.
- `git push origin int_main` -> `Everything up-to-date`; `HEAD` and `origin/int_main` both resolved to `6b2575da` at verification time.
- Concurrency note: after the staged-file check but before commit creation, another task staged `doc/tasks/20260727-form-template-open-fill-binding/execution-log.md`; Git included that already-completed task's two-line push record in `6b2575da`. The event was detected immediately after commit. No history rewrite, revert, or further modification of that task file was performed.
- Final closeout attempt used path-limited `git commit --only`, but a concurrent Git baseline process captured the already-updated task documents in `27dd755a chore: capture concurrent dirty worktree baseline` after the first attempt. That baseline was pushed successfully; no history rewrite or destructive cleanup was used.
- The SHA-256 production/test change is present in remote commit `f18927b9 chore: baseline pre-existing dirty worktree`; `git diff origin/int_main --` for the two owned backend files is empty.
- The paired-runtime/authenticated-readback E2E tooling is present in remote commit `40b7f7b9 chore: preserve dirty workspace before filler entry fix`.
- Final task documentation and durable experience updates were prepared in clean closeout worktree `D:\IntRuoyiWorktree\20260727_edhr_cell_link_closeout` from the latest `origin/int_main` to avoid mixing unrelated concurrent main-workspace changes.

## Final Status

- `completed`: implementation, targeted regression, real Playwright E2E, database readback, fixture rollback, experience consolidation, cleanup apply, task-owned runtime/worktree release, and final evidence refresh are complete.
- The final closeout commit is pushed to `origin/int_main`; no task-owned runtime or original E2E worktree remains.

## Blockers

- No blocker remains for the owned backend/frontend implementation and targeted regression scope.
- Real Playwright verification passed through the authorized `芋道源码/admin` page path on the paired isolated runtime; API-only verification was not used as a substitute.
- Full `MesProEdhrBatchExecutionServiceTest` remains blocked by unrelated existing failures:
  - H2 test schema lacks `bpm_form_template_version.batch_record_report_id`.
  - Batch-record attachment-owner configuration is invalid for `INCOMING_INSPECTION_REPORT` / `batchRecordAttachmentOwners`.
  - `get_releasePendingApproval_locksNormalTaskActions` expects no actions while current behavior returns `OPEN_FORM`, `SAVE_FORM`, and `SUBMIT`.
