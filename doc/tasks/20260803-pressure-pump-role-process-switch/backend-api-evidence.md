# Backend API Evidence: Pressure Pump All-Process Role Switch

## Scope

- Service: `MesFrontlineDeviceAccountContextServiceImpl`
- Public behavior: `listSwitchableProcesses(loginUserId)` and dependent authorization checks for one-line production filling.
- Permission: `mes:pro-feedback:frontline-pressure-pump:all-processes`

## API Contract And Data Contract

- Accounts with a role containing `mes:pro-feedback:frontline-pressure-pump:all-processes` can list every valid process under enabled pressure-pump routes.
- Pressure-pump route detection is limited to enabled routes whose formal route name contains `压力泵`.
- Returned candidates keep the existing candidate contract: route, route process, process, workstation, optional device, and display metadata.
- Ordinary accounts without the permission continue to use the existing workstation/post binding source and do not gain pressure-pump-wide visibility.

## Auth, Permissions, Validation, And Error Behavior

- Authorization source is `PermissionApi.getUserRoleIdListByUserId` plus `PermissionApi.hasAnyPermissionsInRoles`.
- The pressure-pump permission path is explicit and is not a fallback after workstation binding fails.
- Missing enabled pressure-pump route throws `PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_EMPTY`.
- Missing valid pressure-pump route processes throws `PRO_FRONTLINE_PRESSURE_PUMP_ROUTE_PROCESS_EMPTY`.
- Invalid process, workstation, or machinery context throws the existing fail-fast context error instead of returning empty or default success.

## Required Config, Services, Fixtures, And Migrations

- Required services: `PermissionApi`, `MesProRouteService`, `MesProRouteProcessMapper`, `MesProProcessService`, `MesMdWorkstationService`, `MesMdWorkstationMachineService`, `MesDvMachineryService`.
- Required migration: `IntRuoyiBackend/sql/mysql/20260803_mes_frontline_pressure_pump_all_process_permission.sql`.
- Required tests: `MesFrontlineDeviceAccountContextServiceTest`, `MesFrontlineWorkstationPostRouteBindingSourceTest`.

## BDD Scenarios

- BDD: 压力泵角色可切换全部压力泵工序 -> Given 登录账号拥有压力泵一线全工序权限角色, And 系统存在启用压力泵工艺路线及其工序, When 账号进入生产填写页加载可切换工序, Then 后端返回该压力泵路线下全部有效工序, And 不要求该账号岗位绑定工作站。
- BDD: 普通账号仍按岗位工作站授权 -> Given 登录账号没有压力泵一线全工序权限角色, When 账号进入生产填写页加载可切换工序, Then 后端仍按岗位、工作站、工艺路线工序工作站和启用路线解析, And 不得扩大到全部压力泵工序。
- BDD: 压力泵授权配置缺失 fail fast -> Given 登录账号拥有压力泵一线全工序权限角色, But 没有任何启用压力泵路线或有效路线工序, When 加载可切换工序, Then 后端明确返回缺失配置错误, And 不得返回默认全量、空成功或 mock 工序。

## RED

- RED: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesFrontlineWorkstationPostRouteBindingSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected before implementation because the service lacked the pressure-pump permission constructor dependencies and permission constant.

## GREEN

- GREEN: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesFrontlineWorkstationPostRouteBindingSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests, 0 failures, 0 errors, 0 skipped.

## Contract Or Integration Verification

- Existing ordinary workstation/post route binding regression remains covered by `MesFrontlineWorkstationPostRouteBindingSourceTest`.
- Pressure-pump permission success covers bypassing the route binding source and returning all valid pressure-pump route processes.
- Pressure-pump missing route-process configuration covers fail-fast behavior and expected business error text.

## Observability Touchpoints

- Fail-fast ServiceException error codes preserve the existing backend error propagation path.
- No new fallback, mock success, or default-empty response path was introduced.

## Blockers And Downstream Skill Needs

- No backend API blocker remains.
- Database permission migration evidence is recorded separately in `database-schema-evidence.md`.
