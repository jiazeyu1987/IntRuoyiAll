# Execution Log

## Intent

用户要求在批次执行中，将 `灭菌报告`、`成品检报告`、`成品检记录`、`来料检报告` 四个特殊工序解析为工艺路线 `工序开始` 中设置的填写人。

## BDD

BDD: 特殊工序显示路线开始节点配置的填写人 -> Given 工艺路线版本快照包含 `batchRecordAttachmentOwners` 且 4 个附件配置分别绑定用户/角色 When 打开批次执行详情 Then 4 个特殊工序的 `fillableUsers` 分别等于对应配置解析出的当前租户启用用户。

BDD: 特殊工序操作权限使用对应填写人 -> Given 路线生产负责人和特殊工序附件负责人不是同一人 When 非对应附件负责人尝试跳过/完成/上传该特殊工序 Then 后端拒绝；When 对应附件负责人操作 Then 后端允许并保留既有门禁。

BDD: 普通路线表单填写人不受影响 -> Given 普通路线表单已有工作任务、表单权限规则或路线绑定填写人 When 打开批次执行详情 Then 普通表单仍按既有优先级解析 `fillableUsers`。

BDD: 特殊节点右侧操作区展示填写人 -> Given 批次详情接口返回特殊节点 `fillableUsers` When 用户在批次执行详情选中 `来料检报告`、`灭菌报告`、`成品检报告` 或 `成品检记录` Then 右侧特殊节点操作区显示这些填写人。

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

RED: node tests/e2e/edhr-special-node-filler-display-static.spec.js -> FAIL, expected reason: special node action rail did not render `edhr-batch-detail__special-node-filler` or `resolveTaskCardFillersText(selectedTaskForEvidence)`.

GREEN: node tests/e2e/edhr-special-node-filler-display-static.spec.js -> PASS.

GREEN: node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js -> PASS.

GREEN: node tests/e2e/edhr-special-node-display-name-static.spec.js -> PASS.

REGRESSION BLOCKER: node tests/e2e/edhr-special-node-attachment-actions-static.spec.js -> FAIL at existing assertion "删除待提交附件必须调用后端删除接口，不能只删前端内存"; not caused by special-node filler display change.

## Isolated Runtime E2E

- Created worktree `D:\IntRuoyiWorktree\edhr-special-node-filler-e2e-20260727` on branch `codex/edhr-special-node-filler-e2e-20260727`.
- Reserved int_main slot 1: frontend 8082, backend 48082.
- RED: mvn -pl yudao-server -am -DskipTests package -> FAIL in clean HEAD, `MesProBatchRecordExecutionFieldAuditServiceImpl.java` referenced `currentUserId` and `goldenFingerMode` outside scope.
- GREEN: Applied the same minimal compile fix already present in the main workspace, then mvn -pl yudao-server -am -DskipTests package -> PASS.
- GREEN: pnpm install --frozen-lockfile -> PASS, `node_modules\.bin\vite.cmd` exists.
- GREEN: Playwright real login on `http://127.0.0.1:8082` against `http://127.0.0.1:48082` with identity `芋道源码/admin` -> PASS, `dossier-requirements` and `batch-record-attachment-owners` returned business code 0 and expected fixed fields/4 attachment owner rows.
- Runtime cleanup: stopped task-owned 8082 PID 62648 and 48082 PID 61548 after E2E.

## int_main Real E2E

- Runtime preflight: `http://127.0.0.1:48081/actuator/health` -> `UP`; `http://127.0.0.1:8081/` -> HTTP 200.
- Runtime ownership: frontend 8081 PID 49552 from `E:\IntRuoyi\IntRuoyiFronted`; backend 48081 PID 64760 from `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260727-135041.jar` with repo-root `E:\IntRuoyi\IntRuoyiBackend`.
- First real E2E attempts found legacy data blockers: existing `芋道源码` batches either had no special-node owner snapshot or detail was blocked by `batchRecordAttachmentOwners`; the script then required the route active snapshot to contain 4 configured owner rows with 2-4 enabled users.
- Route setup evidence: route `922119` had DRAFT candidate `361` / `V15` from active `358`; initializing default attachment owners assigned role users and `submit-publish` made `V15` usable for new batches.
- GREEN: node doc\tasks\20260727-edhr-special-node-filler-from-route-start\e2e-special-node-filler-yudao-real.cjs -> PASS, batch `900000000878` / `EDHRB-1785132995811`, route `922119/V15`.
- Evidence JSON: `doc/tasks/20260727-edhr-special-node-filler-from-route-start/e2e-artifacts/special-node-filler-yudao-real.json`.
- Result details: `来料检报告` matched role `来料检报告上传1` users `[1,149]`; `灭菌报告` matched role `灭菌报告上传1` users `[1,149,150,151]`; `成品检报告` matched role `成品检报告上传1` users `[1,149,150]`; `成品检记录` matched role `成品检记录上传1` users `[1,149,150,151]`.
- Final E2E recorded `unexpectedMesWriteRequests: []`; the last pass did not perform MES writes and only verified existing prepared real data plus page display.
- REVERIFY 2026-07-27 15:43 CST: runtime health `48081=UP`, frontend `8081=200`; reran the same Playwright real path and received `PASS: special node fillers match route start owners batch=900000000878 route=922119/361`.
- REVERIFY artifacts: refreshed `special-node-filler-yudao-real.json` and `special-node-filler-yudao-real.png`; JSON again records `createdBatch=null`, `routeSetup=null`, `allowedMesWriteRequests=[]`, and `unexpectedMesWriteRequests=[]`.

