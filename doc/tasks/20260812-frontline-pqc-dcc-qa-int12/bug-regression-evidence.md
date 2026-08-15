# INT12 Bug Regression Evidence

## Expected

Selecting an active order must show the QA-owned process/item list from the locked DCC QA version and submit using activeOrderId + QA task identity. The third active order must not disappear because of product/material/current-QA inference or route-process existence checks.

## Reproduction

Earlier implementation paths used old order identity and incomplete formal submit identity, which could hide valid active orders or allow stale responses to overwrite the current selection.

## Root Cause

The final integration had not fully connected DF10/DF11 formal DTOs to the runtime page, backend submit command, task identity validation, and release-writer resultType contract.

## Scope

- BDD: 第三个活跃订单可见正式 QA 工序 -> Given 活跃订单已通过生产路线锁定唯一 DCC 项目代码并锁定 QA 发布版本, When 一线选择该 activeOrderId, Then 页面读取 QA 自有工序和检验项目，不再用产品、物料、当前 QA 或路线工序存在性推断。
- BDD: 巡检轮次独立 -> Given PATROL_AM 与 PATROL_PM 均为 PENDING, When 只提交上午任务, Then 上午 SUBMITTED 且下午仍 PENDING 并可独立填写提交。
- RED: node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs -> FAIL, runtime still allowed old order identity and stale-response overwrite risk.
- RED: node tests/e2e/frontline-pqc-formal-submit-static.spec.js -> FAIL, submit contract lacked formal identity proof.
- GREEN: node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs -> PASS.
- GREEN: node tests/e2e/frontline-pqc-formal-submit-static.spec.js -> PASS.
- GREEN: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderControllerTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcContextServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 33 tests.
- RED: post-merge frozen test -> FAIL, obsolete route-product-to-DCC inference intercepted the formal locked-QA path.
- GREEN: obsolete compatibility query removed; focused service test 4/4 PASS and frozen regression 33/33 PASS at 2026-08-14T16:43:14+08:00.

## Verification

- Regressions covered: activeOrderId identity, locked QA task projection, actual employee switch, formal submit payload, resultType alignment, and separate AM/PM rule identity.
- Real Playwright write-path E2E was not run because local runtime/test tenant/account/traceable task data were not confirmed; this remains a documented prerequisite, not a fallback.

## Blockers

- Real write-path E2E is blocked by missing confirmed local runtime, test tenant/account, permissions, and traceable active-order/PQC task data.
