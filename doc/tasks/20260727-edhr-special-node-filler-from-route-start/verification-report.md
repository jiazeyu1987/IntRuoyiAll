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

## Real E2E

- Created isolated int_main worktree `D:\IntRuoyiWorktree\edhr-special-node-filler-e2e-20260727`, reserved slot 1: frontend 8082, backend 48082.
- Built clean worktree backend after applying the minimal field-audit compile fix: `mvn -pl yudao-server -am -DskipTests package` -> PASS.
- Installed frontend dependencies: `pnpm install --frozen-lockfile` -> PASS.
- Ran current-task Playwright E2E with real login `芋道源码/admin` on `http://127.0.0.1:8082` against `http://127.0.0.1:48082`.
- Result: PASS, `dossier-requirements` and `batch-record-attachment-owners` endpoints returned business code 0; attachment owner endpoint returned 4 fixed records with default role names.

## Remaining Closeout

- Main workspace remains dirty with unrelated/concurrent changes and branch is ahead of `origin/int_main`; no task commit/push was performed.
- Temporary worktree cleanup and port registry release remain before marking the task `completed`.
