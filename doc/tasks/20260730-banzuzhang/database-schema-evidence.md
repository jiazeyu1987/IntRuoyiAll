# F9/F10 Database Schema Evidence

## Data

本次数据变更为 F9/F10 班组长工作台和班组维护新增正式持久化模型：

- `mes_pro_process_pool_team_leader_scope`
- `mes_pro_process_pool_submission_review`
- `mes_pro_process_pool_work_order_abnormal`
- `mes_pro_process_pool_team_employee_binding`
- `mes_pro_process_pool_defect_reason`
- `mes_pro_process_pool_device_parameter_rule`
- `mes_pro_process_pool_team_maintenance_audit`
- `system_menu` / `system_role_menu` / `system_tenant_package.menu_ids` 菜单与权限补齐

## Migration

- Migration file: `IntRuoyiBackend/sql/mysql/20260730_mes_process_pool_team_leader.sql`
- Metadata: `-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_review_copy; type=schema; riskLevel=medium`
- Engine: MySQL / InnoDB / utf8mb4.
- Migration tool gate: `IntRuoyiBackend/script/release/run-release-migration-policy-gate.py`

## Safety

- 仅新增 `mes_pro_process_pool_*` F9/F10 表，不删除或重写已有业务表。
- 菜单安装先检查生产管理父菜单 `5700` 存在，否则 `SIGNAL` fail fast。
- 租户套餐 `menu_ids` 非合法 JSON 时直接 `SIGNAL`，不静默覆盖。
- 菜单和权限使用固定 ID `900310-900314`，按 `NOT EXISTS` 幂等插入。
- 维护动作有独立审计表；禁用员工只写未来候选状态，不改历史提交。

## Rollback

当前迁移未写 destructive rollback。若需回滚：

- 先停用或移除菜单 `900310-900314` 及对应 `system_role_menu` 绑定。
- 再按依赖顺序移除 F9/F10 新增表。
- 回滚前必须备份 `mes_pro_process_pool_team_*`、`mes_pro_process_pool_submission_review`、`mes_pro_process_pool_work_order_abnormal`、`mes_pro_process_pool_defect_reason`、`mes_pro_process_pool_device_parameter_rule` 和 `mes_pro_process_pool_team_maintenance_audit` 数据。

## BDD

- BDD: 班组长负责范围模型 -> Given 班组长配置了员工和工序范围 / When 查询或维护 F9/F10 数据 / Then 只能访问负责范围内提交和维护负责工序。
- BDD: 班组维护审计 -> Given 班组长添加员工、禁用员工、维护不良原因或参数上下限 / When 保存成功 / Then 维护审计表写入动作类型、目标类型、目标 ID、前后快照和服务端时间。
- BDD: 菜单权限可安装 -> Given 生产管理父菜单存在且租户套餐 menu_ids 为合法 JSON / When 执行迁移 / Then 创建班组长工作台页面菜单和四个按钮权限，并合并到含生产管理菜单的租户套餐。

## RED

- RED: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260730_mes_process_pool_team_leader.sql --output ..\doc\tasks\20260730-banzuzhang\migration-policy-gate.json` -> FAIL，expected reason: targeted 单文件门禁缺少依赖迁移 `20260730_mes_process_pool_review_copy`。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: F9/F10 schema 迁移未实现前，必需表和菜单权限不存在。

## GREEN

- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260730-banzuzhang\migration-policy-gate.json` -> PASS，`status=passed`，`migrationCount=397`，包含 `20260730_mes_process_pool_team_leader` 及其 process pool 依赖链。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderScopeServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesWorkOrderAbnormalReportServiceTest,MesTeamEmployeeBindingServiceTest,MesDefectReasonCatalogServiceTest,MesProcessDeviceParameterRuleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

## Verification

- `migration-policy-gate.json` 已写入任务目录，状态为 `passed`。
- `MesProcessPoolTeamLeaderSchemaTest` 覆盖 F9/F10 表、菜单和权限迁移契约。
- Maven 定向回归 16 tests 全部通过。

## Blockers

- 未对真实数据库执行迁移；当前只完成迁移策略门禁和 schema contract 测试。
- 真实租户菜单可见性需要在应用迁移后的本地或测试租户中通过真实登录路径复验。
