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

## Experience Consolidation

- Checked docs/*memory*.md, docs/experience-index.md, frontend/backend/database/E2E/PowerShell/closeout rules for matching long-term experience destinations.
- No new general engineering lesson was added: applicable gates already exist for static contract isolation, Maven -D quoting, and evidence validation; PQC active-order source is a task-specific product contract captured in this task evidence and tests.

## Blockers

- Closeout blocker: current working tree contains unrelated concurrent changes and branch is already ahead of origin; commit/push not performed to avoid mixing task-owned and unrelated work.
- 2026-08-01 closeout refresh: branch is `int_main...origin/int_main [ahead 3]`; recent baseline commits `7186c11a2` and `c64cc99b4` include this task's implementation/evidence together with unrelated concurrent task files. Current working tree still has unrelated dirty/untracked files, so no additional commit/push was performed.
