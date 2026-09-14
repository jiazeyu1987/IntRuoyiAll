# Planning Verification Report

## Verdict

- Result: PASS for phase-two planning.
- Implementation: not started; current milestone is P1.

## Product Decision

- 发布时冻结业务可见来源：requester、CURRENT_VIEW_MATRIX、PUBLIC_FOLDER 收件人，经唯一 assignment hard-scope 服务过滤；项目/分类/目录仅为上下文，技术治理访问不虚构为关联方授权。
- 实际站内通知只发给新版本责任人、正式分发对象和影响任务负责人，同一用户合并原因并去重。
- 正向关联和反向引用都创建影响任务，按相关 Master 去重。
- 影响决定为无需升版或需要升版；需要升版不自动建版，只进入/关联现有大版本流程。
- 通知和人工处理不阻塞文件生效；后续账本与快照作为发布事务必需记录，防止后续工作永久丢失。

## Validation

- Change request validator: PASS.
- Product requirements validator: PASS.
- BDD/TDD acceptance planner validator: PASS.
- Roadmap node development plan validator: PASS.
- Acceptance mapping audit: AC-01 through AC-18 all mapped to requirements, milestones, tests and task state.
- Scoped `git diff --check`: PASS.

## P1 Verification

- Independent result: PASS after one corrective loop.
- Transaction integration: 1 test passed using the real H2 transaction manager and real file/Master/follow-up Mappers.
- P1 and adjacent DCC regression: 253 tests, 0 failures/errors.
- SQL migration contract: 3 passed; complete 12-file dependency closure passed.
- Verified rollback: A/1 remains ACTIVE, B/1 is marked FINALIZATION_FAILED only by the separate failure transaction, Master remains on A/1, all seven follow-up tables have zero residue, and the completion event is not emitted.
- Runtime database migration, E2E and 48081 restart were not authorized and remain deferred to P4.
- P1 commit: `b1e08ba6b`, pushed to `origin/int_main`.

## Artifacts

- `prd.md`: product behavior, states, rules, edge cases and AC-01..AC-18.
- `user-flows.md` and `acceptance-criteria.md`: user-visible paths and rejection gates.
- `development-plan.md`: P1-P4 sequence, ownership, gates and stop conditions.
- `test-plan.md`, `bdd-scenarios.md`, `tdd-plan.md`, `e2e-plan.md`, `test-data.md`: executable acceptance package.
- `task-state.json`: resumable state at P1.

## Blockers

- Subagent-driven P1 execution and Git commit/push are authorized for the current turn.
- Database writes, real E2E and `int_main` restart remain outside the current authorization and are deferred to P4.
- The separate DCC related-file migration metadata fix is implemented and will be committed before P1 starts.

## P4 Local Acceptance 2026-09-08

- Result: PASS for local runtime restart, DCC publication-followup page reachability, workbench impact-assessment page reachability, upload-page precheck, and real frontend new-file submit-to-approval.
- Runtime: local `int_main` backend `48081` health `UP`; frontend `8081` HTTP 200; runtime jar SHA256 `B17DBD3C909BE74DEFE04578DCC092D18355672CE7768C1C460743F4406729D9`.
- Page E2E: real login opened `/dcc/controlled-file/publication-followup` and `/dcc/controlled-file/workbench`; target DCC GET requests returned HTTP 200 with business code 0; no page errors or console errors were recorded.
- Upload precheck: real upload page loaded project, category and related-file controls; visible controls included source upload, PDF upload and submit approval.
- Upload submit: first run exposed a test-data precondition (`MDM_PRODUCT_DCC_CODE_INVALID`) on an invalid project; rerun selected valid test project `T07注册证临时项目`, category `技术调研报告`, uploaded the source document and submitted file `DCC-P4-20260908064319ZPS7` successfully through the page.
- Remaining scope: approval, publish, publication-followup batch, notification delivery and related-file impact task closure are not yet claimed by this local E2E.

## P4 Approval Resume 2026-09-08

- Result: BLOCKED at the first approval confirmation.
- The real frontend approval path was exercised against `DCC-P4-202609081528-NEW`.
- The approval dialog opened, but confirmation returned HTTP 500 because signature-evidence digesting could not read the referenced source object (`S3 NoSuchKey`, HTTP 404).
- Approval, publication, notification and impact-task closure therefore remain unverified. No API or database write was used to bypass the failure.

