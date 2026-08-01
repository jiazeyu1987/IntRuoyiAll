# Execution Log

## User Intent

用户要求实现并验证 PQC 检验员切换来源：

- 切换订单来源是当前活跃订单。
- 切换工序来源是选择的活跃订单对应产品的工艺路线工序。
- 切换员工来源是所有 PQC 员工 + PQC 组长。
- PQC 组长可查看每个 PQC 检验员提交内容，列表内容与检验员填写内容一致，可判定正确性、修正错误内容，并记录提交和修改日志。
- 需确认该口径与生产组长任务不冲突。

## BDD Scenarios

BDD: PQC order selector uses active orders -> Given a PQC inspector opens the fixed template panel / When the order selector loads / Then only active orders are returned and all-order fallback is not allowed.

BDD: PQC process selector uses selected active order route -> Given a PQC inspector selected an active order with product route / When the process selector loads / Then processes come from that product route and missing route fails visibly.

BDD: PQC employee selector uses PQC personnel -> Given a PQC inspector opens the employee selector / When personnel options load / Then the options include all PQC employees and PQC leaders, not unrelated employees.

BDD: PQC leader review is consistent with inspector submissions -> Given PQC inspectors submitted inspection content / When a PQC leader opens the review list / Then list content matches submitted content and correction/submission logs are available.

## Commands And Evidence

- Read backend/frontend/database delivery skills and project trigger rules before implementation.
- Created task directory `doc/tasks/20260801-pqc-active-order-switching/`.
- Read `docs/experience-index.md`; applicable gates recorded in `task.md`.
- Implementation summary:
  - Backend: added PQC active-order context service, active process-pool queries, PQC personnel lookup, PQC switching endpoints.
  - Frontend: PQC order picker now opens active-order options; selected active order loads route processes; selected process loads PQC employees + leaders; employee switch uses PQC endpoint; PQC submit payload maps UI draft to formal `PQC_RESULT`.
  - Non-conflict boundary: production mode still uses device-account process and employee-binding APIs; PQC mode uses separate `/pqc/*` endpoints.

## RED

- RED: `node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js` -> FAIL, expected reason: panel lacked order picker / active-order source contract.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing `MesFrontlinePqcContextService`.

## GREEN

- GREEN: `node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests, 0 failures, 0 errors.
- GREEN: `pnpm ts:check` -> PASS.

## Regression

- REGRESSION: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\process-pool-review-copy-and-revision-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\process-pool-event-revision-api-static.spec.js` -> PASS.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests, 0 failures, 0 errors.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyControllerTest,MesProcessPoolReviewCopyServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 26 tests, 0 failures, 0 errors.

## Evidence Validation

- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260801-pqc-active-order-switching\backend-api-evidence.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260801-pqc-active-order-switching\frontend-feature-evidence.md` -> PASS.
- GREEN: `git diff --check -- <task-owned implementation and doc files>` -> PASS, only CRLF conversion warnings.

## 2026-08-01 Verification Refresh

- GREEN: `node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260801-pqc-active-order-switching\backend-api-evidence.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260801-pqc-active-order-switching\frontend-feature-evidence.md` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests, 0 failures, 0 errors.
- REGRESSION: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\process-pool-review-copy-and-revision-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\process-pool-event-revision-api-static.spec.js` -> PASS.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest,MesProcessPoolReviewCopyControllerTest,MesProcessPoolReviewCopyServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolPqcEventTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 38 tests, 0 failures, 0 errors.

## 2026-08-01 Full PQC / PQC Leader Chain Audit

BDD: PQC inspector submit reaches PQC leader list -> Given a PQC inspector fills active-order/process/employee-based inspection details / When the inspector clicks submit / Then the page must call a formal persistence API, write a process-pool submission event with raw PQC details, and the PQC leader list must display the same `pqcDraft/pqcPieceValues` content with submission and correction logs.

- Audit finding: selector sub-chain is wired, and PQC leader list/review/correction/log sub-chain is wired, but inspector submit currently stops at template payload validation.
- Root cause evidence: `IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue` `handleValidate()` calls `FrontlineTemplateApi.validatePayload(buildFrontlineTemplatePayload(...))` and then `message.success('已提交')`; it does not call `ProFeedbackApi.frontlineSubmit(...)` or any formal PQC submit endpoint.
- Formal submit precondition evidence: `/mes/pro/feedback/frontline/submit` requires `feedbackPayload`, `recordbookPayload`, `processPoolContext`, `actualEmployeeId`, `signatureId`, `signatureEmployeeId`, and `rawPayload`. Backend validation also requires `processPoolContext.deviceAccountUserId` and signature employee consistency.
- PQC context gap: current PQC active-order/process/employee context does not provide all formal submit prerequisites, including `taskId`, `deviceAccountUserId`, `signatureId`, `signatureEmployeeId`, and `recordbookId`. Directly wiring the submit button would require fake/default context, which is forbidden.

