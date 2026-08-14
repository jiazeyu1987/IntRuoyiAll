# INT12 Backend API Evidence

## Scope

Integrate the verified DF10/DF11 frontline PQC projection and formal submit contract into the backend controller/service/write paths for activeOrderId, locked QA task identity, actualEmployeeId, canonical payload hash, production event linkage, correction, and release-writer consumption.

## Contract

- BDD: 完整一线执行 -> Given 第三个活跃订单已锁定 QA 版本且目标任务 PENDING, When 选择授权实际员工、正式设备并签名提交, Then 使用 activeOrderId/QA工序/task 身份保存并以 actualEmployeeId 形成签名与事件。
- BDD: 数值全链同判 -> Given NUMERIC 项目上下限和精度已发布, When 提交边界、超范围、精度超限和非法文本, Then 边界通过、合法超范围形成 FAILURE、非法格式与精度超限被拒绝，纠正/放行同判。
- BDD: 幂等与并发唯一 -> Given 同一 PENDING task 收到相同或冲突并发提交, When task 行锁、CAS 与 canonical hash 执行, Then 仅生成一份正式签名/明细/event，相同内容回同一回执，冲突内容零写入拒绝。
- RED: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderControllerTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcContextServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, integration initially missed formal task identity validation and release-writer resultType alignment.
- GREEN: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderControllerTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlinePqcContextServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 33 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS at 2026-08-14T12:07:31+08:00.
- RED: post-merge frozen test -> FAIL, the unused legacy process-query overload inferred DCC from route product codes before locked-QA validation.
- GREEN: post-merge service test -> PASS, 4 tests; legacy overload and product/project-code inference removed.
- GREEN: post-merge frozen regression -> PASS, 33 tests, 0 failures/errors/skips, BUILD SUCCESS at 2026-08-14T16:43:14+08:00.

## Verification

- Active-order PQC process projection remains based on DF10/DF11 formal contract, with QA process/task identity coming from locked active-order QA version and task rule identity.
- Submit path carries activeOrderId, qaProcessId, pqcTaskId, inspectionRuleKey, actualEmployeeId, canonical payload hash, and productionSubmitEventId without product/material/current-QA inference.
- Release writer test data now uses formal PQC resultType values BOOLEAN/NUMERIC/TEXT; target form value types STRING/NUMBER remain separate.

## Validation

- Backend focused Maven passed with 33 tests and BUILD SUCCESS at 2026-08-14T12:07:31+08:00.
- No API-only mock, fallback, default-success, product/material inference, or current-QA lookup was accepted as verification.
- Runtime process projection now exposes only the formal `activeOrderId` service entry; no `workOrderId + routeId` compatibility query remains.

## Blockers

- Real write-path Playwright E2E remains blocked by missing confirmed local runtime, test tenant/account, permissions, and traceable active-order/PQC task data.
