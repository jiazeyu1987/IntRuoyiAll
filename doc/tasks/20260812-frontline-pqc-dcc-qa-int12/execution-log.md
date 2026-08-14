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