## P4 Approval Root-Cause Correction

- The prior source-object diagnosis was incorrect for the new fixture. `DCC-P4-202609081528-NEW` has a readable MinIO source object and already advanced to `审核会签`.
- The actual retry failure was runtime schema drift: `system_electronic_signature` did not exist in the MySQL database used by `48081`.
- Existing official migrations `20260908_system_electronic_signature_t3.sql`, `t7.sql` and `t8.sql` were applied and repeated successfully. The six `system_electronic_signature*` tables now exist.
- Runtime page re-verification is blocked by an unrelated unresolved Git conflict in the shared frontend, which causes a Vite compiler overlay. The approval transition is therefore not yet claimed as passing.

## Approval Repair 2026-09-08

- Added and validated a forward migration for the unified signature `subject_id` capacity (`varchar(512)`), preserving the full encoded approval identity and remaining within MySQL index limits.
- Applied the migration successfully twice.
- Applied the existing temporary-role migration successfully twice to repair the shared runtime's missing `remind_time` schema used by public tenant/login prerequisites.
- Standard local backend restart completed with Maven `BUILD SUCCESS`; `48081` health is `UP` and `8081` returns HTTP 200.
- A fresh DCC approval transition after these repairs is still pending; the prior retry was affected by concurrent shared frontend/runtime activity.

## P4 Resume Preflight 2026-09-08

- Rechecked local runtime without restarting: `48081` health is `UP` and `8081` returns HTTP 200.
- Rechecked the previously blocking frontend conflict file: `ActiveOrderSubmissionDetailPanel.vue` has no unresolved merge markers.
- Re-ran frontend relaxed type check: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` passed with exit code 0.
- Remaining blocker: fresh real frontend approval/publish/write-path Playwright was not run in this command because the current instruction did not explicitly authorize business writes or service restart.

## Backend Restart Recovery 2026-09-09

- Fixed standard backend restart blockers in System/DCC/MES compile and testCompile paths.
- `mvn -pl yudao-module-system -DskipTests compile` passed.
- `mvn -pl yudao-module-dcc -DskipTests test-compile` passed.
- `mvn -pl yudao-module-mes -am -DskipTests compile` passed.
- `mvn -pl yudao-module-mes -am -DskipTests test-compile` passed.
- Standard local backend restart script completed yudao-server package with BUILD SUCCESS and dispatched the runtime.
- `48081` is listening under PID `31924`; `http://localhost:48081/actuator/health` returned HTTP 200.
- This restores backend runtime for further P4 validation but does not claim the DCC real frontend approval/publish/notification/impact closure.

## P4 Real Frontend Closure Attempt 2026-09-09

- Result: BLOCKED.
- Runtime: `8081` and `48081` reachable; backend health returned `UP`.
- Real frontend upload: PASS. New task-owned file `DCC-P4-20260909P4B` was created and submitted through the upload page. File ID: `2054545668044070331`.
- Read-only task verification: PASS. The file is `PENDING_DOC_CONTROL_REVIEW`; Flowable has current task `81b33998-ac25-11f1-b878-00155dde8c13` assigned to admin.
- Real frontend approval detail: PASS to the point of action. The detail page loaded file data, BPM task list, approval detail and action readiness; readiness returned `ready=true`.
- Real frontend approval write: FAIL. Clicking “审核通过” submitted `/admin-api/dcc/controlled-files/2054545668044070331/approve-task`, but backend returned HTTP 500.
- Root cause: runtime schema drift. Backend log shows `ElectronicSignatureServiceImpl.sign` failed because table `gxp_audit_policy_operation` does not exist.
- Required next gate: apply official migration `IntRuoyiBackend/sql/mysql/20260908_gxp_audit_trail_core.sql` to the local test database, then rerun direct approval, remaining approvers, publish request, follow-up notification delivery, and impact-task decision from real frontend pages.
- Not claimed: approval completion, publication, publication-followup batch, notification delivery, and impact-task closure.

## P4 GxP Migration And Approval Resume 2026-09-09