## 2026-08-01 Full Chain RED / Regression

- RED: `node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> FAIL, expected reason: PQC/一线提交按钮只调用模板 payload validate 后提示已提交，未调用正式提交接口落库。
- GREEN: `node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\process-pool-review-copy-and-revision-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\process-pool-event-revision-api-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest,MesProcessPoolPqcEventTest,MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolReviewCopyControllerTest,MesProcessPoolReviewCopyServiceTest,MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 37 tests, 0 failures, 0 errors.

## Experience Consolidation

- Checked docs/*memory*.md, docs/experience-index.md, frontend/backend/database/E2E/PowerShell/closeout rules for matching long-term experience destinations.
- No new general engineering lesson was added: applicable gates already exist for static contract isolation, Maven -D quoting, and evidence validation; PQC active-order source is a task-specific product contract captured in this task evidence and tests.
- 2026-08-01 refresh: reviewed `project-experience-consolidation`; `docs/experience-index.md` already routes shared-branch concurrent baseline, selective staging, and residual dirty-worktree risks to `docs/powershell-memory.md`, so no new long-term experience document was created.
- 2026-08-01 full-chain audit: reviewed `project-experience-consolidation` again. The reusable lesson is already covered by `docs/frontend-development.md` no-default-success/no-swallowed-submit guidance, `docs/e2e-rules.md` API-only prohibition, and `docs/backend-development.md` formal persistence/no-default-success rules; the specific PQC submit-context gap is task-local product evidence, so no new long-term experience document was created.

## Blockers

- Closeout blocker: current working tree contains unrelated concurrent changes and branch is already ahead of origin; commit/push not performed to avoid mixing task-owned and unrelated work.
- 2026-08-01 closeout refresh: branch is ahead of `origin/int_main`; recent baseline commits include this task's implementation/evidence together with unrelated concurrent task files. Current working tree still has unrelated dirty/untracked files, so no additional commit/push was performed.
- Full-chain blocker: PQC 检验员提交没有正式落库到 `/mes/pro/feedback/frontline/submit` 或 PQC 专用正式提交接口，且当前 PQC 页面/上下文缺少正式提交所需签名、记录本和工序池来源字段；不得用 validate-only、默认 ID、伪造签名或 API-only 成功替代真实提交。

## 2026-08-01 PQC Submit Chain Optimization

BDD: PQC inspector submit reaches PQC leader list -> Given a PQC inspector fills active-order/process/employee-based inspection details / When the inspector clicks submit / Then the page validates the template payload, persists a formal PQC process-pool event with pqcDraft and pqcPieceValues, and the PQC leader list reads the same originalPayloadJson details.

- Implementation: added MesFrontlinePqcSubmitCommand, MesFrontlinePqcSubmitReqVO, POST /mes/pro/feedback/frontline/device-account/pqc/submit, active pool route-process lookup, and MesFrontlinePqcContextService.submitPqcInspection.
- Backend source boundary: PQC submit inherits deviceAccountId/deviceId/workstationId/feedbackSourceType/feedbackSourceId/recordbookSourceType/recordbookSourceId from the selected active pool latest event; missing source context fails fast.
- Frontend implementation: PQC panel now requires an explicit signatureId, validates the template payload first, calls ProFeedbackApi.submitFrontlinePqcInspection, and only then displays 已提交. rawPayload includes pqcDraft, pqcPieceValues, fieldValues, inspectionResult, selectedActiveOrder, selectedProcess, selectedEmployee, and templatePayload.

RED: node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js -> FAIL, expected reason: old handleValidate path only validated template payload and then showed 已提交.
GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 6 tests, 0 failures, 0 errors.
GREEN: node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js -> PASS.
GREEN: node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js -> PASS.
GREEN: pnpm ts:check -> PASS after extending timeout to 240000 ms; first 120000 ms attempt timed out without failure output.
REGRESSION: node tests\e2e\mes-process-pool-team-leader-static.spec.js -> PASS.
REGRESSION: node tests\e2e\process-pool-review-copy-and-revision-static.spec.js -> PASS.
REGRESSION: node tests\e2e\process-pool-event-revision-api-static.spec.js -> PASS.
REGRESSION: mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolPqcEventTest,MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolReviewCopyControllerTest,MesProcessPoolReviewCopyServiceTest,MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 32 tests, 0 failures, 0 errors.

## 2026-08-01 Experience Consolidation Refresh

- Read project-experience-consolidation. No new long-term memory file was created: the reusable lesson is already covered by existing no-default-success, formal persistence/no API-only, and static contract isolation gates.

## 2026-08-01 Remaining Closeout Blocker

- Current workspace contains unrelated concurrent dirty files and untracked task directories. No commit or push was attempted in this turn to avoid mixing task-owned PQC changes with unrelated work.
- Real browser write-type E2E was not run because local services/login/test write data were not started or prepared in this turn.
