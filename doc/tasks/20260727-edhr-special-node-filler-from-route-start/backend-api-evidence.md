# Backend API Evidence

## Scope

- Service: `MesProEdhrBatchExecutionServiceImpl`.
- Response contract: `EdhrBatchExecutionTaskRespVO.fillableUsers` for four special batch record attachment nodes.
- Mutation paths: skip, complete, special attachment upload/delete/save.

## Data Contract

- Reads `configSnapshots.batchRecordAttachmentOwners` from the frozen route snapshot on the batch execution.
- Maps attachment code directly to special node type:
  - `INCOMING_INSPECTION_REPORT`
  - `STERILIZATION_REPORT`
  - `FINISHED_PRODUCT_INSPECTION_REPORT`
  - `FINISHED_PRODUCT_INSPECTION_RECORD`
- Supports direct user sources and role sources; role sources resolve to enabled users through `PermissionApi` and `AdminUserApi`.

## Auth And Validation

- Special node operations now validate against the configured attachment owner users.
- Route close owner / production owner is not accepted as an implicit fallback for special node operations.
- Missing or invalid owner config fails fast through the existing batch attachment owner config error.

## Verification

- GREEN target command passed for special node display, write authorization, and card action context.
- Adjacent `fillableUsers` and special-node attachment regressions passed.
- MES module compile passed through `-am`.
- Route attachment owner and dossier requirement contract tests passed.
- Frontend static contracts for route start batch-record attachments and dossier requirements passed.
- Real Playwright login E2E passed on isolated `int_main` slot 1 runtime: frontend `8082`, backend `48082`.
## BDD

BDD: 特殊工序显示路线开始节点配置的填写人 -> Given route snapshot has batchRecordAttachmentOwners When batch detail is opened Then four special nodes expose fillableUsers from that config.

BDD: 特殊工序操作权限使用对应填写人 -> Given close owner differs from attachment owner When close owner calls special node APIs Then request is rejected; When attachment owner calls Then request is allowed.

## RED

RED: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromStartBatchRecordAttachmentOwnersForSpecialNodes+specialNodeWriteApis_requireConfiguredAttachmentOwnerInsteadOfCloseOwner+get_returnsAttachmentOwnerActionsForPendingSpecialNodes" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected current implementation still used close owner / empty fillableUsers.

## GREEN

GREEN: mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromStartBatchRecordAttachmentOwnersForSpecialNodes+specialNodeWriteApis_requireConfiguredAttachmentOwnerInsteadOfCloseOwner+get_returnsAttachmentOwnerActionsForPendingSpecialNodes" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS.

GREEN: mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromActiveFillWorkTask+detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated+detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated+prepareSpecialNodeAttachmentUpload_returnsTaskScopedMetadata+savePendingSpecialNodeAttachments_booksAllPendingAttachmentsBeforeRelease" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS.

GREEN: mvn -pl yudao-module-mes -am -DskipTests compile -> PASS.

GREEN: mvn -pl yudao-module-mes "-Dtest=MesProRouteBatchRecordAttachmentOwnerServiceTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest,MesProEdhrReleasePrecheckContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS.

GREEN: node tests/e2e/mes-route-flow-start-batch-record-attachments-static.spec.js -> PASS.

GREEN: node tests/e2e/edhr-release-dossier-requirement-setting-static.spec.js -> PASS.

GREEN: Playwright real login E2E `芋道源码/admin` on `http://127.0.0.1:8082` with backend `http://127.0.0.1:48082` -> PASS; `dossier-requirements` and `batch-record-attachment-owners` returned business `code=0`, and attachment owner config returned 4 fixed records with the expected default role names.

## Blockers

- Implementation and required verification are complete.
- Repository closeout remains blocked by unrelated/concurrent dirty main-workspace changes and branch state `int_main...origin/int_main [ahead 11]`; no task commit/push was performed in this mixed workspace.
- Task-owned isolated E2E worktree cleanup remains required before marking the task `completed`.
