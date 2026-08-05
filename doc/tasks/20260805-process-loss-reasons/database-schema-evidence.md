# Database Schema Evidence

## Data

AC-D04 复用 `mes_pro_process_pool_defect_reason` 存储 LOSS 原因，以 `route_process_id` 作为共享绑定主键语义，并在 `mes_pro_feedback` 增加损耗原因快照字段。

## Migration

- Migration file: `IntRuoyiBackend/sql/mysql/20260805_mes_process_loss_reasons.sql`
- Metadata: `allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_process_pool_active_order_authority; type=schema; riskLevel=medium`
- 变更：
  - 移除旧 `leader_user_id + process_id` 维度唯一键。
  - 新增 `uk_mes_pp_loss_reason_route_process(tenant_id, route_process_id, reason_type, reason_code, deleted)`。
  - 新增 `idx_mes_pp_loss_reason_route_process(tenant_id, route_process_id, reason_type, enabled)`。
  - `leader_user_id` 调整为 nullable，LOSS 原因不作为组长所有权字段。
  - `mes_pro_feedback` 新增 `loss_reason_id`、`loss_reason_code_snapshot`、`loss_reason_name_snapshot`。

## Safety

- 迁移为幂等过程，使用 `information_schema` 检查索引和字段存在性。
- 未删除或改写历史报工；历史展示依赖新增快照字段。
- 删除原因使用停用语义，避免破坏历史报工追溯。

## Rollback

- 若需回滚，先停止新写入，保留 `mes_pro_feedback` 快照字段用于历史追溯，再按备份恢复索引结构；不得直接删除已保存快照字段。
- 生产环境执行前需按发布流程备份数据库并确认旧唯一键恢复不会与 routeProcess 共享数据冲突。

## BDD

- BDD: 多个生产组长共享同一工序损耗原因 -> Given 数据绑定到 `route_process_id` When 不同组长访问 Then 读取同一组原因。
- BDD: 历史报工保留损耗原因快照 -> Given 报工写入快照字段 When 后续原因改名或停用 Then 历史报工不受影响。

## RED / GREEN

- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 旧 schema 缺 routeProcess 共享唯一键和报工快照字段。
- GREEN: 后端定向 Maven 命令 -> PASS, `MesProcessPoolTeamLeaderSchemaTest` 4 tests passed。
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\20260805-process-loss-reasons\migration-policy-gate.json` -> PASS, `status=passed`, `migrationCount=432`。

## Verification

- Schema 测试断言新迁移包含 `uk_mes_pp_loss_reason_route_process`、`idx_mes_pp_loss_reason_route_process` 和三个报工快照字段。
- Migration policy gate 通过并识别 `20260805_mes_process_loss_reasons`。

## Blockers

- 尚未在真实写入型 E2E 数据库中执行端到端数据创建、修改、删除和历史快照页面核验；原因是缺任务自有测试租户、生产组长/员工账号和报工样本数据。
