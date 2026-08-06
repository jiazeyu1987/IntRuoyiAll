# P2 Backend API Evidence - Production Leader Unified Process Config

## Scope

- Endpoint scope: `GET /mes/pro/process-pool/team-leader/process-config/list`, `POST /process-config/device-binding/save`, `POST /process-config/device-parameter-rule/save`.
- Service scope: `MesTeamLeaderProcessConfigServiceImpl`, `MesTeamLeaderRuntimeConfigServiceImpl`, and `MesFrontlineRuntimeConfigServiceImpl`.
- Removed duplicate write path: `MesProcessDeviceParameterRuleService`, `MesProcessDeviceParameterRuleServiceImpl`, `MesProcessDeviceParameterRuleSaveReqBO`, and `MesProcessDeviceParameterRuleServiceTest`.

## API Contract And Data Contract

- Unified list returns one row per authorized `routeProcessId`, with route/process labels, loss reasons, mapped devices, parameter limits, `targetValue`, `actualAverage`, `sampleCount`, `statisticsStartTime`, `statisticsEndTime`, and `statisticsWindowDays=30`.
- Device binding request accepts only `routeProcessId + deviceId`; backend resolves the formal `processId` from `mes_pro_route_process`.
- Parameter save request accepts `routeProcessId + deviceId + parameterCode + lowerLimit + targetValue + upperLimit + valueType`; storage continues using `default_value`, but API/VO/BO expose `targetValue`.
- Parameter identity is `routeProcessId + deviceId + parameterCode`; repeated save updates the existing rule instead of inserting another active rule.

## Auth Permissions Validation Errors

- All new endpoints keep `mes:pro-process-pool-team-leader:query` or `maintain` permissions.
- Unified rows come from `MesTeamLeaderLossReasonService.listLossReasonRows`, which is driven by formal route-start production leader authorization.
- Binding and parameter save call `MesRouteStartProductionLeaderAuthorizationService.assertCanMaintainRouteProcess`; no process-only fallback is used.
- Device binding rejects missing context, non-owned devices, disabled/repairing devices, and unauthorized route processes before insert.
- Parameter save rejects missing `routeProcessId`, unmapped devices, missing target value/value type, and `lowerLimit <= targetValue <= upperLimit` violations before insert/update.

## Required Config Services Fixtures Migrations

- Requires P1 migration contract: `mes_pro_process_pool_device_parameter_rule.route_process_id` and `default_value` are non-null and unique by tenant + route process + device + parameter + deleted.
- Uses existing route process, team device, process-device mapping, loss reason, parameter rule, and process pool event mappers.
- No new fallback config, schema migration, external service, or mock success path was introduced in P2.

## BDD Scenarios

- BDD: P2 授权路线工序统一列表 -> Given 当前生产组长经“工序开始”授权多个路线工序 / When 调用统一工序配置列表 / Then 仅返回授权路线工序，并在每行聚合损耗原因、映射设备、参数目标值和实际平均值。
- BDD: P2 路线工序设备绑定 -> Given 设备属于当前生产组长且状态可用 / When 用 routeProcessId 和 deviceId 保存映射 / Then 后端从正式路线工序解析 processId，未授权或不可用设备不写入。
- BDD: P2 设备参数目标值保存 -> Given 设备已映射到该路线工序 / When 保存参数编码、下限、目标值和上限 / Then 必须满足 lowerLimit <= targetValue <= upperLimit，相同上下文更新原规则。
- BDD: P2 正式提交平均值统计 -> Given 近 30 天存在正式 PRODUCTION_SUBMIT 事件 / When 统一列表读取参数统计 / Then 只统计当前 routeProcessId + deviceId + parameterCode 的 raw_payload.equipmentParameters 数值。
- BDD: P2 空路线工序运行态拒绝 -> Given 历史参数规则缺少 routeProcessId / When 前线运行态加载当前工序参数 / Then 该历史规则不匹配任何当前路线工序。

## RED Command

- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL during `testCompile`; expected missing `MesTeamLeaderProcessConfigService` / BOs and new routeProcess/targetValue controller contract.

## GREEN Command

- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.

## Contract Integration Verification

- `MesTeamLeaderProcessConfigServiceTest` covers authorized unified rows, device/parameter aggregation, 30-day average filtering, and null average / zero samples.
- `MesTeamLeaderRuntimeConfigServiceTest` covers routeProcess device binding, server-side process resolution, mapped-device requirement, range validation, and upsert update path.
- `MesProcessPoolTeamLeaderControllerTest` covers new endpoint mappings, current-login leader injection, `targetValue` request mapping, and response conversion.
- `MesFrontlineRuntimeConfigServiceTest` covers rejection of legacy null `routeProcessId` parameter rules.

## Observability Touchpoints

- Creates `CREATE_DEVICE_PARAMETER_RULE` and `UPDATE_DEVICE_PARAMETER_RULE` maintenance audit records with before/after snapshots.
- Creates `BIND_DEVICE_ROUTE_PROCESS` audit records for new route-process device bindings.
- Malformed production submit `rawPayload` raises explicit `PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED` instead of being silently skipped.

## Blockers And Downstream Needs

- P2 backend target tests pass.
- P3 frontend must switch to `/process-config/list`, `/process-config/device-binding/save`, and `/process-config/device-parameter-rule/save`, and display `targetValue` instead of `defaultValue`.
- P4 still must run broader regression and real browser validation.
