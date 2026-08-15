# Execution Log

## User Intent

以 DF10/DF11 正式合同为准完成一线 PQC 最终集成；旧冲突 patch 仅备份，不套回。

## BDD Scenarios

- BDD: 完整一线执行 -> Given 第三个活跃订单已锁定 QA 版本且目标任务 PENDING, When 选择授权实际员工、正式设备并签名提交, Then 使用 activeOrderId/QA工序/task 身份保存并以 actualEmployeeId 形成签名与事件。
- BDD: 人员状态无持久化 -> Given 任务A已选择人员并填写草稿, When 切换订单、QA工序、task或刷新, Then 清空实际人员和草稿，重新 switch 成功前禁止提交。
- BDD: 数值全链同判 -> Given NUMERIC 项目上下限和精度已发布, When 提交边界、超范围、精度超限和非法文本, Then 边界通过、合法超范围形成 FAILURE、非法格式与精度超限被拒绝，纠正/放行同判。
- BDD: 幂等与并发唯一 -> Given 同一 PENDING task 收到相同或冲突并发提交, When task 行锁、CAS 与 canonical hash 执行, Then 仅生成一份正式签名/明细/event，相同内容回同一回执，冲突内容零写入拒绝。
- BDD: 巡检轮次独立 -> Given PATROL_AM 与 PATROL_PM 均为 PENDING, When 只提交上午任务, Then 上午 SUBMITTED 且下午仍 PENDING 并可独立填写提交。
- BDD: 快速切换不串数据 -> Given 订单A请求慢于订单B, When 用户切换到B, Then A响应不得覆盖B的工序、任务、人员或草稿。
- BDD: 工序响应不复制任务身份 -> Given 同一 QA 工序存在多个 PQC 任务选项, When 后端返回工序响应, Then 每个任务身份只存在于 `pqcTaskOptions`，工序顶层不再复制 `pqcTaskId`、规则、状态、检验类型、业务日期、班次、轮次或计划数量。

## Command Intent

- First execute the frozen focused tests as the baseline/RED probe.
- Modify production code only after a test demonstrates a missing formal behavior.
- Run real Playwright only after local runtime, tenant, account, permissions, and traceable data are confirmed.

## Current Evidence

- Worktree created from `int_main` commit `817687224`.
- Runtime slot reserved: slot 6, frontend 8087, backend 48087.
- RED: `node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs` -> FAIL, runtime still used the old workOrderId/routeId switch identity.
- RED: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> FAIL, submit contract lacked formal activeOrderId/task/rule identity.
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderControllerTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcContextServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, integration initially missed formal task identity validation and release-writer resultType alignment.
- GREEN: `node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs` -> PASS, frontline PQC QA process runtime contract.
- GREEN: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS, frontline PQC formal submit static contract.
- GREEN: `pnpm ts:check` -> PASS, exit 0.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderControllerTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcContextServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 33 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS at 2026-08-14T12:07:31+08:00.
- M4 completed: backend/frontend validators, diff check, and forbidden scans pending final run after evidence update.
- Root handoff recheck: GREEN: `node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs` -> PASS.
- Root handoff recheck: GREEN: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS.
- Root handoff recheck: GREEN: `pnpm ts:check` -> PASS, exit 0.
- Root handoff recheck: GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderControllerTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcContextServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 33 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS at 2026-08-14T12:52:53+08:00.
- Root handoff recheck: Verification -> PASS, backend/frontend/bug evidence validators, git diff --check, frontend formal forbidden scan, and backend scoped formal forbidden scan.
- RED: post-merge focused Maven -> FAIL, 34 tests with 1 failure and 1 error because the merged legacy `workOrderId + routeId` process query still inferred DCC from route product codes and returned error `1040760103` before locked-QA task validation.
- Root cause: the legacy overload and its private product/project-code resolver remained alongside the formal `activeOrderId` endpoint, contradicting the frozen rule that runtime reads only the active-order DCC/QA/version snapshots.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -rf :yudao-module-mes` -> PASS, 4 tests, 0 failures/errors/skips, BUILD SUCCESS at 2026-08-14T16:32:16+08:00.
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderControllerTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcContextServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -rf :yudao-module-mes` -> PASS, 33 tests, 0 failures/errors/skips, BUILD SUCCESS at 2026-08-14T16:43:14+08:00.
- GREEN: post-restart runtime static contract and formal submit static contract -> PASS; `pnpm ts:check` -> PASS.
- M5 blocked: real Playwright write-path E2E still requires confirmed local runtime, test tenant/account, permissions, and traceable active-order/PQC task data. No mock or API-only substitute used.

## 2026-08-15 Frontend Static Contract Closeout Recheck

