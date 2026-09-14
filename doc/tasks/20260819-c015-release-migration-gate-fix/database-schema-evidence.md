# Database Schema Evidence

## Data

本次不执行远端数据库 DDL/DML，不修改测试服数据；仅修复 SQL release-migration 元数据依赖，确保发布门禁能够在构建前正确 fail fast。

## Migration

`20260814_mes_c015_route_dcc_qa_reconciliation_schema.sql` 首行 dependsOn 从 evidence-only `backfill` 改为 executable `bootstrap`；`20260818_mes_pressure_pump_same_name_item_convergence.sql` 首行 dependsOn 从 evidence-only `postflight` 改为 executable `schema`；SQL 执行体不变。

## Safety

未加入 fallback、默认成功、数据推断或远端手工修补；修复只改变发布依赖图元数据，完整 SQL root 通过 app gate 和维护仓实际 ops gate。

## Rollback

回滚本次提交即可恢复元数据；因未执行远端数据库操作，不需要数据恢复。

## BDD:

Given C015 schema 和压力泵 data 迁移需要发布执行, When migration policy gate 运行, Then 可执行迁移只能依赖可执行迁移，evidence-only 迁移只能作为独立证据门禁。

## RED:

app 完整 migration policy gate 在补齐闭包规则后 FAIL，发现 `20260818_mes_pressure_pump_same_name_item_convergence` (`data`) dependsOn `20260814_mes_c015_route_dcc_qa_reconciliation_postflight` (`postflight`)。

## GREEN:

聚焦 pytest 12 passed；app full gate status=passed migrationCount=505；maintenance actual ops gate status=passed migrationCount=505。

## Verification

证据文件：`app-migration-policy-gate.json` 与 `maintenance-ops-migration-policy-gate.json`。

## Blockers

无当前阻塞。
