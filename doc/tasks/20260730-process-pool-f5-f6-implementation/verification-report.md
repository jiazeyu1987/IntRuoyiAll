# Verification Report

## Status

completed

## Evidence

- F5/F6 implementation merged into `int_main`:
  - F5 merge after local history filtering: `9c04772b merge: integrate process pool review copy`
  - F6 merge after local history filtering: `5f5fc5d2 merge: integrate process pool event revision`
- Main review against 21-requirement gate:
  - PASS for F5/F6 owned behavior: original records are preserved for review copy, clamp only handles lower/upper limits, original record revision requires new electronic signature and field-level diff, allocated quantity fragments are locked, timeline remains read-only.
  - PASS for existing-system integration boundary: new work is attached to the formal process-pool event model, FIFO lock service, existing MES controller/service/mapper/test patterns, and frontend API wrapper pattern; no existing surplus/resource pool is reused as the process pool.
  - PASS for F5/F6 write-path UI closure: independent hidden routes and pages now call the formal F5/F6 write APIs, and Playwright real write-path E2E passed against `int_main` `8081/48081`.
- Main review fix:
  - RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-review-copy-revision-static.spec.cjs` -> FAIL, direct one-to-many review-copy JOIN could duplicate timeline events.
  - GREEN: same command -> PASS after aggregating review-copy summaries by `tenant_id + event_id`.
- Backend regression:
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopySchemaTest,MesProcessPoolReviewCopyServiceTest,MesProcessPoolReviewCopyControllerTest,MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesProcessPoolEventRevisionDiffContractTest,ProcessPoolTimelineRevisionSummaryTest,MesProcessPoolEventRevisionControllerContractTest,ProcessPoolTimelineTraceabilityTest,ProcessPoolTimelineContentSummaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 33 tests.
- Frontend/static contracts:
  - `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-review-copy-api-static.spec.cjs` -> PASS.
  - `node IntRuoyiFronted\tests\e2e\process-pool-event-revision-api-static.spec.js` -> PASS.
  - `node IntRuoyiFronted\tests\e2e\process-pool-review-copy-and-revision-static.spec.js` -> PASS.
  - `node --check IntRuoyiFronted\tests\e2e\process-pool-review-copy-and-revision.spec.ts` -> PASS.
  - `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-review-copy-revision-static.spec.cjs` -> PASS.
  - `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS.
  - `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-frontend-static.spec.cjs` -> PASS.
  - `pnpm run ts:check` from `IntRuoyiFronted` -> PASS.
- Real frontend E2E:
  - `pnpm run test:e2e process-pool-review-copy-and-revision.spec.ts` from `IntRuoyiFronted` -> PASS, 2 tests.
  - Runtime evidence: old `48081` backend PID `27752` was confirmed as `E:\IntRuoyi` `int_main`, stopped, and replaced by runtime jar `backend-process-pool-f5f6-20260730-114729.jar`; new listener PID `46996`; health `UP`; jar SHA256 `E3956703BE44E84F6D3FAE2BE209716E88F2AAC3A52796A2DCDEE36E02920007`.
  - DB evidence: RUN3 review event `5` saved review copy status `SUBMITTED`, signature `6073011550063`, `pressure raw_value=50`, `corrected_value=40`, `rule_type=CLAMP_TO_MAX`; RUN3 revision event `6` saved revision signature `6073011550064` and updated event raw payload `outputQuantity=91`.
- Backend focused regression after UI closure:
  - First Maven targeted command without `surefire.failIfNoSpecifiedTests=false` failed before `yudao-module-mes` because sibling modules had no matching test classes; this is the expected reactor boundary.
  - `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopySchemaTest,MesProcessPoolEventRevisionSchemaTest,MesProcessPoolReviewCopyControllerTest,MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionDiffContractTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolReviewCopyServiceTest,MesProcessPoolEventRevisionFifoLockTest,ProcessPoolTimelineRevisionSummaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 30 tests.
- Guards:
  - `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, `int_main` frontend `8081`, backend `48081`.
  - `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS.
  - `git diff --check` -> PASS with CRLF conversion warnings only.
- Prior real E2E prerequisite gap:
  - The earlier missing `test:e2e` script and absent write-path spec/page gap is closed by the frontend write entry implementation and passing real Playwright E2E above.
- Closeout:
  - cleanup preview/apply -> PASS, no deletes, no blockers, no warnings.
  - task worktrees `20260730-process-pool-f5-review-copy` and `20260730-process-pool-f6-event-revision` removed with `git worktree remove`.
  - port registry entries for slots `16` and `17` marked inactive.
  - frontend write-entry cleanup preview/apply -> PASS; deleted task-owned Playwright browser artifacts and `IntRuoyiFronted\test-results`; no blockers or warnings.
- Remote push:
  - Concurrent baseline commit after local history filtering: `16020173 chore: baseline concurrent upload evidence update`.
  - Frontend write-entry implementation commit after local history filtering: `1c4ae352 feat: add process pool review and revision write pages`.
  - Frontend write-entry closeout doc commit after local history filtering: `5d6d9940 docs: record process pool write path closeout`.
  - User decision: `doc/tasks/20260729-local-scheduler-tenant-copy/source-tenant-1-full-config.json` must not be committed to Git.
  - History cleanup: `git filter-branch --force --index-filter 'git rm -r --cached --ignore-unmatch doc/tasks/20260729-local-scheduler-tenant-copy/source-tenant-1-full-config.json' --prune-empty -- HEAD --not origin/int_main` removed the 214MB blob from pending history.
  - Push preflight object scan after cleanup: the removed path no longer appears in `git rev-list --objects origin/int_main..HEAD`; largest remaining blob is `doc/tasks/20260729-local-scheduler-tenant-copy/probe-source-full-config-after-role-fix.json`, size `2064369` bytes.
  - Earlier remote status: `git push origin int_main` -> FAIL twice, `Recv failure: Connection was reset`; `git ls-remote --heads origin int_main` later succeeded and returned `d1e9729c1b45935eac4c047b707c81ef1283c44d`.
  - Final push: first retry after cleanup failed with `Recv failure: Connection was reset`; second retry `git push origin int_main` -> PASS, `d1e9729c..9ac4d013 int_main -> int_main`.
  - Final status verification: `git rev-parse HEAD` and `git rev-parse origin/int_main` both returned `9ac4d0131dd9852808df9323b79ef0c5f60629c4`; `git status --short --branch` no longer reports ahead.
