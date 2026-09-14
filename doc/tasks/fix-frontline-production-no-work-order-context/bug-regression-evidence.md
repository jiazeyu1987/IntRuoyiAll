# Bug Regression Evidence：一线生产取消工单匹配上下文

## Bug Summary And Expected Behavior

- Bug：一线员工提交正式生产数据时，系统把一线生产误绑定到 activeOrder/workOrder/task/recordbook 上下文；同时电子签名链路容易被误解为当前登录账号签名。
- Expected：一线生产不需要匹配任何工单；只要签名员工与选择的实际填写员工一致即可完成电子签名和提交。登录账号可以是设备账号或组长账号，但不得替代实际填写员工签名。

## Reproduction

- Reproduction command: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolEventServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Reproduction path: 构造无 activeOrder/workOrder/task/recordbook 的一线生产运行态和正式提交请求。
- Previous failure: 旧逻辑要求 productionSubmitContext 匹配 activeOrder/workOrder/task/recordbook，导致无工单一线生产无法加载或提交。

## Root Cause

- 生产报工链路复用了工序池/PQC 的工单上下文假设，把 workOrderId、taskId、itemId、recordbookId 作为一线生产正式提交前置条件。
- 运行态 `productionSubmitContext` 由工单/任务/记录本推导，而不是由一线生产实际需要的路线、工序、工位、设备账号和选择员工签名驱动。

## Regression Test

- Updated: `MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_doesNotRequireActiveOrderWhenFrontlineProductionHasNoWorkOrder`
- Updated: `MesProFrontlineFeedbackSubmitServiceTest#shouldSubmitFrontlineProductionWithoutWorkOrderTaskItemOrRecordbook`
- Updated: `MesProFrontlineFeedbackSubmitServiceTest#shouldSignAsSelectedEmployeeWhenDeviceAccountIsLoggedIn`
- Updated frontend static contracts: `frontline-formal-submit-static.spec.cjs`、`frontline-formal-submit-selected-employee-static.spec.cjs`、`role-matrix-ac-m10-sop-production-static.spec.cjs`

## RED

- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolEventServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before fix，旧逻辑要求 activeOrder/workOrder/task/recordbook。

## GREEN

- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolEventServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，22 tests，0 failures，0 errors。
- GREEN: `node tests\e2e\frontline-formal-submit-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\frontline-formal-submit-selected-employee-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\role-matrix-ac-m10-sop-production-static.spec.cjs` -> PASS。

## Verification

- Verification: 运行态 `productionSubmitContext` 返回 routeId、routeProcessId、processId、workstationId、approveUserId，并保持 workOrderId、taskId、itemId、recordbookId 为空。
- Verification: 正式提交可在没有 workOrderId、taskId、itemId、recordbookPayload 时创建反馈和工序池生产提交事件。
- Verification: 电子签名服务按所选员工/实际填写员工记录签名，登录设备账号不需要等于实际填写员工。

## Risk And Regression Scope

- Protected scope: 一线生产正式提交无工单路径、所选员工电子签名、工序池生产提交幂等、无记录本生产提交。
- Preserved scope: PQC/检验链路仍要求 activeOrder/workOrder/task/recordbook 来源，不按生产无工单口径放宽。
- Data risk: 新迁移只放开一线生产必要的 nullable 字段并添加生成列唯一键，不删除业务数据。

## Blockers And Follow-Up

- BLOCKED: 相邻 JUnit `MesP0FrontlineSubmitIdempotencyTest,MesFrontlineEmployeeSwitchServiceTest` 因同模块并发 Maven 占用共享 `target` 未补跑。
- Follow-up: 等并发 Maven 结束后补跑相邻 JUnit；如果失败，按失败栈继续修正并追加 GREEN 证据。
