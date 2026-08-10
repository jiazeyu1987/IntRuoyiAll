# Verification Report

## Bug

已修复一线运行态 `frontline runtime deviceId=41` 触发的“班组长工作台缺少负责范围上下文”问题。根因是 route-start 生产组长候选中的 `deviceId` 来自工位正式设备，但运行态解析误把它当成班组设备维护绑定来反查 leader；现在候选显式携带来源，route-start 来源使用正式负责组长，设备账号 post-binding 来源仍保持原设备绑定门禁。

## Expected

班组长工作台读取一线运行态上下文时，route-start 生产组长候选必须使用正式 `routeStartProductionLeaders` 负责范围解析当前组长人员上下文；设备账号 post-binding 候选仍必须使用正式设备/工序绑定，缺失时 fail fast。

## Reproduction

新增 `MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_keepsLeaderScopeWhenRouteStartCandidateDeviceHasNoTeamBinding`，构造 route-start 候选携带 `deviceId=41` 且没有班组设备映射，RED 阶段复现 `PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED` / `frontline runtime deviceId=41`。

## Root Cause

`MesFrontlineRuntimeConfigServiceImpl.resolveLeaderUserIds` 只要看到候选 `deviceId` 就按 `mes_pro_process_pool_team_process_device` 反查 leader，未区分 route-start 生产组长候选里的工作站正式设备和设备账号 post-binding 里的班组维护设备绑定。

## Changed Areas

- `MesFrontlineRouteProcessCandidate` 新增 `contextSource`，区分 `POST_BINDING`、`ROUTE_START_PRODUCTION_LEADER` 和 `PQC_ACTIVE_ORDER`。
- `MesFrontlineDeviceAccountContextServiceImpl` 标记设备账号候选和 route-start 生产组长候选来源。
- `MesFrontlineRuntimeConfigServiceImpl` 对 route-start 候选按正式负责组长解析员工上下文，不再要求工作站正式设备必须已有班组设备绑定。
- `MesFrontlineRuntimeConfigServiceTest` 新增 `deviceId=41` 等价回归用例。

## RED / GREEN

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_keepsLeaderScopeWhenRouteStartCandidateDeviceHasNoTeamBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，复现 `PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED` / `frontline runtime deviceId=41`。
- GREEN: 同一目标 JUnit -> PASS，Tests run: 1, Failures: 0, Errors: 0。

## Regression

- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineProductionEmployeeLeaderProcessScopeTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineSubmitAuthorizationTest,MesFrontlineSubmitIdentityTraceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- Result: Tests run: 23, Failures: 0, Errors: 0, Skipped: 0.
- RECHECK PASS: same regression command rerun on 2026-08-07 22:12 -> Tests run: 23, Failures: 0, Errors: 0, Skipped: 0.
- PASS: `git diff --check -- <task-owned files>`，仅出现 LF/CRLF 提示，无 whitespace error。
- PASS: bug regression evidence validator -> `Bug regression evidence is valid.`

## Design Constraints

- No fallback, silent downgrade, mock success, or swallowed error was introduced.
- Device-account `POST_BINDING` candidates still require formal device/process binding context.
- Route-start production leader candidates now use their formal `routeStartProductionLeaders` responsible-scope context.

## Blockers

- None.
