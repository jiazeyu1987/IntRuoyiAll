# Database Schema Evidence：一线生产无工单工序池提交

## Data Change Goal And Affected Entities

- Goal：支持一线生产正式提交不匹配任何工单，同时保持工序池生产提交唯一性和幂等性。
- Affected entities: `mes_pro_process_pool`、`mes_pro_process_pool_event`、`mes_pro_process_pool_quantity_fragment`、测试 fixture `create_tables.sql`。

## Database Engine And Migration Tool

- Database engine: MySQL。
- Migration tool: 项目 SQL 迁移文件 + `run-release-migration-policy-gate.py` 发布迁移策略门禁。

## Schema And Migration Changes

- Migration: `IntRuoyiBackend/sql/mysql/20260808_mes_process_pool_frontline_no_work_order.sql`
- `mes_pro_process_pool.work_order_id` 改为 nullable，新增生成列 `work_order_context_key = COALESCE(work_order_id, 'NO_WORK_ORDER')`。
- `mes_pro_process_pool_event.work_order_id` 改为 nullable，新增生成列 `work_order_context_key`，并允许 `recordbook_source_type/source_id` 为空。
- `mes_pro_process_pool_quantity_fragment.work_order_id` 改为 nullable。
- 工序池唯一键和事件幂等唯一键改用 `work_order_context_key`，避免 MySQL nullable unique 允许重复无工单上下文。

## Data Safety Analysis

- 不删除、重写或回填历史业务数据。
- Nullable 放宽只扩大一线生产可接受上下文，不改变已有带工单记录的业务键。
- 生成列使用确定性表达式；带工单记录继续以工单 ID 作为唯一键，无工单记录统一归入 `NO_WORK_ORDER` 上下文参与唯一性约束。

## Rollback Or Recovery Plan

- 若部署前发现兼容问题，停止应用该迁移即可。
- 若部署后需恢复非空约束，必须先只读核对相关表中 `work_order_id IS NULL` 的生产提交记录并制定数据归属方案；不得直接恢复 NOT NULL 导致已有无工单生产提交失败。
- 恢复唯一键前必须先移除依赖 `work_order_context_key` 的唯一约束，再按旧索引定义重建；执行前需备份目标表。

## BDD Scenarios

- BDD: 无工单生产提交入池 -> Given 一线生产提交没有 workOrderId / When 写入工序池 pool、event、quantity fragment / Then 三张表允许 work_order_id 为空，并按 `NO_WORK_ORDER` 上下文保持唯一。
- BDD: 有工单提交保持唯一 -> Given 生产提交带 workOrderId / When 写入工序池 / Then 继续按该 workOrderId 与路线、工序、设备、工位保持唯一。

## RED

- RED: 旧 schema fixture 中工序池生产提交相关列为 NOT NULL 或唯一键直接包含 nullable work_order_id，无法同时支持无工单提交与无工单幂等唯一性。

## GREEN

- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\fix-frontline-production-no-work-order-context\migration-policy-gate.json` -> PASS，status=passed，migrationCount=449。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolEventServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，22 tests，0 failures，0 errors。

## Migration Verification

- Migration policy gate output: `doc/tasks/fix-frontline-production-no-work-order-context/migration-policy-gate.json`
- Verified migration ID: `20260808_mes_process_pool_frontline_no_work_order`
- Gate metadata: type=`schema`，riskLevel=`medium`，allowedEnvironments=`test,backup,prod`，dependsOn=`20260803_mes_process_pool_event_idempotency`。

## Blockers

- No schema blocker remains for the implemented migration.
- Adjacent backend JUnit补跑因并发 Maven 占用共享 `target` 阻塞；该阻塞不影响迁移策略门禁结论。
