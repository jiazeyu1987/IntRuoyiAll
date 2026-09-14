# Backend API Evidence

## Endpoint And Service Scope

- Endpoint: `POST /mes/pro/feedback/frontline/device-account/pqc/switch-employee`
- Endpoint: `GET /mes/pro/feedback/frontline/device-account/pqc/personnel`
- Service: `MesFrontlinePqcContextService`

## API Contract And Data Contract

- PQC personnel list must represent the current login PQC user/leader identity for one-line PQC fill.
- PQC switch employee command must reject any `actualEmployeeId` that is not the login user.

## Auth, Permissions, Validation, And Error Behavior

- Existing permissions remain unchanged.
- Invalid non-login employee switch must fail fast with a business exception.

## Required Config, Services, Fixtures, And Migrations

- No schema migration expected.
- Target test will use existing service test fixtures/patterns.

## BDD Scenarios

- BDD: 后端拒绝非本人切换 -> Given 登录人调用 PQC 切换员工接口 When 请求的 `actualEmployeeId` 不是登录人 Then 后端返回业务错误，不返回其他人员模板。
- BDD: PQC 人员列表锁定登录人 -> Given 登录人是 PQC 员工或 PQC 组长 When 获取一线 PQC 人员候选 Then 只返回当前登录人的候选记录。

## RED Command And Expected Failure

- RED: Backend service test was updated with `shouldRejectPqcEmployeeSwitchWhenActualEmployeeIsNotLoginUser`, expecting business error code `PRO_FRONTLINE_PQC_EMPLOYEE_NOT_BOUND`.
- Independent backend RED was not completed in this continuation before inheriting the backend implementation; final Maven verification is blocked before tests by existing compile errors outside this task scope.

## GREEN Command And Passing Result

- GREEN: blocked before pass; `MesFrontlinePqcContextServiceTest` did not run because `yudao-module-mes` compile failed in unrelated classes.
- Blocked before GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` hung during Maven incremental compile cleanup.
- Blocked before GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` failed in `compile` before running `MesFrontlinePqcContextServiceTest`.

## Contract Or Integration Verification

- Verification: Source contract updated: `GET /pqc/personnel` passes `getLoginUserId()` into `listPqcEmployeeCandidates(loginUserId)`.
- Service contract updated: `switchPqcActualEmployee` calls `requirePqcEmployee(loginUserId, actualEmployeeId)` before loading active order context.
- Verification blocker: `yudao-module-mes` compile fails in unrelated classes referencing missing nested types: `MesProBatchRecordSharedRowTypeRules.RowType`, `MesProBatchRecordSharedPageTitleRules.SharedPageTitleType`, and `CapacityWindowAllocator.ScheduleWindowResult`.

## Observability Touchpoints

- Business exception path must not be swallowed or mapped to default success.

## Blockers And Downstream Skill Needs

- Existing dirty worktree limits final commit/push completion until coordinated.
- Backend target test cannot be marked passing until the existing MES compile blockers are fixed and the same Maven target is rerun.
