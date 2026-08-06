# Execution Log

## User Intent

用户要求按照已确认计划实施：在本机 `int_main` 中给 `芋道源码/admin` 绑定 `球囊扩张压力泵` 和 `按压式球囊扩充压力泵` 两条产线的工序开始生产组长。

## BDD

- BDD: admin can add pressure pump route processes -> Given 本机 tenant `1` 的 `admin` 登录生产组长工作台, When 点击新增工序候选, Then 候选应包含 route `922119 / RT000028 / 球囊扩张压力泵` 与 route `980091 / RT000028-IDI / 按压式球囊扩充压力泵` 的 active 路线工序。
- BDD: binding is scoped to active tenant 1 routes only -> Given 数据修复只允许本机 tenant `1`, When 写入工序开始生产组长快照, Then 只能更新 active version `448` 与 `622`，不得修改 tenant `122`、draft version 或历史版本。
- BDD: missing route start leader snapshot fails fast -> Given active route snapshot 缺少 `routeStartProductionLeaders`, When 执行写入前 RED 校验, Then 校验必须失败并指出缺失快照，禁止用角色权限或空列表成功替代。

## Evidence

- Skill: `database-schema-delivery` loaded.
- Trigger docs read: `docs/database-rules.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`, `docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦`.
- Schema evidence: `mes_pro_route_version.route_snapshot_json` is `mediumtext`, collation `utf8mb4_unicode_ci`; target writes use container mysql `--default-character-set=utf8mb4` and ASCII-safe `UNHEX` for Chinese JSON values.
- Git baseline note: existing workspace already contains many unrelated dirty changes from other tasks. Current task does not modify those files and only owns `doc/tasks/20260806-admin-pressure-pump-route-start-leader/` plus the local DB rows `mes_pro_route_version.id IN (448, 622)`.

## RED

- RED: `docker exec int-ruoyi-mysql ... mysql ... < db-repair/red-missing-route-start-leaders.sql` -> FAIL, expected reason: route versions `448` and `622` are missing `routeStartProductionLeaders`.

## GREEN

Pending.

## Rollback

- Full row backup: `db-backup/route-version-448-622-before.sql`.
- Rollback command, if required: apply the backup file back to local Docker MySQL after confirming no later task legitimately changed route versions `448` or `622`.