- DEPENDENCY: confirmed `IntRuoyiFronted/node_modules`, `node_modules/.bin/cross-env.cmd`, and `node_modules/.bin/vue-tsc.cmd` exist before installation.
- GREEN: `pnpm install --frozen-lockfile` -> PASS, exit 0; lockfile was current and dependencies were already up to date. pnpm reported the configured ignored-build-script warning without failing installation.
- GREEN: `pnpm ts:check` -> PASS, exit 0.
- GREEN: `node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs` -> PASS, exit 0, `PASS: frontline PQC QA process runtime contract`.
- GREEN: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS, exit 0, `PASS: frontline PQC formal submit static contract`.
- VALIDATOR: frontend-feature, backend-api, and bug-regression evidence validators -> PASS, exit 0; all three validator self-tests -> PASS, exit 0.
- STATIC: `git diff --check` -> PASS, exit 0; only existing LF-to-CRLF working-copy advisories were emitted.
- BLOCKED: precise formal-contract forbidden scan -> FAIL, exit 1. The frontend API/page/context portion had zero violations, but the backend `MesFrontlinePqcProcessRespVO` still declares eight process-level task compatibility fields (`pqcTaskId`, `inspectionRuleKey`, `taskStatus`, `inspectionType`, `businessDate`, `shiftCode`, `roundNo`, `plannedInspectionQuantity`) and `MesFrontlinePqcContextServiceImpl` still writes all eight. This produced 16 violations against `interface-contracts.md` section 2, which places task identity only in `pqcTaskOptions`.
- No production-code fix was made during this verification-only continuation. The task returned to `blocked`; no fallback, compatibility acceptance, or silent downgrade was used.
- EXPERIENCE: consolidated the reusable cross-layer response-contract gate into the existing `docs/backend-development.md` PQC DCC-QA target-state section and added matching `docs/experience-index.md` keywords; no new long-term document was created.

## 2026-08-15 Formal Response Contract Remediation

- PREREQUISITE: formal 18-file MES compile prerequisite commit `90b2e1e73` was independently traced to `2810aec91`; it was cherry-picked to `int_main` as `254bb6181` after branch runtime guard PASS.
- PRESERVATION: two newer scheduler-workbench files differed from the formal prerequisite. Both were backed up with hashes, removed for the cherry-pick, and restored byte-for-byte afterward; they remain unrelated unstaged modifications.
- BDD: 工序响应不复制任务身份 -> Given 同一 QA 工序存在多个 PQC 任务选项/When 后端返回工序响应/Then 任务身份只存在于 `pqcTaskOptions`，工序顶层无八个重复字段。
- RED PROBE: focused `MesFrontlinePqcContextServiceTest` command reached Surefire and was required to fail only on the exact response-shape assertion before production changes.
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL as expected, Surefire ran 5 tests with exactly 1 failure; the new assertion found `[pqcTaskId, inspectionRuleKey, taskStatus, inspectionType, businessDate, shiftCode, roundNo, plannedInspectionQuantity]` at process-response top level.
- IMPLEMENTATION: removed the eight process-level fields, the service's arbitrary pending-task projection, and the unused controller compatibility mapper that still wrote six removed fields. All per-task identity and inspection data remains in each `pqcTaskOptions` entry.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -rf :yudao-module-mes` -> PASS, 5 tests, 0 failures/errors/skips, BUILD SUCCESS at 2026-08-15T15:34:57+08:00.
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest,MesFrontlinePqcEmployeeSwitchServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcSubmissionConcurrencyTest,MesProcessPoolPqcInspectionCorrectionServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 44 tests, 0 failures/errors/skips, BUILD SUCCESS at 2026-08-15T15:39:40+08:00.
- FRONTEND DEPENDENCY: confirmed `pnpm-lock.yaml`, `node_modules`, and `node_modules/.bin/vue-tsc.cmd`; pnpm 10.22.0 and Node v24.12.0 are available.
- GREEN: `pnpm ts:check` -> PASS, exit 0.
- GREEN: `node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs` -> PASS, exit 0, `PASS: frontline PQC QA process runtime contract`.
- GREEN: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS, exit 0, `PASS: frontline PQC formal submit static contract`.
- VALIDATOR: final frontend-feature, backend-api, and bug-regression evidence validators -> PASS, exit 0; all three validator self-tests -> PASS, exit 0.
- STATIC: final `git diff --check` -> PASS, exit 0; only existing LF-to-CRLF working-copy advisories were emitted.
- FORBIDDEN: precise backend/frontend formal-response scan -> PASS, exit 0, 0 violations. The eight task fields are absent from the outer backend/frontend process response, present in task options, have no process-level backend writer or frontend reader, and the process API uses `activeOrderId` only.
- CLEANUP PREVIEW: PASS with delete set empty after formally preserving the three validator evidence files and the prerequisite overlap backup; apply was not run because full INT12 remains blocked on real write-path prerequisites.
- EXPERIENCE: project-level response-contract symmetry and negative-scan guidance already exists in `docs/backend-development.md` and is indexed by `docs/experience-index.md`; no duplicate or new long-term document was created.
- STATUS: the requested static-contract closeout is PASS. Full INT12 remains BLOCKED only on the unconfirmed real Playwright write-path prerequisites; no mock or API-only substitute was used.

## 2026-08-15 User Verification Waiver

- USER: `不用测试，继续推进`.
- DECISION: no additional test, runtime startup, login, data preparation, Playwright, API-only substitute, or mock verification was run after this instruction.
- RISK: the real signed write-path remains unverified in a confirmed tenant/account/data environment; the completed static and backend regression evidence is retained but is not presented as real E2E evidence.
- STATUS: `ready_for_closeout`; proceed with cleanup preview/apply and final task records.

## 2026-08-15 Final Closeout

- CLEANUP PREVIEW: PASS, delete/blocked/warning sets were empty and all declared evidence/backup paths were retained.
- CLEANUP APPLY: PASS, `deleted_paths` was empty; current worktree is the primary `int_main` worktree, so no merge or primary-worktree removal was performed.
- INTEGRATION: implementation commit `389c7bf9e` follows prerequisite commit `254bb6181` on local `int_main`; no remote push was performed.
- EXPERIENCE: existing response-contract symmetry guidance remains in `docs/backend-development.md` and `docs/experience-index.md`; no duplicate long-term document is needed.
- FINAL STATUS: `completed` with the user-approved real Playwright verification exception recorded above.
