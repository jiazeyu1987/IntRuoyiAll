# Bug Regression Evidence

## Bug

app 仓 migration policy gate 未拒绝 executable migration 依赖 evidence-only migration，导致 app 预检可能通过但维护仓实际发布 gate 在 build-release 前失败。

## Expected

当 `schema/data/menu/config/permission/seed` dependsOn `preflight/backfill/postflight/rollback-dry-run` 时，app gate 必须抛出 `MigrationPolicyError`，错误包含 `executable migration cannot depend on evidence-only migration`。

## Reproduction

`python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py -q`。

## Root Cause

app gate 缺少 executable/evidence-only 类型分类和依赖闭包检查；同时 C015 schema 和后续压力泵 data 迁移各有一处依赖 evidence-only 迁移的元数据。

## RED:

`python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py -q` -> FAIL, 新增测试失败于 `DID NOT RAISE MigrationPolicyError`。

## GREEN:

`python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py script\tests\test_mes_pressure_pump_same_name_item_convergence_sql.py -q` -> PASS, 12 passed。

## Verification

app full migration policy gate status=passed, migrationCount=505；maintenance actual ops gate status=passed, migrationCount=505。

## Blockers

无当前阻塞；下一步提交新的已提交 HEAD 并恢复仅测试服发布。
