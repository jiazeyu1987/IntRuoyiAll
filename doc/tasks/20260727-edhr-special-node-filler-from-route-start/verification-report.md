# Verification Report

## Scope

- eDHR batch execution special nodes: `来料检报告`, `灭菌报告`, `成品检报告`, `成品检记录`.
- Source of fillers: route start `configSnapshots.batchRecordAttachmentOwners` in frozen route snapshot.
- Authorization source: same configured attachment owner users, not route close/production owner.

## Passed

- `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromStartBatchRecordAttachmentOwnersForSpecialNodes+specialNodeWriteApis_requireConfiguredAttachmentOwnerInsteadOfCloseOwner+get_returnsAttachmentOwnerActionsForPendingSpecialNodes" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0.
- `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromActiveFillWorkTask+detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated+detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated+prepareSpecialNodeAttachmentUpload_returnsTaskScopedMetadata+savePendingSpecialNodeAttachments_booksAllPendingAttachmentsBeforeRelease" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0.
- `mvn -pl yudao-module-mes -am -DskipTests compile`
- Result: PASS, reactor build through `yudao-module-mes`.
- `mvn -pl yudao-module-mes "-Dtest=MesProRouteBatchRecordAttachmentOwnerServiceTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest,MesProEdhrReleasePrecheckContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS, Tests run: 12, Failures: 0, Errors: 0, Skipped: 0.
- `node tests/e2e/mes-route-flow-start-batch-record-attachments-static.spec.js`
- Result: PASS.
- `node tests/e2e/edhr-release-dossier-requirement-setting-static.spec.js`
- Result: PASS.
- `node tests/e2e/edhr-special-node-filler-display-static.spec.js`
- Result: PASS after RED failure proved special node action rail did not display fillers.
- `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js`
- Result: PASS.
- `node tests/e2e/edhr-special-node-display-name-static.spec.js`
- Result: PASS.

## Real E2E

- Created isolated int_main worktree `D:\IntRuoyiWorktree\edhr-special-node-filler-e2e-20260727`, reserved slot 1: frontend 8082, backend 48082.
- Built clean worktree backend after applying the minimal field-audit compile fix: `mvn -pl yudao-server -am -DskipTests package` -> PASS.
- Installed frontend dependencies: `pnpm install --frozen-lockfile` -> PASS.
- Ran current-task Playwright E2E with real login `芋道源码/admin` on `http://127.0.0.1:8082` against `http://127.0.0.1:48082`.
- Result: PASS, `dossier-requirements` and `batch-record-attachment-owners` endpoints returned business code 0; attachment owner endpoint returned 4 fixed records with default role names.
- Re-ran current-task Playwright E2E on `int_main` real local path `http://localhost:8081` against `http://127.0.0.1:48081`, identity `芋道源码/admin`.
- Result: PASS, batch `900000000878` / `EDHRB-1785132995811`, route `922119` version `V15`, all four special nodes matched route start attachment owners and displayed fillers in the right-side special node action rail.
- Evidence: `doc/tasks/20260727-edhr-special-node-filler-from-route-start/e2e-artifacts/special-node-filler-yudao-real.json`.
- Reverified on 2026-07-27 at 15:43 CST with backend health `UP` and frontend HTTP `200`; the same real Playwright path passed again for batch `900000000878` and route version `361/V15`.
- Reverification performed no MES writes: `createdBatch=null`, `routeSetup=null`, `allowedMesWriteRequests=[]`, `unexpectedMesWriteRequests=[]`.

## Known Regression Blocker

- `node tests/e2e/edhr-special-node-attachment-actions-static.spec.js` -> FAIL at existing assertion “删除待提交附件必须调用后端删除接口”; this is outside the special-node filler display change and remains a separate blocker.

## Remaining Closeout

- Main workspace remains dirty with unrelated/concurrent changes; no task commit/push was performed.
- Temporary worktree cleanup and port registry release remain before marking the task `completed`.
- Final cleanup preview/apply passed and removed only task-owned runtime logs plus the obsolete failure screenshot; the reproducible E2E script, PASS JSON, and PASS screenshot were retained.
- The retained E2E `.cjs` and screenshot `.png` are ignored by repository rules and require explicit force-add when a safe task-owned commit is prepared.
