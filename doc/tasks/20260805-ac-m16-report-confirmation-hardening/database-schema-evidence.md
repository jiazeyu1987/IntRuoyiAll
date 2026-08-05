# Database Schema Evidence

## Data

受影响实体为 `mes_pro_process_pool_submission_review`，目标是保证同一租户、同一提交事件、同一删除状态下只能存在一条终态复核事实，支撑 AC-M16 重复确认和退回后继续分配的数据库级防线。

## Migration

新增迁移 `IntRuoyiBackend/sql/mysql/20260805_mes_process_pool_ac_m16_terminal_constraints.sql`，依赖 `20260804_mes_process_pool_timeline_performance_indexes`，先检查历史重复 review，发现重复即 `SIGNAL SQLSTATE '45000'`，无重复时添加 `uk_mes_pp_submission_review_event (tenant_id, event_id, deleted)`。

## Safety

迁移不删除、不覆盖、不回填业务数据；若历史存在重复终态 review，迁移 fail-fast 阻塞发布，要求先进行有审计的数据治理，避免静默选择任一终态。

## Rollback

回滚策略为移除唯一索引 `uk_mes_pp_submission_review_event`；由于迁移不改写数据，回滚不需要数据反向变更。若迁移因重复数据阻塞，回滚点是保留原表不变并先治理重复记录。

## BDD:

BDD: 已有终态复核禁止二次分配 -> Given 一个提交事件已有 review 终态 When 再次确认 Then 应在服务层和数据库层均拒绝第二条有效终态事实。

## RED:

`java @doc\tasks\20260805-ac-m16-report-confirmation-hardening\junit-console-red.args` -> FAIL，旧服务实现未读取 `selectLatestByEventIdForUpdate` 终态导致退回后继续分配用例失败。

## GREEN:

`python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\20260805-ac-m16-report-confirmation-hardening\migration-policy-gate.json` -> PASS，新增迁移被识别为 schema、allowedEnvironments=test/backup/prod、dependsOn 正确。

## Verification

静态 SQL 断言通过：新增 SQL 包含 release-migration 元数据、重复数据 `SIGNAL SQLSTATE '45000'`、重复提示文案和 `UNIQUE KEY uk_mes_pp_submission_review_event`；损坏的 `20260730_mes_process_pool_team_leader.sql` 已按 HEAD 正式内容恢复，NUL 字节问题消除。

## Blockers

未连接真实数据库执行迁移；当前任务只交付可发布迁移与静态/策略门禁证据，真实库执行需按发布流程另行授权。
