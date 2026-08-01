# Backend API Evidence - P1

## Scope

- Phase: P1 班组配置与活跃订单后端模型。
- Backend module: `IntRuoyiBackend/yudao-module-mes`.
- Controller scope: `MesProcessPoolTeamLeaderController`.
- Service scope: `MesTeamLeaderActiveOrderService`, `MesTeamLeaderRuntimeConfigService`, and adjacent existing team maintenance services.

## Contract

- Active order API:
  - `POST /mes/pro/process-pool/team-leader/active-order/add`
  - `PUT /mes/pro/process-pool/team-leader/active-order/remove`
  - `GET /mes/pro/process-pool/team-leader/active-order/list`
- Runtime config API:
  - `POST /employee-profile/create`
  - `POST /process-employee-binding/save`
  - `POST /team-device/create`
  - `PUT /team-device/status/update`
  - `POST /process-device-binding/save`
  - `POST /runtime-device-parameter-rule/save`
  - `POST /process-defect-reason/save`
- Auth and permission behavior: Controller tests assert query endpoints use `mes:pro-process-pool-team-leader:query`, maintenance endpoints use `mes:pro-process-pool-team-leader:maintain`, and client request VOs do not accept `leaderUserId`.
- Data contract: `leaderUserId` is always injected from `SecurityFrameworkUtils.getLoginUserId()`; temporary employees can omit `systemUserId`; devices support `ENABLED`, `REPAIRING`, `DISABLED`; parameter rules carry `unit`, `lowerLimit`, `upperLimit`, `defaultValue`, and `valueType`.

## Validation

- Missing command inputs, unknown leader, missing work order, unavailable device, invalid device status, invalid parameter range, and missing process scope fail through service exceptions.
- No default-success, mock-success, fallback source, or silent downgrade path was introduced.
- Existing legacy maintenance endpoints remain present while P1 formal runtime-config endpoints are added.

## BDD:

- BDD: 活跃订单池维护 -> Given 生产组长选择生产订单 When 添加到活跃订单池 Then 活跃订单记录按当前登录组长保存并按 FIFO 加入顺序查询。
- BDD: 临时工档案维护 -> Given 生产组长新增临时工 When 未提供系统用户编号 Then 员工档案仍可创建并绑定到工序。
- BDD: 设备状态维护 -> Given 设备处于报修 When 员工或工序绑定读取可用设备 Then 报修设备不可用；When 组长恢复为启用 Then 设备重新可用。
- BDD: 设备参数默认值 -> Given 设备参数上下限 10-20 When 默认值为 15 Then 保存成功；When 默认值为 25 Then 保存失败。

## RED:

- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason:新增 Controller 测试先引用 P1 活跃订单、员工档案、设备状态、工序-设备、工序-异常和运行态设备参数接口；生产代码缺少对应 VO、Controller 方法和服务方法，testCompile 报缺少符号。

## GREEN:

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 15, failures: 0, errors: 0, skipped: 0.
- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamEmployeeBindingServiceTest,MesProcessDeviceParameterRuleServiceTest,MesDefectReasonCatalogServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 20, failures: 0, errors: 0, skipped: 0.

## Verification

- Controller tests verify route mappings, permissions, login-user injection, active order list response mapping, runtime-config request mapping, and absence of client-provided `leaderUserId`.
- Service tests verify active order add/remove/list, temporary employee profile creation, process employee binding, device creation, repairing-device rejection, device repair/disable/recover state updates, device parameter default values, out-of-range default rejection, and process defect reason binding.
- Adjacent regression tests verify existing employee binding, old device parameter rule, and defect reason catalog services remain passing.

## Blockers

- No P1 backend/API unit-test blocker remains.
- Later phases still need employee runtime-config read APIs, report allocation, FIFO allocation, batch-record backfill, frontend integration, real E2E data, and `int_main` fusion.
