# Execution Log

## Intent

用户要求在批次执行中，将 `灭菌报告`、`成品检报告`、`成品检记录`、`来料检报告` 四个特殊工序解析为工艺路线 `工序开始` 中设置的填写人。

## BDD

BDD: 特殊工序显示路线开始节点配置的填写人 -> Given 工艺路线版本快照包含 `batchRecordAttachmentOwners` 且 4 个附件配置分别绑定用户/角色 When 打开批次执行详情 Then 4 个特殊工序的 `fillableUsers` 分别等于对应配置解析出的当前租户启用用户。

BDD: 特殊工序操作权限使用对应填写人 -> Given 路线生产负责人和特殊工序附件负责人不是同一人 When 非对应附件负责人尝试跳过/完成/上传该特殊工序 Then 后端拒绝；When 对应附件负责人操作 Then 后端允许并保留既有门禁。

BDD: 普通路线表单填写人不受影响 -> Given 普通路线表单已有工作任务、表单权限规则或路线绑定填写人 When 打开批次执行详情 Then 普通表单仍按既有优先级解析 `fillableUsers`。

## Evidence

- Task directory created: `doc/tasks/20260727-edhr-special-node-filler-from-route-start`.
- Experience preflight: PASS, matched `docs/backend-development.md#edhr-详情回填门禁` and `docs/backend-development.md#edhr-批次任务配置来源门禁`.

## RED / GREEN / Regression

RED: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromStartBatchRecordAttachmentOwnersForSpecialNodes+specialNodeWriteApis_requireConfiguredAttachmentOwnerInsteadOfCloseOwner+get_returnsAttachmentOwnerActionsForPendingSpecialNodes" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason: special node fillableUsers empty, close owner still allowed, attachment owner action card unresolved.

GREEN: mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromStartBatchRecordAttachmentOwnersForSpecialNodes+specialNodeWriteApis_requireConfiguredAttachmentOwnerInsteadOfCloseOwner+get_returnsAttachmentOwnerActionsForPendingSpecialNodes" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0.

REGRESSION: mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromActiveFillWorkTask+detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated+detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated+prepareSpecialNodeAttachmentUpload_returnsTaskScopedMetadata+savePendingSpecialNodeAttachments_booksAllPendingAttachmentsBeforeRelease" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0.

GREEN: mvn -pl yudao-module-mes -am -DskipTests compile -> PASS, reactor build through yudao-module-mes succeeded.

GREEN: mvn -pl yudao-module-mes "-Dtest=MesProRouteBatchRecordAttachmentOwnerServiceTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest,MesProEdhrReleasePrecheckContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 12, Failures: 0, Errors: 0, Skipped: 0.

GREEN: node tests/e2e/mes-route-flow-start-batch-record-attachments-static.spec.js -> PASS.

GREEN: node tests/e2e/edhr-release-dossier-requirement-setting-static.spec.js -> PASS.

## Isolated Runtime E2E

- Created worktree `D:\IntRuoyiWorktree\edhr-special-node-filler-e2e-20260727` on branch `codex/edhr-special-node-filler-e2e-20260727`.
- Reserved int_main slot 1: frontend 8082, backend 48082.
- RED: mvn -pl yudao-server -am -DskipTests package -> FAIL in clean HEAD, `MesProBatchRecordExecutionFieldAuditServiceImpl.java` referenced `currentUserId` and `goldenFingerMode` outside scope.
- GREEN: Applied the same minimal compile fix already present in the main workspace, then mvn -pl yudao-server -am -DskipTests package -> PASS.
- GREEN: pnpm install --frozen-lockfile -> PASS, `node_modules\.bin\vite.cmd` exists.
- GREEN: Playwright real login on `http://127.0.0.1:8082` against `http://127.0.0.1:48082` with identity `芋道源码/admin` -> PASS, `dossier-requirements` and `batch-record-attachment-owners` returned business code 0 and expected fixed fields/4 attachment owner rows.
- Runtime cleanup: stopped task-owned 8082 PID 62648 and 48082 PID 61548 after E2E.

## Evidence Validation

- GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260727-edhr-special-node-filler-from-route-start\backend-api-evidence.md -> PASS, Backend API evidence is valid.

## Closeout Gate

- Worktree cleanup preflight: `D:\IntRuoyiWorktree\edhr-special-node-filler-e2e-20260727` is registered on branch `codex/edhr-special-node-filler-e2e-20260727` at HEAD `18e0ca582c0eea1c88bfd1665bfdf2e658c0d21d`.
- Worktree cleanup preflight: ports 8082 and 48082 are no longer listening.
- Worktree cleanup preflight: dirty files are task-owned E2E/runtime artifacts: `MesProBatchRecordExecutionFieldAuditServiceImpl.java` compile fix for isolated clean worktree package verification, and temporary Playwright script `IntRuoyiFronted/tests/e2e/edhr-special-node-route-owner-api-current-task.e2e.cjs`.
- BLOCKED: deleting the dirty isolated worktree requires explicit authorization to discard those task-owned temporary changes under the project worktree deletion gate.

## Task Cleanup

- CLEANUP PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-edhr-special-node-filler-from-route-start --mode preview -> PASS, keep `task.md`, `execution-log.md`, `verification-report.md`, `backend-api-evidence.md`; delete only task-owned E2E foreground logs.
- CLEANUP APPLY: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-edhr-special-node-filler-from-route-start --mode apply -> PASS, deleted task-owned E2E foreground logs and removed empty `e2e-artifacts` directory.
- GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260727-edhr-special-node-filler-from-route-start\backend-api-evidence.md -> PASS after cleanup.

## Experience Consolidation

- Checked existing durable rules for `请求地址不存在`, old runtime Jar, paired worktree URLs, real login, and API-only restrictions.
- Existing coverage found in `docs/e2e-rules.md#worktree-隔离运行态-url-门禁` and `docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁`; no new long-term experience document is needed.
