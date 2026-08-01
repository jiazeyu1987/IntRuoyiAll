# Database Schema Evidence - P1

## Data

- Data change goal: persist production team leader active orders, team-local employee profiles, team-local devices, process-device bindings, process-employee profile references, and device parameter defaults.
- Affected entities:
  - `mes_pro_process_pool_active_order`
  - `mes_pro_process_pool_team_employee_profile`
  - `mes_pro_process_pool_team_device`
  - `mes_pro_process_pool_team_process_device`
  - `mes_pro_process_pool_team_employee_binding.employee_profile_id`
  - `mes_pro_process_pool_device_parameter_rule.unit`
  - `mes_pro_process_pool_device_parameter_rule.default_value`

## Migration

- Migration file: `IntRuoyiBackend/sql/mysql/20260731_mes_process_pool_team_leader_p1_runtime_config.sql`.
- Migration style: additive tables and columns for P1 runtime configuration; no destructive table drop or data rewrite.
- Persistence model files: new DO/Mapper classes for active order, employee profile, team device, and process-device binding; existing employee binding and device parameter rule DOs extended.

## Safety

- P1 migration is additive and preserves existing team leader maintenance tables.
- Existing employee-user binding is preserved; temporary employees use nullable `employee_profile_id` / `systemUserId` flow instead of replacing system users.
- Device status values are enforced in service validation; disabled devices are marked unavailable through `enabled=false`, while `REPAIRING` remains enabled for history but unavailable for new runtime use.
- Real database migration was not applied in this turn; validation used source migration and schema contract tests only.

## Rollback

- If P1 migration must be rolled back before production application, remove the newly added P1 tables and columns in a controlled rollback migration after confirming no P1 runtime data has been created.
- If rollback is needed after data exists, first export task-owned rows from active orders, team employee profiles, team devices, process-device bindings, and parameter defaults, then perform an audited rollback plan.

## BDD:

- BDD: 活跃订单持久化 -> Given 组长加入订单到活跃池 When 保存 Then 系统记录 `leader_user_id`, `work_order_id`, `active_status`, `joined_at`。
- BDD: 临时工不依赖用户系统 -> Given 员工档案无 `system_user_id` When 绑定工序 Then 绑定记录使用 `employee_profile_id`。
- BDD: 设备参数默认值持久化 -> Given 参数包含单位和默认值 When 保存 Then `unit` 与 `default_value` 落到设备参数规则。

## RED:

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason:缺少 P1 DO、Mapper、服务和迁移字段模型。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason:新增接口合同引用的 P1 VO 和服务方法尚未实现。

## GREEN:

- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS through the P1 combined GREEN command, verifying schema contract coverage.
- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamEmployeeBindingServiceTest,MesProcessDeviceParameterRuleServiceTest,MesDefectReasonCatalogServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run: 20, failures: 0, errors: 0, skipped: 0.

## Verification

- `MesProcessPoolTeamLeaderSchemaTest` imports and asserts P1 DO fields and SQL migration content, including active order table, team device table, employee profile table, process-device table, `employee_profile_id`, `unit`, and `default_value`.
- Service tests verify persistence payloads include the expected P1 fields and that invalid parameter defaults fail fast.

## Blockers

- No P1 schema-contract blocker remains.
- Real database migration execution and tenant data validation remain future environment gates before full E2E and production fusion.