- Result: PARTIAL PASS, then BLOCKED.
- GxP runtime migration: PASS. Official core SQL was applied to the local test database and repeated successfully.
- GxP SQL contract repair: PASS. Added regression assertions for runtime base fields and DCC signature payload capacity; `python -m pytest IntRuoyiBackend/script/tests/test_gxp_audit_core_contract.py -q` passed 4 tests.
- Runtime schema repair: PASS. `gxp_audit_policy_operation` now has `update_time/creator/updater/deleted`; `gxp_audit_event` now has `update_time/creator/updater/deleted`, `idempotency_key varchar(512)`, `subject_id varchar(2048)`, and prefix subject index.
- Policy registry seed: PASS. `IntRuoyiBackend/config/gxp-audit-policy.yaml` passed coverage gate and was imported into local tenant 1 with 8 active operations, including `signature.record.create`.
- Fresh real frontend fixture: PASS. `DCC-P4-20260909P4C` / `2054545668044070332` was created through the upload page, and its source object exists in MinIO.
- Real frontend approval progress: PASS for DOC_CONTROL_REVIEW and admin MATRIX_REVIEW tasks.
- Current blocker: zhaojie is the active BPM assignee for the remaining MATRIX_REVIEW task, but the DCC detail endpoint returns code `1080000012 Current user cannot access this controlled file` for that same user. Because E2E must use the real frontend and cannot bypass with API/DB approval, remaining review, final approval, publish, notification and impact-task closure are not claimed.

## P4 Current BPM Assignee Detail Access Fix 2026-09-09

- Result: CODE FIX PASS; runtime/E2E continuation still pending.
- Root cause: pending DCC detail and pending-original preview permission trusted route snapshot participants but did not trust the actual current Flowable task assignee.
- Fix verified: current running BPM task assignee can open the pending DCC detail and preview the pending original file even when route snapshot rows are missing or stale.
- Boundary verified: download remains denied for the unpublished pending file; existing `DccControlledFileQueryServiceTest` coverage, including future-stage denial, still passes.
- RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest#getControlledFile_pendingFileAllowsCurrentBpmAssigneeEvenWhenRouteSnapshotMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` failed with code `1080000012`.
- GREEN: same targeted command passed 1 test / 0 failures / 0 errors.
- REGRESSION: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed 98 tests / 0 failures / 0 errors.
- Not yet claimed: the fixed code has not been loaded into `48081`, and the remaining real frontend zhaojie approval, final approval, publish, notification and impact-task closure were not run in this turn.

## P4 Final Closure Evidence 2026-09-10

- Runtime recovered after machine restart: local Docker MySQL/Redis/MinIO are running, backend `48081` health is `UP`, and frontend `8081` is available after a clean Vite restart.
- Real frontend lifecycle closure passed for `DCC-P4-20260910P4D A/1` and related `DCC-P4-20260909P4C B/1`.
- Final states: P4C A/1 `SUPERSEDED`; P4C B/1 and P4D A/1 `ACTIVE`; all three publication batches `COMPLETED` with 7/7 `SENT` notifications per batch.
- Impact task 1 completed initial `NO_REVISION_REQUIRED`, management-page reopen, `REVISION_REQUIRED`, create/link B/1, and automatic `RESOLVED` after B/1 publication. Reverse impact task 2 completed as `NO_REVISION_REQUIRED`.
- P4D and P4C B/1 each contain five `VALID` DCC signature projections using `HMAC_SHA256`.
- Adjacent backend regression passed 162 tests; the three DCC frontend contracts and SQL capacity contract passed.
- Latest read-only real-page Playwright `p4-local-page-e2e-result.json` is `PASS`, with zero page/console errors. Its `timelineText` renders all publication, notification and impact events through linked-version publication resolution.
- Global relaxed `vue-tsc` is currently red only in unrelated parallel MES work at `TeamLeaderWorkbenchPage.vue:6010`. The independent tester recorded it as repository-level residue outside P4 ownership and approved P4-AC1 through P4-AC5 and AC-18.

## Closeout 2026-09-10

- Completion gate: PASS; all phases and phase acceptance criteria are completed, `test_status=passed`, and no blocking prerequisite remains.
- Independent tester: PASS / APPROVED for DCC P4 and AC-18.
- Cleanup preview/apply: PASS; formal task/evidence files were retained and 5165 task-local temporary artifacts were removed.
- Worktree handling: not applicable because this is the primary `int_main` workspace.
