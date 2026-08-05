# Database Schema Evidence

## Data

目标是为生产组长建立正式生产人员档案能力，受影响实体包括 `mes_pro_process_pool_team_employee_profile`、`mes_pro_process_pool_team_employee_binding` 和 `mes_pro_process_pool_team_maintenance_audit`。

## Migration

- 迁移文件：`IntRuoyiBackend/sql/mysql/20260805_mes_process_pool_production_personnel.sql`。
- 数据库引擎：MySQL。
- 迁移工具：项目 SQL release migration 文件。
- 结构变化：新增 `display_name`、`signature_password_hash`、`signature_password_updated_at`、`active_display_name`、`display_name_snapshot`、`operator_user_id`、`result_status`、`change_summary`。
- 唯一约束：`uk_mes_pp_team_employee_active_display_name (tenant_id, leader_user_id, active_display_name, deleted)`，用于控制同一生产组长有效员工显示名不重复。

## Safety

- 迁移使用 `information_schema` 检查后再新增列/索引，避免重复执行失败。
- 已有档案 `display_name` 用 `employee_name` 回填，确保历史员工可继续显示。
- 本地测试库已应用迁移并核对新增列与唯一索引；任务自有 E2E fixture 仅用于测试租户真实验收。

## Rollback

- 回滚策略：在发布前如需回滚，先备份三张目标表，再移除新增唯一索引和新增列；历史业务数据不做静默删除。
- 风险：删除 `display_name_snapshot` 或审计字段会影响历史追溯，应仅在确认未上线或已完成数据备份后执行。

## BDD

- BDD: 显示名唯一 -> Given 同一生产组长已有有效员工显示名；When 新增或改名为同名；Then 唯一约束和服务校验阻止重复。
- BDD: 临时工签名密码 -> Given 新增临时工；When 保存签名密码；Then 只保存密码哈希和更新时间。
- BDD: 审计留痕 -> Given 人员管理写操作成功；When 查询审计；Then 返回动作、操作人、目标、结果和摘要。

## RED / GREEN

- RED: `node tests/e2e/production-personnel-management-static.spec.cjs` -> FAIL, 前端和 API wrapper 尚未声明生产人员档案合同。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderRuntimeConfigServiceTest,MesFrontlineRuntimeConfigServiceTest,MesFrontlineRuntimeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 30, Failures: 0, Errors: 0, Skipped: 0。

## Verification

- `MesProcessPoolTeamLeaderSchemaTest` 覆盖新增列、签名密码哈希字段、显示名唯一约束、绑定显示名快照和审计字段。
- 本地测试库字段和索引核对通过：`display_name`、`signature_password_hash`、`signature_password_updated_at`、`active_display_name`、`display_name_snapshot`、审计字段和唯一索引均存在。
- `git diff --check` -> PASS，无 whitespace error。

## Blockers

- 无当前 blocker。