## Frontend Display Fix

- Added `IntRuoyiFronted/tests/e2e/edhr-special-node-filler-display-static.spec.js` as RED/GREEN contract.
- Updated `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue` so the special-node action rail renders `edhr-batch-detail__special-node-filler edhr-batch-detail__rail-process-form-filler` and uses `resolveTaskCardFillersText(selectedTaskForEvidence)`.

## Evidence Validation

- GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260727-edhr-special-node-filler-from-route-start\backend-api-evidence.md -> PASS, Backend API evidence is valid.
- GREEN: python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260727-edhr-special-node-filler-from-route-start\bug-regression-evidence.md -> PASS.
- GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260727-edhr-special-node-filler-from-route-start\frontend-feature-evidence.md -> PASS.

## Closeout Gate

- Worktree cleanup preflight: `D:\IntRuoyiWorktree\edhr-special-node-filler-e2e-20260727` is registered on branch `codex/edhr-special-node-filler-e2e-20260727` at HEAD `18e0ca582c0eea1c88bfd1665bfdf2e658c0d21d`.
- Worktree cleanup preflight: ports 8082 and 48082 are no longer listening.
- Worktree cleanup preflight: dirty files are task-owned E2E/runtime artifacts: `MesProBatchRecordExecutionFieldAuditServiceImpl.java` compile fix for isolated clean worktree package verification, and temporary Playwright script `IntRuoyiFronted/tests/e2e/edhr-special-node-route-owner-api-current-task.e2e.cjs`.
- BLOCKED: deleting the dirty isolated worktree requires explicit authorization to discard those task-owned temporary changes under the project worktree deletion gate.

## Task Cleanup

- CLEANUP PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-edhr-special-node-filler-from-route-start --mode preview -> PASS, keep `task.md`, `execution-log.md`, `verification-report.md`, `backend-api-evidence.md`; delete only task-owned E2E foreground logs.
- CLEANUP APPLY: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-edhr-special-node-filler-from-route-start --mode apply -> PASS, deleted task-owned E2E foreground logs and removed empty `e2e-artifacts` directory.
- GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260727-edhr-special-node-filler-from-route-start\backend-api-evidence.md -> PASS after cleanup.
- CLEANUP REAPPLY after final real E2E: preview -> PASS with no blockers/warnings; apply -> PASS, deleted only `backend-48081-special-node-filler.stderr.log`, `backend-48081-special-node-filler.stdout.log`, and `special-node-filler-yudao-real-failure.png`.
- CLEANUP KEEP confirmed: reproducible Playwright script, PASS JSON, PASS screenshot, backend API evidence, bug regression evidence, frontend feature evidence, and the three core task records.
- The retained `.cjs` and `.png` are covered by repository ignore rules; a future task-owned commit must explicitly force-add them rather than silently omitting them.

## Experience Consolidation

- Checked existing durable rules for `请求地址不存在`, old runtime Jar, paired worktree URLs, real login, and API-only restrictions.
- Existing coverage found in `docs/e2e-rules.md#worktree-隔离运行态-url-门禁` and `docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁`.
- Updated `docs/e2e-rules.md#edhr-单据填写人显示值门禁`: special-node verification must cover both backend `fillableUsers` and the selected node's right-side action rail; API correctness alone is insufficient.
- Existing `docs/experience-index.md` keywords `fillableUsers` and `页面填写人断言` already route to this section, so no index edit or new long-term document was needed.

## Final Reverification

- GREEN: `node tests/e2e/edhr-special-node-filler-display-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-special-node-display-name-static.spec.js` -> PASS.
- GREEN: backend health `UP`, frontend HTTP `200`.
- GREEN: backend API, bug regression, and frontend feature evidence validators -> PASS.
- GREEN: `git diff --check` for task-owned implementation, tests, task records, evidence, and `docs/e2e-rules.md` -> PASS; only Git line-ending conversion warnings were emitted.
- EXPECTED ADJACENT BLOCKER: `node tests/e2e/edhr-special-node-attachment-actions-static.spec.js` -> FAIL at the pre-existing pending-attachment delete API assertion; this does not invalidate the focused special-node filler behavior but prevents claiming the wider attachment action suite is green.

## Runtime Start 2026-07-27

- Confirmed `int_main` runtime contract: frontend `8081`, backend `48081`, workspace `E:\IntRuoyi`.
- Frontend started from `E:\IntRuoyi\IntRuoyiFronted` and returned HTTP `200`; listener PID `41928`.
- First backend launch failed fast because the configured local Docker MySQL endpoint was not yet accepting connections.
- Confirmed the existing `int-ruoyi-mysql` and `int-ruoyi-redis` containers became available on configured ports `23306` and `26379`; no datasource or application configuration was changed.
- Retried the same backend startup script; `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` started successfully on `48081`, listener PID `41520`, health status `UP`.
