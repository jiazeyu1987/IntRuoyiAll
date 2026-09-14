# Backend API Evidence

## Endpoint, Service, Job, Or Handler Scope

- Scope: `MesProFrontlineFeedbackSubmitServiceImpl` formal production submit path.
- Scope: process pool submit event creation and timeline projection.
- Scope: loss reason and device parameter validation for current route process configuration.

## API Contract And Data Contract

- Submit payload accepts `lossDetails[]`, `selectedDevice`, and `deviceParameterReadings[]`.
- `lossQuantity` must equal the sum of `lossDetails[].quantity`.
- Device parameter readings preserve value, lower limit, upper limit, unit, and `parameterStatus`.
- Timeline response projects `outputQuantity`, `lossQuantity`, `lossDetails`, `selectedDevice`, and `deviceParameterReadings`.

## Auth, Permissions, Validation, And Error Behavior

- Existing device-account login and submit authorization remain before write.
- Loss reasons are validated through current `routeProcessId`.
- Selected device must exist, be enabled, and be bound to the current `processId` and leader configuration.
- Parameter rules are validated by `routeProcessId`, `processId`, `deviceId`, leader user, and `parameterCode`.
- Out-of-range parameter values are normalized to abnormal status but are not rejected.
- Missing or mismatched formal configuration fails fast with business errors; no default-success or mock path was added.

## Required Config, Services, Fixtures, And Migrations

- Required services: feedback submit service, recordbook entry service, process pool event service, submit authorization service.
- Required config: team leader loss reasons, process-device binding, device parameter rules.
- Fixtures: Java contract tests build task-local submit payloads and mocks.
- Migrations: none.

## BDD Scenarios

- BDD: 多损耗原因提交 -> Given 当前工序配置多个损耗原因, When 提交报工, Then 明细数组和总损耗一致并可保存。
- BDD: 损耗合计校验 -> Given 明细合计与总损耗不一致, When 后端处理, Then 拒绝提交且不写入事件。
- BDD: 设备参数作用域 -> Given 设备和参数规则由班组长按工序配置, When 提交参数读数, Then 按当前工序/设备/参数校验。
- BDD: 参数超限允许提交 -> Given 参数值超出上下限, When 提交, Then 后端保存异常状态但不拒绝。

## RED Command And Expected Failure

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitDetailContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 旧后端缺少结构化明细合计校验、设备参数快照和读模型投影。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigProcessScopeTest,MesProcessPoolTimelineSubmissionPayloadDisplayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增配置作用域与时间轴结构化字段合同未满足。

## GREEN Command And Passing Result

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitDetailContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests, 0 failures.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigProcessScopeTest,MesProcessPoolTimelineSubmissionPayloadDisplayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests, 0 failures.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackRawLimitBypassTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackSubmitRollbackTest,MesP0ProductionSubmitClosedLoopContractTest,MesProFrontlineFeedbackSubmitDetailContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 16 tests, 0 failures.

## Contract Or Integration Verification

- `MesProFrontlineFeedbackSubmitDetailContractTest` covers structured submit details and loss total behavior.
- `MesFrontlineRuntimeConfigProcessScopeTest` covers current-process validation boundaries.
- `MesProcessPoolTimelineSubmissionPayloadDisplayTest` covers timeline/read-model projection fields.
- Existing submit service, raw limit bypass, route order, rollback, and P0 closed-loop tests remain GREEN.

## Observability Touchpoints

- Business validation failures continue through project service exception codes.
- No exception swallowing or silent downgrade was introduced.
- Raw payload retains structured snapshots for audit/review surfaces.

## Blockers And Downstream Skill Needs

- No backend blocker remains for targeted Maven verification.
- No database schema or migration skill was required.
